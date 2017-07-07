package com.bonitasoft.bulk;

import com.bonitasoft.engine.api.ProcessAPI;
import com.bonitasoft.engine.api.TenantAPIAccessor;
import org.bonitasoft.engine.bpm.connector.ConnectorInstance;
import org.bonitasoft.engine.bpm.connector.ConnectorInstanceNotFoundException;
import org.bonitasoft.engine.bpm.connector.ConnectorInstanceWithFailureInfo;
import org.bonitasoft.engine.bpm.connector.ConnectorInstancesSearchDescriptor;
import org.bonitasoft.engine.bpm.flownode.*;
import org.bonitasoft.engine.bpm.process.ProcessDeploymentInfo;
import org.bonitasoft.engine.bpm.process.ProcessDeploymentInfoCriterion;
import org.bonitasoft.engine.exception.BonitaHomeNotSetException;
import org.bonitasoft.engine.exception.SearchException;
import org.bonitasoft.engine.exception.ServerAPIException;
import org.bonitasoft.engine.exception.UnknownAPITypeException;
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
    private  Map<Long, String> tasksWithConnectors;
    private  HashMap<Long, String> processDefinitions;
    private  final Set<String> usedProcessDefinitions = new HashSet<>();
    private final Logger logger = Logger.getLogger("com.bonitasoft.bulk");

    /**
     *
     * @param session Bonita API session
     * @return
     * @throws SearchException
     * @throws BonitaHomeNotSetException
     * @throws ServerAPIException
     * @throws UnknownAPITypeException
     */
    public Map<String, Map<String, Map<String, Serializable>>> searchFailedFlowNodes(APISession session, Long startDateMs, Long endDateMs) throws SearchException, BonitaHomeNotSetException, ServerAPIException, UnknownAPITypeException, ConnectorInstanceNotFoundException {
        Map<String, Map<String, List<FlowNodeInstance>>> failedFlowNodes = new HashMap<String, Map<String, List<FlowNodeInstance>>>();
        getProcessDefinitions(session);
        Map<String, Serializable> failedConnectors = searchFailedConnectors(session);

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
                    String key = generateConnectorTaskKey(fni);
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
                    String key = generateFlowNodeKey(fni);
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
                String key = generateFlowNodeKey(fni);
                List<FlowNodeInstance> flowNode = flowNodes.get(key);
                if(flowNode == null){
                    flowNode = new ArrayList<FlowNodeInstance>();
                }
                flowNode.add(fni);
                flowNodes.put(key,flowNode);
                failedFlowNodes.put(fni.getType().toString(), flowNodes);
            }
        }

        return prepareResponse(failedFlowNodes);

    }

    private Map<String,Map<String,Map<String,Serializable>>> prepareResponse(Map<String, Map<String, List<FlowNodeInstance>>> failedFlowNodes) {

        Map<String, Serializable> result;
        Map<String, Map<String, Serializable>> groupedResult;
        Map<String,Map<String,Map<String,Serializable>>> output = new HashMap<>();

        for(Map.Entry<String, Map<String, List<FlowNodeInstance>>> fnTypeEntry : failedFlowNodes.entrySet()){
            String type = fnTypeEntry.getKey();
            groupedResult = new HashMap<>();
            for(String fnKey : fnTypeEntry.getValue().keySet()){

                result = new HashMap<>();
                List<FlowNodeInstance> lFn = fnTypeEntry.getValue().get(fnKey);
                FlowNodeInstance reference = lFn.get(0);
                result.put("count", lFn.size());
                result.put("name", reference.getName());
                result.put("type", reference.getType());
                if(reference.getType().equals(FailedFlowNodeType.CONNECTOR_ON_ACTIVITY.toString())){

                }
                groupedResult.put(fnKey, result);
            }
            output.put(type, groupedResult);
        }


        return output;
    }


    private  Map<String,Serializable> searchFailedConnectors(APISession session) throws BonitaHomeNotSetException, ServerAPIException, UnknownAPITypeException, SearchException, ConnectorInstanceNotFoundException {
        ProcessAPI p = TenantAPIAccessor.getProcessAPI(session);
        SearchOptionsBuilder sob = new SearchOptionsBuilder(0, Integer.MAX_VALUE);
        sob.filter(ConnectorInstancesSearchDescriptor.STATE, "FAILED");
        List<ConnectorInstance> connInsts = p.searchConnectorInstances(sob.done()).getResult();
        Map<String, Serializable> map = new HashMap<String, Serializable>();
        tasksWithConnectors = new HashMap<Long, String>();

        Set<String> exceptionMsg = new HashSet<>();

        for (ConnectorInstance connInst : connInsts) {
            String name = connInst.getName();
            Map<String, Serializable> connMap = (Map<String, Serializable>) map.get(name);
            if (connMap == null) {
                connMap = new HashMap<String, Serializable>();
                connMap.put("connectorId", connInst.getConnectorId());
                connMap.put("containerType", connInst.getContainerType());
                connMap.put("name", name);
                connMap.put("id-containerId", new HashMap<Long, Long>());
                ConnectorInstanceWithFailureInfo failData = p.getConnectorInstanceWithFailureInformation(connInst.getId());
                connMap.put("exceptionMessage", failData.getExceptionMessage());
                exceptionMsg.add(failData.getExceptionMessage());
//                connMap.put("stackTrace", failData.getStackTrace());
            }
            Map<Long, Long> ids = (Map<Long, Long>) connMap.get("id-containerId");
            ids.put(connInst.getId(), connInst.getContainerId());
            connMap.put("id-containerId", (Serializable) ids);
            tasksWithConnectors.put(connInst.getContainerId(), name);
            Long count = (Long) connMap.get("count");
            if (count == null) {
                count = 0L;
            }
            connMap.put("count", count + 1);
            map.put(name, (Serializable) connMap);
        }
        for(String s : exceptionMsg)
            logger.warning(s);
        return map;
    }

    /**
     * Method that generates a key for a task with a connector that failed
     * @param fni FlowNodeInstance
     * @return processName -- processVersion -- flowNodeType -- activityName -- connectorName
     */
    private  String generateConnectorTaskKey(FlowNodeInstance fni){
        String connectorName = tasksWithConnectors.get(fni.getId());
        return generateFlowNodeKey(fni) +" -- "+ connectorName;
    }

    /**
     * Method that generates a key for a flowNode without a connector that failed
     * @param fni FlowNodeInstance
     * @return processName -- processVersion -- flowNodeType -- activityName -- connectorName
     */
    private  String generateFlowNodeKey(FlowNodeInstance fni){
        String process = processDefinitions.get(fni.getProcessDefinitionId());
        String key = process +" -- "+ fni.getType().toString() +" -- "+ fni.getName();
        usedProcessDefinitions.add(process);
        return key;
    }
    public  Set<String> getProcessUsedDefinitions(APISession session) throws BonitaHomeNotSetException, ServerAPIException, UnknownAPITypeException{
        if(usedProcessDefinitions.size() > 0)
            return usedProcessDefinitions;
        else{
            return new HashSet<String>(getProcessDefinitions(session));
        }
    }

    private  List<String> getProcessDefinitions(APISession session) throws BonitaHomeNotSetException, ServerAPIException, UnknownAPITypeException {
        ProcessAPI p = TenantAPIAccessor.getProcessAPI(session);
        List<ProcessDeploymentInfo> pds = p.getProcessDeploymentInfos(0, Integer.MAX_VALUE, ProcessDeploymentInfoCriterion.NAME_ASC);
        processDefinitions = new HashMap<Long, String>();
        List<String> processNames = new ArrayList<String>();
        for(ProcessDeploymentInfo pd : pds){
            String fullName = pd.getName() +" -- "+ pd.getVersion();
            processDefinitions.put(pd.getProcessId(), fullName);
            processNames.add(fullName);

        }
        return processNames;



    }
}
