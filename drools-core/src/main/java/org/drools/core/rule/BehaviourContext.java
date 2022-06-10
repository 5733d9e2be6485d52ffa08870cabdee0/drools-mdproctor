package org.drools.core.rule;

import java.util.Collection;

import org.drools.core.common.EventFactHandle;
import org.drools.base.time.JobHandle;

public interface BehaviourContext {

    Collection<EventFactHandle> getFactHandles();

    default JobHandle getJobHandle() {
        return null;
    }

    default void setJobHandle(JobHandle jobHandle) {
        throw new UnsupportedOperationException();
    }
}
