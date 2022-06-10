package org.drools.base.base;

import org.drools.base.definitions.rule.RuleBase;
import org.drools.base.rule.accessor.GlobalResolver;

public interface ValueResolver {

    Object getGlobal(String name);

    long getCurrentTime();

    GlobalResolver getGlobalResolver();

    RuleBase getRuleBase();
}
