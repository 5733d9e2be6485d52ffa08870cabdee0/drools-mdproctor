package org.kie.api.conf;

public interface MultiValueFlowBaseOption extends MultiValueKieBaseOption {
    default String type() {
        return SingleValueFlowBaseOption.TYPE;
    }
}
