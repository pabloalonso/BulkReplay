package com.bonitasoft.bulk;

import org.bonitasoft.engine.bpm.flownode.FlowNodeType;

/**
 * Created by pablo on 05/07/2017.
 */
public enum FailedFlowNodeType {

    CONNECTOR_ON_ACTIVITY,

    ACTIVITY_OTHER,

    USER_TASK,

    MANUAL_TASK,

    SEND_TASK,

    RECEIVE_TASK,

    CALL_ACTIVITY,

    LOOP_ACTIVITY,

    MULTI_INSTANCE_ACTIVITY,

    SUB_PROCESS,

    GATEWAY,

    START_EVENT,

    INTERMEDIATE_CATCH_EVENT,

    BOUNDARY_EVENT,

    INTERMEDIATE_THROW_EVENT,

    END_EVENT;

}
