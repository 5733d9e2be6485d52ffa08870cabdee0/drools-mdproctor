package org.kie.api.conf;

public interface SingleValueRuleBaseOption extends SingleValueKieBaseOption {
    static String TYPE = "Rule";
    default String getType() {
        return TYPE;
    }
}
