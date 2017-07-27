package com.bonitasoft.bulk.rest;

import com.bonitasoft.bulk.FailedFlowNodesAccesorExt;
import com.bonitasoft.bulk.beans.FlowNodeStats;
import com.bonitasoft.bulk.setup.ServletContextClass;
import org.bonitasoft.engine.bpm.connector.ConnectorInstanceNotFoundException;
import org.bonitasoft.engine.exception.BonitaHomeNotSetException;
import org.bonitasoft.engine.exception.SearchException;
import org.bonitasoft.engine.exception.ServerAPIException;
import org.bonitasoft.engine.exception.UnknownAPITypeException;
import org.bonitasoft.engine.platform.LoginException;
import org.bonitasoft.engine.platform.LogoutException;
import org.bonitasoft.engine.session.APISession;
import org.bonitasoft.engine.session.SessionNotFoundException;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Path("flowNodesStats")
public class FlowNodeStatsRest {
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getFlowNodeStats(@QueryParam("processDefinitionId") Long processDefinitionId, @QueryParam("startDate") Long startDateMs, @QueryParam("endDate") Long endDateMs) throws UnknownAPITypeException, ServerAPIException, ConnectorInstanceNotFoundException, BonitaHomeNotSetException, SearchException, LoginException, LogoutException, SessionNotFoundException {
        APISession apiSession = ServletContextClass.login();
        List<FlowNodeStats> l = ((FailedFlowNodesAccesorExt) ServletContextClass.failedFlowNodesAccesor()).getFlowNodeStats(apiSession, processDefinitionId, startDateMs, endDateMs);
        ServletContextClass.logout(apiSession);
        return Response.status(Response.Status.OK).entity(l).build();
    }
}
