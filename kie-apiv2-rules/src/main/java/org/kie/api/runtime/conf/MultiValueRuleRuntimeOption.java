package org.kie.api.runtime.conf;

public interface MultiValueRuleRuntimeOption extends MultiValueKieSessionOption {
    default String getType() {
        return SingleValueRuleRuntimeOption.TYPE;
    }
}
