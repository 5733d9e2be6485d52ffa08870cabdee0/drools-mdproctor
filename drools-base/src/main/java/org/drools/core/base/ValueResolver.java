package org.drools.core.base;

import org.drools.core.definitions.rule.RuleBase;
import org.drools.core.rule.accessor.GlobalResolver;

public interface ValueResolver {

    Object getGlobal(String name);

    long getCurrentTime();

    GlobalResolver getGlobalResolver();

    RuleBase getRuleBase();
}
