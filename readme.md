## ART-BPMS-DROOLS

This module adds [Drools] DRL support to Camunda BPMS

#### Features

* Execution of DRL files from BPMN

#### Installation

1. [Maven] is required for building this module
1. Clone this repository, navigate into its root folder and execute `mvn clean install` command
1. To add the module into maven or gradle project, the following dependency should be imported and added to project's WAR/EAR libs directory or camunda engine's classpath:
    ```
    <dependency>
        <groupId>com.artezio.bpm.camunda</groupId>
        <artifactId>drools</artifactId>
        <version>1.0</version>
    </dependency>
    ```

#### Usage

1. Add DRL files to your Process Application archive (e.g. `src/main/resources/rule1.drl`, `src/main/resources/rule2.drl`)
1. In BPMN diagram create a Delegate Expression Service task and point it to `com.artezio.bpm.integration.drools.DroolsDelegate` class (`${droolsDelegate}` delegate expression when using CDI)
1. Input. Provide the following input params:
    * `rules` - List of input rule filenames within the deployment, e.g. `rule1.drl`, `rule2.drl`
    * `facts` - List of facts that will be inserted into Drools Working memory
1. Output. The following *local* variables are created in the task after evaluating all rules and are available for output mapping:
    * `facts` - List of objects in the final state of Working memory after firing all rules
    * `inserted` - List of objects that were inserted into Working memory during rules evaluation
    * `updated` - List of objects that were updated in Working memory during rules evaluation
    * `deleted` - List of objects that were deleted from Working memory during rules evaluation
1. To map local task variable to global process variable you could for example create task output parameter of type `text` and specify `#{inserted}` as value. The variable is then populated with `List` of inserted objects, despite being marked as `text`
1. Deploy the Process Application archive and run the process. DRL files will be executed when the Service task is run
  
   
[Drools]: https://www.drools.org   
[Maven]: https://maven.apache.org