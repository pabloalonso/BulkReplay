package com.bonitasoft.bulk.beans;

import java.util.List;

/**
 * Created by pablo on 10/07/2017.
 * Example:
 *  {
 *  errorPool -- 1.0 -- GATEWAY -- 4: {
 *  count: 500,
 *  name: "4",
 *  ids: [],
 *  type: "GATEWAY"
 *  }
 */
public class FlowNodesToReplay {
    private String fnKey;
    private Long count;
    private String name;
    private List<Long> ids;
    private String type;
    private Long batchSize;
    private Long interval;


    public String getFnKey() {
        return fnKey;
    }

    public void setFnKey(String fnKey) {
        this.fnKey = fnKey;
    }

    public Long getCount() {
        return count;
    }

    public void setCount(Long count) {
        this.count = count;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Long> getIds() {
        return ids;
    }

    public void setIds(List<Long> ids) {
        this.ids = ids;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Long getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(Long batchSize) {
        this.batchSize = batchSize;
    }

    public Long getInterval() {
        return interval;
    }

    public void setInterval(Long interval) {
        this.interval = interval;
    }

    public String toString(){
        String o = "";
        o += "Key: "+ fnKey + " | batchSize: " + batchSize + " | interval: " + interval + " | ids: " + ids.toString();
        return o;
    }
}
