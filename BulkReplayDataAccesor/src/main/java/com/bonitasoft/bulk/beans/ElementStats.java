package com.bonitasoft.bulk.beans;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ElementStats {
    private String name;
    private List<Long> flowNodeIds = new ArrayList<>();

    private Long batchSize;
    private Long interval;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Long> getFlowNodeIds() {
        return flowNodeIds;
    }

    public void setFlowNodeIds(List<Long> flowNodeIds) {
        this.flowNodeIds = flowNodeIds;
    }
    public void addFlowNodeId(Long flowNodeId) {
        this.flowNodeIds.add(flowNodeId);
    }


    public Long getBatchSize() {
        return batchSize;
    }

    public Long getInterval() {
        return interval;
    }

    public void setBatchSize(Long batchSize) {
        this.batchSize = batchSize;
    }

    public void setInterval(Long interval) {
        this.interval = interval;
    }


}

