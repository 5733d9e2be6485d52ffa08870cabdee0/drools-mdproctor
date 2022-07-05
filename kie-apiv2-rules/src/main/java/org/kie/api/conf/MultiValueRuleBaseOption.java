package org.kie.api.conf;

import org.kie.internal.builder.conf.SingleValueRuleBuilderOption;

public interface MultiValueRuleBaseOption extends MultiValueKieBaseOption {
    static String TYPE = SingleValueRuleBuilderOption.TYPE;
    
    default String type() {
        return TYPE;
    }
}
