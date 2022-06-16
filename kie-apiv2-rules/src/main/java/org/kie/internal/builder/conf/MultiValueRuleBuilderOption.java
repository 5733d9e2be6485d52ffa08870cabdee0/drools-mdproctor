package org.kie.internal.builder.conf;

public interface MultiValueRuleBuilderOption extends MultiValueKieBuilderOption {
    default String getType() {
        return SingleValueRuleBuilderOption.TYPE;
    }
}
