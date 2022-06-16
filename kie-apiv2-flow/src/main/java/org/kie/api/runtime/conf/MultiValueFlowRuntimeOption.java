package org.kie.api.runtime.conf;

public interface MultiValueFlowRuntimeOption extends MultiValueKieSessionOption {
    default String getType() {
        return SingleValueFlowRuntimeOption.TYPE;
    }
}
