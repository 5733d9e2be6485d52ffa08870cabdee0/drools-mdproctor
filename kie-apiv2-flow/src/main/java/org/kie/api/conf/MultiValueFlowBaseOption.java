package org.kie.api.conf;

public interface MultiValueFlowBaseOption extends MultiValueKieBaseOption {
    default String getType() {
        return SingleValueFlowBaseOption.TYPE;
    }
}
