package com.bonitasoft.bulk;

import com.bonitasoft.bulk.FailedFlowNodesAccesor;
import com.bonitasoft.engine.api.LoginAPI;
import com.bonitasoft.engine.api.TenantAPIAccessor;
import com.google.gson.Gson;
import org.apache.commons.io.FileUtils;
import org.bonitasoft.engine.api.ApiAccessType;
import org.bonitasoft.engine.bpm.flownode.FlowNodeInstance;
import org.bonitasoft.engine.session.APISession;
import org.bonitasoft.engine.util.APITypeManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Created by pablo on 04/07/2017.
 */
public class FailedFlowNodesAccesorTest {



    //private static final String SERVER_URL = "http://localhost:28232";
    private static final String SERVER_URL = "http://localhost:8081";
    private static final String BONITA = "bonita";
    private static final String USERNAME = "install";
    private static final String PASSWORD = "install";
    private static final int NUM_ELEMENTS = 4;

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
        failedFlowNodesAccesor = new FailedFlowNodesAccesor();
    }

    @After
    public void tearDown() throws Exception {
        LoginAPI loginAPI = TenantAPIAccessor.getLoginAPI();
        loginAPI.logout(apiSession);
    }

    @Test
    public void searchFailedFlowNodes() throws Exception {
        Map<String, Map<String, Map<String, Serializable>>> l = failedFlowNodesAccesor.searchFailedFlowNodes(apiSession, null, null);
        assertNotNull(l);
        assertTrue(l.size() == NUM_ELEMENTS);


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

/*
    @Test
    public void getProcessDefinitions() throws Exception {
        Set<String> l = failedFlowNodesAccesor.getProcessUsedDefinitions(apiSession);
        assertNotNull(l);

        Gson gson = new Gson();
        String json = gson.toJson(l);
        FileUtils.writeStringToFile(new File("processNames.json"), json);
    }

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