package org.drools.mvel;

import org.drools.base.util.MVELExecutor;
import org.drools.base.util.MVELExecutorProvider;

public class MVELExecutorProviderImpl implements MVELExecutorProvider {
    @Override
    public MVELExecutor get() {
        return (MVELExecutor) MVELSafeHelper.getEvaluator();
    }
}
