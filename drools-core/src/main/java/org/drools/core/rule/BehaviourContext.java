package org.drools.core.rule;

import java.util.Collection;

import org.drools.core.common.EventFactHandle;
import org.drools.base.time.JobHandle;
import org.drools.core.time.impl.AbstractJobHandle;

public interface BehaviourContext {

    Collection<EventFactHandle> getFactHandles();

    default AbstractJobHandle getJobHandle() {
        return null;
    }

    default void setJobHandle(AbstractJobHandle jobHandle) {
        throw new UnsupportedOperationException();
    }
}
