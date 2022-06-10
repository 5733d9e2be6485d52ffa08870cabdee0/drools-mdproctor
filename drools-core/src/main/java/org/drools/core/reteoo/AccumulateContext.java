package org.drools.core.reteoo;

import org.drools.core.common.PropagationContext;
import org.drools.base.rule.AccumulateContextEntry;

public class AccumulateContext extends AccumulateContextEntry implements AccumulateNode.BaseAccumulation {

    private PropagationContext propagationContext;

    public AccumulateContext() {
        super(null);
    }

    public PropagationContext getPropagationContext() {
        return propagationContext;
    }

    public void setPropagationContext(PropagationContext propagationContext) {
        this.propagationContext = propagationContext;
    }
}
