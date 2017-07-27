package com.bonitasoft.bulk.rest;

import com.bonitasoft.bulk.FailedFlowNodesAccesorExt;
import com.bonitasoft.bulk.beans.ConnectorStats;
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

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.logging.Logger;

@Path("deepFlowNodesStats")
public class DeepFlowNodeStatsRest {
    private final Logger logger = Logger.getLogger("com.bonitasoft.bulk.rest");
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response getDeepFlowNodeStats(FlowNodeStats flowNodeStats) throws UnknownAPITypeException, ServerAPIException, ConnectorInstanceNotFoundException, BonitaHomeNotSetException, SearchException, LoginException, LogoutException, SessionNotFoundException {
        APISession apiSession = ServletContextClass.login();
        logger.severe(flowNodeStats.getFlowNodeIds().toString());
        List<ConnectorStats> l = ((FailedFlowNodesAccesorExt) ServletContextClass.failedFlowNodesAccesor()).getDeepFlowNodeStats(apiSession, flowNodeStats.getFlowNodeIds());
        ServletContextClass.logout(apiSession);
        return Response.status(Response.Status.OK).entity(l).build();
    }
}
