package org.kie.api.runtime.conf;

public interface SingleValueFlowRuntimeOption extends SingleValueKieSessionOption {
    static String TYPE = "Flow";
    default String type() {
        return TYPE;
    }
}
