package org.kie.internal.builder.conf;

public interface MultiValueFlowBuilderOption extends MultiValueKieBuilderOption {
    default String getType() {
        return SingleValueFlowBuilderOption.TYPE;
    }
}
