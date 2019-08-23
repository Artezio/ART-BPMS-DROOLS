package com.artezio.bpm.integration.drools;

import com.artezio.bpm.integration.drools.model.Item;
import com.artezio.bpm.integration.drools.model.Manufacturer;
import de.odysseus.el.ExpressionFactoryImpl;
import de.odysseus.el.util.SimpleResolver;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.runtime.ProcessInstanceWithVariables;
import org.camunda.bpm.engine.task.Task;
import org.camunda.bpm.engine.test.Deployment;
import org.camunda.bpm.engine.test.ProcessEngineRule;
import org.camunda.bpm.engine.test.mock.Mocks;
import org.easymock.TestSubject;
import org.jboss.weld.junit4.WeldInitiator;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import javax.el.ELResolver;
import javax.el.ExpressionFactory;
import javax.enterprise.inject.Produces;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class DroolsDelegateTest {

    @Rule
    public ProcessEngineRule processEngineRule = new ProcessEngineRule();
    @Rule
    public WeldInitiator weld = WeldInitiator.fromTestPackage()
            .activate()
            .build();

    @Produces
    public ELResolver getElResolver() {
        return new SimpleResolver(true);
    }

    @Produces
    public ExpressionFactory getExpressionFactory() {
        return new ExpressionFactoryImpl();
    }

    @TestSubject
    private DroolsDelegate droolsDelegate = new DroolsDelegate();
    private Map<String, Object> variables = new HashMap<>();

    {
        Mocks.register("droolsDelegate", droolsDelegate);
    }

    @Before
    public void init() throws NoSuchFieldException {
        variables.put("startedBy", "TestUser");
    }

    @After
    public void tearDown() {
        variables.clear();
        Set<String> taskIds = processEngineRule.getTaskService().createTaskQuery()
                .list()
                .stream()
                .map(Task::getId)
                .collect(Collectors.toSet());
        processEngineRule.getHistoryService().createHistoricTaskInstanceQuery()
                .finished()
                .list()
                .forEach(historicTaskInstance -> taskIds.add(historicTaskInstance.getId()));

        taskIds.forEach(taskId -> processEngineRule.getHistoryService().deleteHistoricTaskInstance(taskId));
        taskIds.stream()
                .map(taskId -> processEngineRule.getTaskService().createTaskQuery().taskId(taskId).singleResult())
                .filter(Objects::nonNull)
                .map(Task::getProcessDefinitionId)
                .filter(Objects::nonNull)
                .forEach(processDefinitionId -> processEngineRule.getRepositoryService().deleteProcessDefinition(processDefinitionId, true));
        processEngineRule.getTaskService().deleteTasks(taskIds);

        List<ProcessInstance> processInstances = processEngineRule.getRuntimeService().createProcessInstanceQuery().list();
        processInstances.forEach(processInstance ->
                processEngineRule.getRuntimeService().suspendProcessInstanceById(processInstance.getId()));
    }

    @Test
    @Deployment(resources = {"runTestDroolsRules.bpmn",
            "find-expensive-items-manufacturers.drl",
            "find-manufacturers-from-usa.drl"
    })
    public void testRunDroolsFromBpmn() {
        Manufacturer bentley = new Manufacturer("Bentley", "Great Britain");
        Manufacturer colgate = new Manufacturer("Colgate-Palmolive", "USA");
        Manufacturer procterAndGamble = new Manufacturer("Procter & Gamble", "USA");

        Item expensiveVehicle1 = new Item(125000.0, "T Series", bentley);
        Item expensiveVehicle2 = new Item(200000.0, "X Series", bentley);
        Item cheapToothpaste1 = new Item(0.9, "Colgate Total 12", colgate);
        Item cheapToothpaste2 = new Item(2.7, "Blendamet", procterAndGamble);
        Item expensiveToothpaste = new Item(900.0, "Colgate Total 12 pack of 500", colgate);

        variables.put("items", Arrays.asList(expensiveVehicle1, expensiveVehicle2, cheapToothpaste1, cheapToothpaste2, expensiveToothpaste));

        ProcessInstanceWithVariables process = processEngineRule.getRuntimeService().createProcessInstanceByKey("FindExpensiveItemsManufacturers")
                .setVariables(variables)
                .executeWithVariablesInReturn();

        List<Object> insertedObjects = (List<Object>) process.getVariables().get("inserted");
        List<Object> deletedObjects = (List<Object>) process.getVariables().get("deleted");
        List<Object> updatedObjects = (List<Object>) process.getVariables().get("updated");
        List<Object> facts = (List<Object>) process.getVariables().get("facts");

        assertNotNull(insertedObjects);
        assertNotNull(deletedObjects);
        assertNotNull(updatedObjects);
        assertNotNull(facts);

        assertTrue(facts.contains(colgate));
        assertFalse(facts.contains(bentley));
        assertFalse(facts.contains(procterAndGamble));

    }

}