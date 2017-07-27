package com.bonitasoft.bulk.setup;

import com.bonitasoft.bulk.FailedFlowNodesAccesor;
import com.bonitasoft.bulk.FailedFlowNodesAccesorExt;
import org.bonitasoft.engine.api.ApiAccessType;
import org.bonitasoft.engine.api.LoginAPI;
import org.bonitasoft.engine.api.TenantAPIAccessor;
import org.bonitasoft.engine.exception.BonitaHomeNotSetException;
import org.bonitasoft.engine.exception.ServerAPIException;
import org.bonitasoft.engine.exception.UnknownAPITypeException;
import org.bonitasoft.engine.platform.LoginException;
import org.bonitasoft.engine.platform.LogoutException;
import org.bonitasoft.engine.session.APISession;
import org.bonitasoft.engine.session.SessionNotFoundException;
import org.bonitasoft.engine.util.APITypeManager;
import org.bonitasoft.engine.authentication.AuthenticationConstants;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Created by pablo on 06/07/2017.
 */
public class ServletContextClass implements ServletContextListener {

    private final String CONFIG_PROPERTIES = "/config.properties";
    private final Logger log = Logger.getLogger("com.bonitasoft.bulk");
    private static String SERVER_URL;
    private static String BONITA;
    private static String USERNAME;
    private static String PASSWORD;

    private static FailedFlowNodesAccesor failedFlowNodesAccesor;
    private static String CAS_SERVICE;
    private static String CAS_TICKET;
    public static Long BULK_MIN_SECONDS;
    public static Long BULK_MAX_BATCH_SIZE;
    private static Integer AUTH_TYPE;

    public static String RETRY_COMMAND_NAME;
    public static String DELETE_COMMAND_NAME;

    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {
        log.info("Getting properties from config.properties");
        final Properties propsFromFile = new Properties();
        try {
            propsFromFile.load(getClass().getResourceAsStream(CONFIG_PROPERTIES));


            this.SERVER_URL = propsFromFile.getProperty("SERVER_URL");
            this.BONITA = propsFromFile.getProperty("BONITA");
            this.RETRY_COMMAND_NAME = propsFromFile.getProperty("RETRY_COMMAND_NAME");
            this.DELETE_COMMAND_NAME = propsFromFile.getProperty("DELETE_COMMAND_NAME");


            this.AUTH_TYPE = new Integer(propsFromFile.getProperty("AUTH_TYPE"));
            this.USERNAME = propsFromFile.getProperty("USERNAME");
            this.PASSWORD = propsFromFile.getProperty("PASSWORD");
            this.CAS_SERVICE = propsFromFile.getProperty("CAS_SERVICE");
            this.CAS_TICKET = propsFromFile.getProperty("CAS_TICKET");
            this.BULK_MIN_SECONDS = new Long(propsFromFile.getProperty("BULK_MIN_SECONDS"));
            this.BULK_MAX_BATCH_SIZE = new Long(propsFromFile.getProperty("BULK_MAX_BATCH_SIZE"));


            switch (AUTH_TYPE) {
                case 1:
                    log.info("Auth will use CAS_TICKET: " + CAS_TICKET);
                    break;
                case 2:
                    log.info("Auth will use CAS_SERVICE: " + CAS_SERVICE);
                    break;
                default:
                    log.info("Auth will use BASIC AUTHETICATION ");
                    break;
            }

            Map<String, String> settings = new HashMap<String, String>();
            settings.put("server.url", SERVER_URL);
            settings.put("application.name", BONITA);
            APITypeManager.setAPITypeAndParams(ApiAccessType.HTTP, settings);

            failedFlowNodesAccesor = new FailedFlowNodesAccesorExt();
        }catch (final IOException e) {
            log.severe("Properties File "+CONFIG_PROPERTIES+ "could not be handled. " + e.getMessage());
        }
    }

    public static FailedFlowNodesAccesor failedFlowNodesAccesor(){
        return failedFlowNodesAccesor;
    }

    public static APISession login() throws BonitaHomeNotSetException, ServerAPIException, UnknownAPITypeException, LoginException {
        LoginAPI loginAPI = TenantAPIAccessor.getLoginAPI();
        Map<String, Serializable> loginParams = new HashMap<String, Serializable>();
        APISession apiSession;
        switch(AUTH_TYPE){

            case 1:     loginParams.put(AuthenticationConstants.CAS_TICKET, CAS_TICKET );
                        apiSession = loginAPI.login(loginParams);
                        break;
            case 2:     loginParams.put(AuthenticationConstants.CAS_SERVICE, CAS_SERVICE);
                        apiSession = loginAPI.login(loginParams);
                        break;
            default:    apiSession = loginAPI.login(USERNAME, PASSWORD);
                        break;
        }
        return apiSession;
    }
    public static void logout(APISession apiSession) throws LogoutException, SessionNotFoundException, BonitaHomeNotSetException, ServerAPIException, UnknownAPITypeException {
        LoginAPI loginAPI = TenantAPIAccessor.getLoginAPI();
        loginAPI.logout(apiSession);
    }
    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {

    }
}
