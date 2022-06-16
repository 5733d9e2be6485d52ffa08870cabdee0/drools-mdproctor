package org.kie.api.conf;

public interface SingleValueFlowBaseOption extends SingleValueKieBaseOption {
    static String TYPE = "FlowBaseOption";
    default String getType() {
        return TYPE;
    }
}
