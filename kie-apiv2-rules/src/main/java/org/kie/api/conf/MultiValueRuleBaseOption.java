package org.kie.api.conf;

public interface MultiValueRuleBaseOption extends MultiValueKieBaseOption {
    default String getType() {
        return SingleValueRuleBaseOption.TYPE;
    }
}
