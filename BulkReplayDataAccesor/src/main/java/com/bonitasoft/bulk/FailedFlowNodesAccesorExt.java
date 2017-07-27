package com.bonitasoft.bulk;

import com.bonitasoft.bulk.beans.ConnectorStats;
import com.bonitasoft.bulk.beans.FlowNodeStats;
import com.bonitasoft.engine.api.ProcessAPI;
import com.bonitasoft.engine.api.TenantAPIAccessor;
import org.bonitasoft.engine.bpm.connector.ConnectorInstance;
import org.bonitasoft.engine.bpm.connector.ConnectorInstanceNotFoundException;
import org.bonitasoft.engine.bpm.connector.ConnectorInstanceWithFailureInfo;
import org.bonitasoft.engine.bpm.connector.ConnectorInstancesSearchDescriptor;
import org.bonitasoft.engine.bpm.flownode.FlowNodeInstance;
import org.bonitasoft.engine.bpm.flownode.FlowNodeInstanceSearchDescriptor;
import org.bonitasoft.engine.bpm.process.ProcessInstance;
import org.bonitasoft.engine.bpm.process.ProcessInstanceSearchDescriptor;
import org.bonitasoft.engine.exception.BonitaHomeNotSetException;
import org.bonitasoft.engine.exception.SearchException;
import org.bonitasoft.engine.exception.ServerAPIException;
import org.bonitasoft.engine.exception.UnknownAPITypeException;
import org.bonitasoft.engine.search.SearchOptionsBuilder;
import org.bonitasoft.engine.search.SearchResult;
import org.bonitasoft.engine.session.APISession;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;



public class FailedFlowNodesAccesorExt extends FailedFlowNodesAccesor{
    private final Logger logger = Logger.getLogger("com.bonitasoft.bulk");

    public List<FlowNodeStats> getFlowNodeStats(APISession apiSession, Long processDefinitionId, Long startDateMs, Long endDateMs) throws SearchException, BonitaHomeNotSetException, ServerAPIException, UnknownAPITypeException {
        ProcessAPI processAPI = TenantAPIAccessor.getProcessAPI(apiSession);
        Map<String, FlowNodeStats> map = new HashMap<String,FlowNodeStats>();

        SearchOptionsBuilder sob, sob2;
        int size = 500;
        boolean pendingResults = true;
        int i =0, j=0;
        final List<Long> caseIds = new ArrayList<>();
        while(pendingResults) {
            sob = new SearchOptionsBuilder(i, size);
            sob.filter(ProcessInstanceSearchDescriptor.PROCESS_DEFINITION_ID, processDefinitionId);
            SearchResult<ProcessInstance> r = processAPI.searchFailedProcessInstances(sob.done());
            List<ProcessInstance> result = r.getResult();

            for(ProcessInstance failedCase : result){
                caseIds.add(failedCase.getRootProcessInstanceId());
                sob2 = new SearchOptionsBuilder(0, 1);
                sob2.filter(FlowNodeInstanceSearchDescriptor.ROOT_PROCESS_INSTANCE_ID, failedCase.getRootProcessInstanceId());
                sob2.and().filter(FlowNodeInstanceSearchDescriptor.STATE_NAME, "failed");

                if(startDateMs != null){
                    sob2.and().greaterOrEquals(FlowNodeInstanceSearchDescriptor.LAST_UPDATE_DATE, startDateMs);
                }
                if(endDateMs != null){
                    sob2.and().lessOrEquals(FlowNodeInstanceSearchDescriptor.LAST_UPDATE_DATE, endDateMs);
                }


                SearchResult<FlowNodeInstance> r2 = processAPI.searchFlowNodeInstances(sob2.done());
                List<FlowNodeInstance> result2 = r2.getResult();
                try{
                    FlowNodeInstance fni = result2.get(0);
                    FlowNodeStats fns = map.get(fni.getName());
                    if(fns == null){
                        fns = new FlowNodeStats(fni.getName(), fni.getType().toString());
                    }
                    fns.addFlowNodeId(fni.getId());
                    map.put(fni.getName(), fns);

                }catch (Exception e){
                    logger.warning("No failed flownodes on case "+ failedCase.getRootProcessInstanceId());
                }


            }

            if(result.size() < size){
                pendingResults = false;
            }else{
                i = i + size;
            }
        }
        return new ArrayList<FlowNodeStats>(map.values());

    }
    public List<ConnectorStats> getDeepFlowNodeStats(APISession apiSession, List<Long> flowNodeIds) throws SearchException, BonitaHomeNotSetException, ServerAPIException, UnknownAPITypeException, ConnectorInstanceNotFoundException {
        final ProcessAPI processAPI = TenantAPIAccessor.getProcessAPI(apiSession);

        SearchOptionsBuilder sob;
        Map<String, ConnectorStats> map = new HashMap<>();
        ConnectorStats noConnector = new ConnectorStats("no-connector","No Connector Involved", "", "");
        final List<ConnectorInstance> connectorInstanceList = new ArrayList<>();

        for(Long flowNodeId : flowNodeIds) {
            sob = new SearchOptionsBuilder(0, 10);
            sob.filter(ConnectorInstancesSearchDescriptor.CONTAINER_ID, flowNodeId);
            sob.and().filter(ConnectorInstancesSearchDescriptor.STATE, "FAILED");

            SearchResult<ConnectorInstance> r = processAPI.searchConnectorInstances(sob.done());
            List<ConnectorInstance> result = r.getResult();
            if (result.size() > 0) {
                //Connector error
                for (ConnectorInstance ci : result) {
                    ConnectorInstanceWithFailureInfo failData = processAPI.getConnectorInstanceWithFailureInformation(ci.getId());
                    ConnectorStats connectorStats = map.get(ci.getName() + "-" + failData.getExceptionMessage());
                    if (connectorStats == null) {
                        connectorStats = new ConnectorStats(ci.getConnectorId(), ci.getName(), failData.getExceptionMessage(), failData.getStackTrace());
                    }
                    connectorStats.addFlowNodeId(flowNodeId);

                    map.put(ci.getName() + "-" + failData.getExceptionMessage(), connectorStats);
                }
            } else {
                //No connector error
                noConnector.addFlowNodeId(flowNodeId);

            }
        }
        List<ConnectorStats> output = new ArrayList<>(map.values());
        if(noConnector.getFlowNodeIds().size()>0)
            output.add(noConnector);
        return output;
    }

}
