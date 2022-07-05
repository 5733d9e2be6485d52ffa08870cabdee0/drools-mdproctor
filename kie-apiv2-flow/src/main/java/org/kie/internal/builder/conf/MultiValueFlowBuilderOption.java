package org.kie.internal.builder.conf;

public interface MultiValueFlowBuilderOption extends MultiValueKieBuilderOption {
    static String TYPE = SingleValueFlowBuilderOption.TYPE;

    default String type() {
        return TYPE;
    }
}
