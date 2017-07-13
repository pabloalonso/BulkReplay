package com.bonitasoft.bulk;

import com.bonitasoft.bulk.beans.FailedFlowNodeType;
import com.bonitasoft.engine.api.ProcessAPI;
import com.bonitasoft.engine.api.TenantAPIAccessor;
import com.bonitasoft.engine.bpm.process.impl.ProcessInstanceSearchDescriptor;
import org.bonitasoft.engine.bpm.connector.ConnectorInstance;
import org.bonitasoft.engine.bpm.connector.ConnectorInstanceNotFoundException;
import org.bonitasoft.engine.bpm.connector.ConnectorInstanceWithFailureInfo;
import org.bonitasoft.engine.bpm.connector.ConnectorInstancesSearchDescriptor;
import org.bonitasoft.engine.bpm.flownode.*;
import org.bonitasoft.engine.bpm.process.ProcessDeploymentInfo;
import org.bonitasoft.engine.bpm.process.ProcessDeploymentInfoCriterion;
import org.bonitasoft.engine.bpm.process.ProcessInstance;
import org.bonitasoft.engine.exception.BonitaHomeNotSetException;
import org.bonitasoft.engine.exception.SearchException;
import org.bonitasoft.engine.exception.ServerAPIException;
import org.bonitasoft.engine.exception.UnknownAPITypeException;
import org.bonitasoft.engine.search.Order;
import org.bonitasoft.engine.search.SearchOptionsBuilder;
import org.bonitasoft.engine.search.SearchResult;
import org.bonitasoft.engine.session.APISession;

import java.io.Serializable;
import java.util.*;
import java.util.logging.Logger;

/**
 * Created by pablo on 04/07/2017.
 */
public class FailedFlowNodesAccesor {
    /*
    private  Map<Long, Map<String,Serializable>> tasksWithConnectors = new HashMap<Long, Map<String,Serializable>>();
    private  HashMap<Long, String> processDefinitions;
    private  final Set<String> usedProcessDefinitions = new HashSet<>();
    */
    private final Logger logger = Logger.getLogger("com.bonitasoft.bulk");

    /**
     * Method that will return the list of processes that have a task on failed on the last searchFailedFlowNodes, if no call done yet, it will return all of them
     * @param session Bonita API Session
     * @return Set of ProcessName and ProcessVersion
     * @throws BonitaHomeNotSetException
     * @throws ServerAPIException
     * @throws UnknownAPITypeException
     */
    public  Set<String> getProcessUsedDefinitions(APISession session) throws BonitaHomeNotSetException, ServerAPIException, UnknownAPITypeException{
        /* It will not be stateless and let several users t work with it
        if(usedProcessDefinitions.size() > 0)
            return usedProcessDefinitions;
        else{
            return new HashSet<String>(getProcessDefinitions(session));

        }
        */
        return new HashSet<String>(getProcessDefinitions(session).values());
    }

    public List<Map<String, Serializable>> getDetailedConnectorInformation(APISession apiSession, List<Long> flowNodeIds) throws ConnectorInstanceNotFoundException, ServerAPIException, SearchException, BonitaHomeNotSetException, UnknownAPITypeException {
        final Map<String, Map<String, Serializable>> errorMsgs = new HashMap<String, Map<String, Serializable>>();
        Map<Long, Map<String,Serializable>> tasksWithConnectors = searchFailedConnectors(apiSession);
        if(tasksWithConnectors.size() > 0){
            for(Long flowNodeId : flowNodeIds){
                Map<String, Serializable> connectorInfo = tasksWithConnectors.get(flowNodeId);
                Map<String, Serializable> connector = errorMsgs.get(connectorInfo.get("exceptionMessage"));
                if(connector == null){
                    connectorInfo.put("count", 1L);
                    errorMsgs.put((String)connectorInfo.get("exceptionMessage"), connectorInfo);
                }else {
                    connector.put("count", (Long)connector.get("count")+1);
                    errorMsgs.put((String) connectorInfo.get("exceptionMessage"), connector);
                }
            }
        }
        return new ArrayList<Map<String, Serializable>>(errorMsgs.values());
    }

    public List<Map<String, Serializable>> searchHandlingGateways(APISession apiSession, Integer minutes, Long processDefinitionId) throws BonitaHomeNotSetException, ServerAPIException, UnknownAPITypeException, SearchException {
        ProcessAPI processApi = TenantAPIAccessor.getProcessAPI(apiSession);
        SearchOptionsBuilder sob;
        Long ms = System.currentTimeMillis() - (minutes*60*60*1000);
        int size = 100;
        int i = 0;
        boolean pendingResults = true;
        List<ProcessInstance> processInstanceList = new ArrayList<ProcessInstance>();
        while(pendingResults){
            sob = new SearchOptionsBuilder(i, size);
            sob.lessOrEquals(ProcessInstanceSearchDescriptor.LAST_UPDATE, ms);
            sob.and().filter(ProcessInstanceSearchDescriptor.PROCESS_DEFINITION_ID, processDefinitionId);
            sob.sort(ProcessInstanceSearchDescriptor.PROCESS_DEFINITION_ID, Order.ASC);

            List<ProcessInstance> result = processApi.searchProcessInstances(sob.done()).getResult();
            processInstanceList.addAll(result);

            if(result.size() < size){
                pendingResults = false;
            }else{
                i = i + size;
            }
        }

        Map<ProcessInstance,List<FlowNodeInstance>> handlingGatewayCases = new HashMap<ProcessInstance,List<FlowNodeInstance>>();

        for(ProcessInstance processInstance : processInstanceList) {

            sob = new SearchOptionsBuilder(0, 100);
            sob.filter(FlowNodeInstanceSearchDescriptor.ROOT_PROCESS_INSTANCE_ID, processInstance.getRootProcessInstanceId());
            sob.and().filter(FlowNodeInstanceSearchDescriptor.STATE_NAME, "executing");
            List<FlowNodeInstance> l = processApi.searchFlowNodeInstances(sob.done()).getResult();
            for(FlowNodeInstance f : l) {

                if(f.getType().equals(FlowNodeType.GATEWAY)) {
                    List<FlowNodeInstance> li = handlingGatewayCases.get(processInstance);
                    if(li == null) {
                        li = new ArrayList<FlowNodeInstance>();
                    }
                    li.add(f);
                    handlingGatewayCases.put(processInstance, li);
                }

            }

        }
        return prepareHandlingGatewayCases(handlingGatewayCases);

    }



    /**
     * Method that will return the failed Flownodes between 2 dates
     * @param session Bonita API Session
     * @param startDateMs Date min to perform the search (if null, no date filter will be applied)
     * @param endDateMs Date max to perform the search (if null, it will not be limited)
     * @return Complex structure that will be used by the display
     * @throws SearchException
     * @throws BonitaHomeNotSetException
     * @throws ServerAPIException
     * @throws UnknownAPITypeException
     * @throws ConnectorInstanceNotFoundException
     */
    public Map<String, Map<String, Map<String, Serializable>>> searchFailedFlowNodes(APISession session, Long startDateMs, Long endDateMs) throws SearchException, BonitaHomeNotSetException, ServerAPIException, UnknownAPITypeException, ConnectorInstanceNotFoundException {
        Map<String, Map<String, List<FlowNodeInstance>>> failedFlowNodes = new HashMap<String, Map<String, List<FlowNodeInstance>>>();
        HashMap<Long, String> processDefinitions = getProcessDefinitions(session);

        Map<Long, Map<String,Serializable>> tasksWithConnectors = searchFailedConnectors(session);

        ProcessAPI p = TenantAPIAccessor.getProcessAPI(session);


        SearchOptionsBuilder sob = new SearchOptionsBuilder(0, 0);
        sob.filter(FlowNodeInstanceSearchDescriptor.STATE_NAME, "FAILED");
        if(startDateMs != null){
            sob.and().greaterOrEquals(FlowNodeInstanceSearchDescriptor.LAST_UPDATE_DATE, startDateMs);
        }
        if(endDateMs != null){
            sob.and().lessOrEquals(FlowNodeInstanceSearchDescriptor.LAST_UPDATE_DATE, endDateMs);
        }
        List<FlowNodeInstance> fnis = new ArrayList<FlowNodeInstance>();
        long total = p.searchFlowNodeInstances(sob.done()).getCount();
        int size = 500;
        int i = 0;
        boolean pendingResults = true;
        while(pendingResults){
            sob = new SearchOptionsBuilder(i, size);
            sob.filter(FlowNodeInstanceSearchDescriptor.STATE_NAME, "FAILED");
            if(startDateMs != null){
                sob.and().greaterOrEquals(FlowNodeInstanceSearchDescriptor.LAST_UPDATE_DATE, startDateMs);
            }
            if(endDateMs != null){
                sob.and().lessOrEquals(FlowNodeInstanceSearchDescriptor.LAST_UPDATE_DATE, endDateMs);
            }
            SearchResult<FlowNodeInstance> result = p.searchFlowNodeInstances(sob.done());
            List<FlowNodeInstance> r = result.getResult();
            fnis.addAll(r);

            if(r.size() < size){

                pendingResults = false;
            }else{


                i = i + size;
            }
        }
        logger.info("Total num of retrieved FailedFlowNodes is " + fnis.size());

        for(FlowNodeInstance fni : fnis){
            FlowNodeType type = fni.getType();
            if(type.toString().contains("ACTIVITY_OTHER") || type.toString().contains("TASK")){
                if(tasksWithConnectors.containsKey(fni.getId())){
                    //CONNECTOR_ON_ACTIVITY FAILED
                    Map<String, List<FlowNodeInstance>> connectors = failedFlowNodes.get(FailedFlowNodeType.CONNECTOR_ON_ACTIVITY.toString());
                    if(connectors == null){
                        connectors = new HashMap<String, List<FlowNodeInstance>>();
                    }
                    String key = generateConnectorTaskKey(fni,processDefinitions , tasksWithConnectors);
                    List<FlowNodeInstance> connector = connectors.get(key);
                    if(connector == null){
                        connector = new ArrayList<FlowNodeInstance>();
                    }
                    connector.add(fni);
                    connectors.put(key,connector);
                    failedFlowNodes.put(FailedFlowNodeType.CONNECTOR_ON_ACTIVITY.toString(), connectors);
                }else{
                    //ACTIVITY_OTHER FAILED
                    Map<String, List<FlowNodeInstance>> tasks = failedFlowNodes.get(FailedFlowNodeType.ACTIVITY_OTHER.toString());
                    if(tasks == null){
                        tasks = new HashMap<String, List<FlowNodeInstance>>();
                    }
                    String key = generateFlowNodeKey(fni, processDefinitions);
                    List<FlowNodeInstance> task = tasks.get(key);
                    if(task == null){
                        task = new ArrayList<FlowNodeInstance>();
                    }
                    task.add(fni);
                    tasks.put(key,task);
                    failedFlowNodes.put(FailedFlowNodeType.ACTIVITY_OTHER.toString(), tasks);
                }
            }else{
                //OTHER FLOWNODE THAN ACTIVITY_OTHER FAILED
                Map<String, List<FlowNodeInstance>> flowNodes = failedFlowNodes.get(fni.getType().toString());
                if(flowNodes == null){
                    flowNodes = new HashMap<String, List<FlowNodeInstance>>();
                }
                String key = generateFlowNodeKey(fni, processDefinitions);
                List<FlowNodeInstance> flowNode = flowNodes.get(key);
                if(flowNode == null){
                    flowNode = new ArrayList<FlowNodeInstance>();
                }
                flowNode.add(fni);
                flowNodes.put(key,flowNode);
                failedFlowNodes.put(fni.getType().toString(), flowNodes);
            }
        }

        return prepareFailedFlowNodesResponse(failedFlowNodes);

    }

    private Map<String,Map<String,Map<String,Serializable>>> prepareFailedFlowNodesResponse(Map<String, Map<String, List<FlowNodeInstance>>> failedFlowNodes) {

        final Map<String,Map<String,Map<String,Serializable>>> output = new HashMap<>();

        Map<String, Map<String, Serializable>> groupedResult;
        Map<String, Serializable> result;
        List<FlowNodeInstance> lFn;
        for(Map.Entry<String, Map<String, List<FlowNodeInstance>>> fnTypeEntry : failedFlowNodes.entrySet()){
            String type = fnTypeEntry.getKey();
            groupedResult = new HashMap<>();
            for(String fnKey : fnTypeEntry.getValue().keySet()){

                result = new HashMap<>();
                lFn = fnTypeEntry.getValue().get(fnKey);
                FlowNodeInstance reference = lFn.get(0);
                result.put("fnKey", fnKey);
                result.put("count", lFn.size());
                result.put("name", reference.getName());
                result.put("type", reference.getType());
                List<Long> ids = new ArrayList<Long>();
                for(FlowNodeInstance value :lFn){
                    ids.add(value.getId());
                }
                result.put("ids", (Serializable)ids);
                groupedResult.put(fnKey, result);
            }
            output.put(type, groupedResult);
        }


        return output;
    }

    private List<Map<String, Serializable>> prepareHandlingGatewayCases(Map<ProcessInstance, List<FlowNodeInstance>> handlingCases) {
        final List<Map<String, Serializable>> output = new ArrayList<Map<String, Serializable>>();
        Map<String, Serializable> caseMap;
        List<Long> idList;
        for(Map.Entry<ProcessInstance, List<FlowNodeInstance>> caseEntry :handlingCases.entrySet()){
            ProcessInstance processInstance = caseEntry.getKey();
            caseMap = new HashMap<String, Serializable>();
            caseMap.put("startDate", processInstance.getStartDate());
            caseMap.put("lastUpdateDate", processInstance.getLastUpdate());
            caseMap.put("rootCaseId", processInstance.getRootProcessInstanceId());
            idList = new ArrayList<Long>();
            for(FlowNodeInstance flowNodeInstance : caseEntry.getValue()){
                idList.add(flowNodeInstance.getId());
            }
            caseMap.put("flowNodeIds", (Serializable)idList);
            output.add(caseMap);
        }
        return output;
    }

    private Map<Long, Map<String,Serializable>> searchFailedConnectors(APISession session) throws BonitaHomeNotSetException, ServerAPIException, UnknownAPITypeException, SearchException, ConnectorInstanceNotFoundException {
        final ProcessAPI processAPI = TenantAPIAccessor.getProcessAPI(session);
        final SearchOptionsBuilder sob = new SearchOptionsBuilder(0, Integer.MAX_VALUE);
        sob.filter(ConnectorInstancesSearchDescriptor.STATE, "FAILED");
        final List<ConnectorInstance> connectorInstanceList = processAPI.searchConnectorInstances(sob.done()).getResult();
        final Map<Long, Map<String,Serializable>> tasksWithConnectors = new HashMap<Long, Map<String,Serializable>>();
        for (ConnectorInstance connectorInstance : connectorInstanceList) {
            String name = connectorInstance.getName();
            Map<String, Serializable> connectorMap = new HashMap<String, Serializable>();
            connectorMap.put("connectorId", connectorInstance.getConnectorId());
            connectorMap.put("containerType", connectorInstance.getContainerType());
            connectorMap.put("name", name);
            connectorMap.put("containerId", connectorInstance.getContainerId());
            connectorMap.put("id", connectorInstance.getId());
            ConnectorInstanceWithFailureInfo failData = processAPI.getConnectorInstanceWithFailureInformation(connectorInstance.getId());
            connectorMap.put("exceptionMessage", failData.getExceptionMessage());
            connectorMap.put("stackTrace", failData.getStackTrace());
            tasksWithConnectors.put(connectorInstance.getContainerId(), connectorMap);
        }
        return tasksWithConnectors;

    }

    /**
     * Method that generates a key for a task with a connector that failed
     * @param fni FlowNodeInstance
     * @return processName -- processVersion -- flowNodeType -- activityName -- connectorName
     */
    private  String generateConnectorTaskKey(FlowNodeInstance fni, HashMap<Long, String> processDefinitions, Map<Long, Map<String,Serializable>> tasksWithConnectors){
        final String connectorName = (String) tasksWithConnectors.get(fni.getId()).get("name");
        final String key = generateFlowNodeKey(fni, processDefinitions) +" -- "+ connectorName;
        return key;
    }

    /**
     * Method that generates a key for a flowNode without a connector that failed
     * @param flowNodeInstance FlowNodeInstance
     * @return processName -- processVersion -- flowNodeType -- activityName -- connectorName
     */
    private  String generateFlowNodeKey(FlowNodeInstance flowNodeInstance, HashMap<Long, String> processDefinitions){
        final String process = processDefinitions.get(flowNodeInstance.getProcessDefinitionId());
        final String key = process +" -- "+ flowNodeInstance.getType().toString() +" -- "+ flowNodeInstance.getName();
        //usedProcessDefinitions.add(process);
        return key;
    }


    private HashMap<Long, String> getProcessDefinitions(APISession session) throws BonitaHomeNotSetException, ServerAPIException, UnknownAPITypeException {
        final ProcessAPI p = TenantAPIAccessor.getProcessAPI(session);
        final List<ProcessDeploymentInfo> processDeploymentInfoList = p.getProcessDeploymentInfos(0, Integer.MAX_VALUE, ProcessDeploymentInfoCriterion.NAME_ASC);
        HashMap<Long, String> processDefinitions = new HashMap<Long, String>();
        final List<String> processNames = new ArrayList<String>();
        for(ProcessDeploymentInfo processDeploymentInfo : processDeploymentInfoList){
            String fullName = processDeploymentInfo.getName() +" -- "+ processDeploymentInfo.getVersion();
            processDefinitions.put(processDeploymentInfo.getProcessId(), fullName);
            processNames.add(fullName);

        }
        return processDefinitions;



    }
}
