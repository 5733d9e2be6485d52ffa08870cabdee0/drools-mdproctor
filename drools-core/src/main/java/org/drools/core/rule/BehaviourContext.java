package org.drools.core.rule;

import java.util.Collection;

import org.drools.core.common.EventFactHandle;
import org.drools.core.time.JobHandle;
import org.kie.api.runtime.rule.FactHandle;

public interface BehaviourContext {

    Collection<EventFactHandle> getFactHandles();

    default JobHandle getJobHandle() {
        return null;
    }

    default void setJobHandle(JobHandle jobHandle) {
        throw new UnsupportedOperationException();
    }
}
