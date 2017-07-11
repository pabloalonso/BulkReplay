package com.bonitasoft.bulk.setup;

import com.bonitasoft.bulk.FailedFlowNodesAccesor;
import org.bonitasoft.engine.api.ApiAccessType;
import org.bonitasoft.engine.util.APITypeManager;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.io.IOException;
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
    public static String SERVER_URL;
    public static String BONITA;
    public static String USERNAME;
    public static String PASSWORD;
    public static String COMMAND_NAME;
    public static FailedFlowNodesAccesor failedFlowNodesAccesor;

    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {
        log.info("Getting properties from config.properties");
        final Properties propsFromFile = new Properties();
        try {
            propsFromFile.load(getClass().getResourceAsStream(CONFIG_PROPERTIES));

        } catch (final IOException e) {
            e.printStackTrace();
        }

        this.SERVER_URL = propsFromFile.getProperty("SERVER_URL");
        this.BONITA = propsFromFile.getProperty("BONITA");
        this.USERNAME = propsFromFile.getProperty("USERNAME");
        this.PASSWORD = propsFromFile.getProperty("PASSWORD");
        this.COMMAND_NAME = propsFromFile.getProperty("COMMAND_NAME");

        log.info("The following configuration will be used:");
        log.info("SERVER URL "+ SERVER_URL);
        log.info("BONITA "+ BONITA);
        log.info("USERNAME "+ USERNAME);
        log.info("PASSWORD "+ PASSWORD);
        log.info("COMMAND_NAME "+ COMMAND_NAME);

        Map<String, String> settings = new HashMap<String, String>();
        settings.put("server.url", SERVER_URL);
        settings.put("application.name", BONITA);
        APITypeManager.setAPITypeAndParams(ApiAccessType.HTTP, settings);

        failedFlowNodesAccesor = new FailedFlowNodesAccesor();
    }

    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {

    }
}
