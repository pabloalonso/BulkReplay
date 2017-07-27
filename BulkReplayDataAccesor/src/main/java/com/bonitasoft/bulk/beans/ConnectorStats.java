package com.bonitasoft.bulk.beans;

import java.util.ArrayList;
import java.util.List;

public class ConnectorStats extends ElementStats{
    private String connectorId;

    private String exceptionMessage;
    private String stackTrace;


    public ConnectorStats(){
        super();
    }

    public ConnectorStats(String connectorId, String name, String exceptionMessage, String stackTrace) {
        super();
        this.connectorId = connectorId;
        this.setName(name);
        this.exceptionMessage = exceptionMessage;
        this.stackTrace = stackTrace;
    }

    public String getConnectorId() {
        return connectorId;
    }

    public void setConnectorId(String connectorId) {
        this.connectorId = connectorId;
    }

    public String getExceptionMessage() {
        return exceptionMessage;
    }

    public void setExceptionMessage(String exceptionMessage) {
        this.exceptionMessage = exceptionMessage;
    }

    public String getStackTrace() {
        return stackTrace;
    }

    public void setStackTrace(String stackTrace) {
        this.stackTrace = stackTrace;
    }




    public String toString(){

        return getName() +  " | " + connectorId +  " | " + getFlowNodeIds().size() +  " | " + exceptionMessage +  " | " + stackTrace;
    }

}
