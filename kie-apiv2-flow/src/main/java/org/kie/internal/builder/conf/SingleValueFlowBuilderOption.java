package org.kie.internal.builder.conf;

public interface SingleValueFlowBuilderOption extends SingleValueKieBuilderOption {
    static String TYPE = "Flow";
    default String getType() {
        return TYPE;
    }
}
