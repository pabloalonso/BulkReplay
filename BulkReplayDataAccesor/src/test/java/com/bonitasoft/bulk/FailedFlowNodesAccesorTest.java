package com.bonitasoft.bulk;

import com.bonitasoft.bulk.FailedFlowNodesAccesor;
import com.bonitasoft.bulk.beans.ConnectorStats;
import com.bonitasoft.bulk.beans.FlowNodeStats;
import com.bonitasoft.engine.api.LoginAPI;
import com.bonitasoft.engine.api.TenantAPIAccessor;

import org.apache.commons.io.FileUtils;
import org.bonitasoft.engine.api.ApiAccessType;
import org.bonitasoft.engine.authentication.AuthenticationConstants;
import org.bonitasoft.engine.bpm.flownode.FlowNodeInstance;
import org.bonitasoft.engine.bpm.flownode.FlowNodeType;
import org.bonitasoft.engine.session.APISession;
import org.bonitasoft.engine.util.APITypeManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.Serializable;
import java.util.*;

import static org.junit.Assert.*;

/**
 * Created by pablo on 04/07/2017.
 */
public class FailedFlowNodesAccesorTest {



    //private static final String SERVER_URL = "http://localhost:28232";
    private static final String SERVER_URL = "http://localhost:8080";


    private static final String BONITA = "bonita";

    private static final String USERNAME = "install";
    private static final String PASSWORD = "install";
    private static final int NUM_ELEMENTS = 4;

    private static final String PROD_SERVER_URL = "http://gap2bpmp";
    private static final String CAS_TICKET = "ST-679657-KY2GzB943fAwfbJMJ1Fj-loginkrb1";



    private APISession apiSession;
    private FailedFlowNodesAccesor failedFlowNodesAccesor;

    @Before
    public void setUp() throws Exception {
        Map<String, String> settings = new HashMap<String, String>();
        settings.put("server.url", SERVER_URL);
        settings.put("application.name", BONITA);
        APITypeManager.setAPITypeAndParams(ApiAccessType.HTTP, settings);
        LoginAPI loginAPI = TenantAPIAccessor.getLoginAPI();
        apiSession = loginAPI.login(USERNAME, PASSWORD);
        failedFlowNodesAccesor = new FailedFlowNodesAccesorExt();
    }

    //@Before
    public void prodSetUp() throws Exception {
        System.out.println("Login ");
        Map<String, String> settings = new HashMap<String, String>();
        settings.put("server.url", PROD_SERVER_URL);
        settings.put("application.name", BONITA);
        APITypeManager.setAPITypeAndParams(ApiAccessType.HTTP, settings);
        LoginAPI loginAPI = TenantAPIAccessor.getLoginAPI();
        Map<String, Serializable> loginParams = new HashMap<String, Serializable>();
        loginParams.put(AuthenticationConstants.CAS_TICKET, CAS_TICKET);
        apiSession = loginAPI.login(loginParams);
        System.out.println("Login Done");
        failedFlowNodesAccesor = new FailedFlowNodesAccesorExt();
        System.out.println("New failedFlowNodesAccesor created");
    }


    @After
    public void tearDown() throws Exception {
        System.out.println("Logout");
        LoginAPI loginAPI = TenantAPIAccessor.getLoginAPI();
        loginAPI.logout(apiSession);
        System.out.println("Logout Done");
    }
    //@Test
    public void searchFailedFlowNodes2() throws Exception {
        System.out.println("Starting searchFailedFlowNodes Test");
        System.out.println(new Date());
        List<FlowNodeStats> l = ((FailedFlowNodesAccesorExt) failedFlowNodesAccesor).getFlowNodeStats(apiSession, 6787088739119895609L, null, null);
        assertNotNull(l);
        System.out.println("End searchFailedFlowNodes Test");
        System.out.println(new Date());
        System.out.println(l);

        for(FlowNodeStats f :l){
            if(f.getType().equals(FlowNodeType.AUTOMATIC_TASK.toString())){
                System.out.println("Starting searchConnectors Test");
                System.out.println(new Date());
                List<ConnectorStats> fs = ((FailedFlowNodesAccesorExt) failedFlowNodesAccesor).getDeepFlowNodeStats(apiSession, f.getFlowNodeIds());
                assertNotNull(fs);
                System.out.println("End searchConnectors Test");
                System.out.println(new Date());
                System.out.println(fs);
                //break;
            }
        }

//        assertTrue(l.size() == NUM_ELEMENTS);


    }

//    @Test
    public void searchFailedFlowNodes() throws Exception {
        System.out.println("Starting searchFailedFlowNodes Test");
        Map<String, Map<String, Map<String, Serializable>>> l = failedFlowNodesAccesor.searchFailedFlowNodes(apiSession, null, null);
        assertNotNull(l);
        System.out.println(l);
//        assertTrue(l.size() == NUM_ELEMENTS);


    }
/*
    @Test
    public void searchFailedFlowNodesAndGenerateFile() throws Exception {
        Map<String, Map<String, Map<String, Serializable>>> l = failedFlowNodesAccesor.searchFailedFlowNodes(apiSession, null, null);
        assertNotNull(l);

        Gson gson = new Gson();
        String json = gson.toJson(l);
        FileUtils.writeStringToFile(new File("failedFlowNode.json"), json);

    }
    */


//    @Test
    public void getProcessDefinitions() throws Exception {
        Set<String> l = failedFlowNodesAccesor.getProcessUsedDefinitions(apiSession);
        System.out.println(l);
        assertNotNull(l);
/*
        Gson gson = new Gson();
        String json = gson.toJson(l);
        FileUtils.writeStringToFile(new File("processNames.json"), json);
        */
    }
/*
    @Test
    public void searchFailedFlowNodesBetweenDates() throws Exception {
        Map<String, Map<String, Map<String, Serializable>>> l = failedFlowNodesAccesor.searchFailedFlowNodes(apiSession, System.currentTimeMillis()-(50L*  24 * 60 * 60 * 1000), System.currentTimeMillis());
        assertNotNull(l);
        System.out.println(l);
        assertTrue(l.size() == NUM_ELEMENTS -2);
    }

    @Test
    public void searchFailedFlowNodesStartDate() throws Exception {
        Map<String, Map<String, Map<String, Serializable>>> l = failedFlowNodesAccesor.searchFailedFlowNodes(apiSession, System.currentTimeMillis() - (50L* 24 * 60 * 60 * 1000), null);
        assertNotNull(l);
        System.out.println(l.size());
        assertTrue(l.size() == NUM_ELEMENTS -2);
    }
    @Test
    public void searchFailedFlowNodesBetweenDatesWithoutData() throws Exception {
        Map<String, Map<String, Map<String, Serializable>>> l = failedFlowNodesAccesor.searchFailedFlowNodes(apiSession, System.currentTimeMillis()-(1000), System.currentTimeMillis());
        assertNotNull(l);
        assertTrue(l.size() == 0);
    }

    @Test
    public void searchFailedFlowNodesStartDateWithoutData() throws Exception {
        Map<String, Map<String, Map<String, Serializable>>> l = failedFlowNodesAccesor.searchFailedFlowNodes(apiSession, System.currentTimeMillis() , null);
        assertNotNull(l);
        assertTrue(l.size() == 0);
    }


*/
}