package com.artezio.bpm.integration.drools;

import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.KieRepository;
import org.kie.api.builder.Message;
import org.kie.api.event.rule.ObjectDeletedEvent;
import org.kie.api.event.rule.ObjectInsertedEvent;
import org.kie.api.event.rule.ObjectUpdatedEvent;
import org.kie.api.event.rule.RuleRuntimeEventListener;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.internal.io.ResourceFactory;

import javax.inject.Named;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

@Named
public class DroolsDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) {
        List facts = (List) execution.getVariable("facts");
        List<String> rules = (List<String>) execution.getVariable("rules");
        KieSession session = getStatefulKieSession(execution, rules);
        WorkingMemoryHistory history = fireRules(session, facts);
        execution.setVariableLocal("inserted", history.getInserted());
        execution.setVariableLocal("updated", history.getUpdated());
        execution.setVariableLocal("deleted", history.getDeleted());
        execution.setVariableLocal("facts", session.getObjects().stream().collect(Collectors.toList()));
        session.dispose();
    }

    private class WorkingMemoryHistory {
        private List<Object> inserted = new LinkedList<>();
        private List<Object> updated = new LinkedList<>();
        private List<Object> deleted = new LinkedList<>();

        void insert(Object object) {
            inserted.add(object);
        }

        void update(Object object) {
            updated.add(object);
        }

        void delete(Object object) {
            deleted.add(object);
        }

        public List<Object> getInserted() {
            return inserted;
        }

        public List<Object> getUpdated() {
            return updated;
        }

        public List<Object> getDeleted() {
            return deleted;
        }
    }

    private class WorkingMemoryHistoryCollector implements RuleRuntimeEventListener {
        private WorkingMemoryHistory workingMemoryHistory = new WorkingMemoryHistory();

        @Override
        public void objectInserted(ObjectInsertedEvent event) {
            workingMemoryHistory.insert(event.getObject());
        }

        @Override
        public void objectUpdated(ObjectUpdatedEvent event) {
            workingMemoryHistory.update(event.getObject());
        }

        @Override
        public void objectDeleted(ObjectDeletedEvent event) {
            workingMemoryHistory.delete(event.getOldObject());
        }

        public WorkingMemoryHistory getHistory() {
            return workingMemoryHistory;
        }
    }

    public WorkingMemoryHistory fireRules(KieSession kSession, List<?> facts) {
        facts.forEach(kSession::insert);
        WorkingMemoryHistoryCollector historyCollector = new WorkingMemoryHistoryCollector();
        kSession.addEventListener(historyCollector);
        kSession.fireAllRules();
        return historyCollector.getHistory();
    }

    private KieSession getStatefulKieSession(DelegateExecution execution, Collection<String> rules) {
        RepositoryService repositoryService = execution.getProcessEngineServices().getRepositoryService();
        String deploymentId = repositoryService.getProcessDefinition(execution.getProcessDefinitionId()).getDeploymentId();
        KieServices kServices = KieServices.Factory.get();
        KieRepository kRepository = kServices.getRepository();
        KieFileSystem kFileSystem = kServices.newKieFileSystem();
        rules.forEach(
                rule -> kFileSystem.write(ResourceFactory
                        .newInputStreamResource(repositoryService.getResourceAsStream(deploymentId, rule))
                        .setTargetPath(rule)));
        KieBuilder kBuilder = kServices.newKieBuilder(kFileSystem);
        kBuilder.buildAll();
        if (kBuilder.getResults().hasMessages(Message.Level.ERROR)) {
            throw new RuntimeException("Kie Builder Errors:\n" + kBuilder.getResults().toString());
        }
        KieContainer kContainer = kServices.newKieContainer(kRepository.getDefaultReleaseId());
        return kContainer.newKieSession();
    }

}