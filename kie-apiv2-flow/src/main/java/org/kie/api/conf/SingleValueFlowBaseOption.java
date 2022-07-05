package org.kie.api.conf;

public interface SingleValueFlowBaseOption extends SingleValueKieBaseOption {
    static String TYPE = "Flow";
    default String type() {
        return TYPE;
    }
}
