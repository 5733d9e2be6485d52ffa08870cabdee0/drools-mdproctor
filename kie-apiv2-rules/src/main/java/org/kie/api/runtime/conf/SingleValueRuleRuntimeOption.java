package org.kie.api.runtime.conf;

public interface SingleValueRuleRuntimeOption extends SingleValueKieSessionOption {
    static String TYPE = "Rule";
    default String getType() {
        return TYPE;
    }
}
