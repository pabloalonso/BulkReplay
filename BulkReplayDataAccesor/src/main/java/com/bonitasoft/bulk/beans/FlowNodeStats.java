package com.bonitasoft.bulk.beans;

import java.util.ArrayList;
import java.util.List;

public class FlowNodeStats extends ElementStats{

    private String type;

    public FlowNodeStats() {
        super();
    }

    public FlowNodeStats(String name, String type){
        super();
        this.setName(name);
        this.type = type;
    }


    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
    public String toString(){
        return getName() +  " | " + type +  " | " + getFlowNodeIds().size();
    }
}
