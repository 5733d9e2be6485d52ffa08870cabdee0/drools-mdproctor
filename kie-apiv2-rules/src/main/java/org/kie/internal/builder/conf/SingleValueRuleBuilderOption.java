package org.kie.internal.builder.conf;

public interface SingleValueRuleBuilderOption extends SingleValueKieBuilderOption {
    static String TYPE = "Rule";
    default String getType() {
        return TYPE;
    }
}
