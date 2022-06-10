package org.drools.base.base;

import org.drools.base.rule.Declaration;
import org.kie.api.runtime.rule.FactHandle;

public interface BaseTuple {
    FactHandle get(Declaration declaration);

    FactHandle get(int index);

    FactHandle getFactHandle();

    Object getObject(Declaration declaration);

    Object getObject(int index);

    Object getContextObject();

    int size();

    default Object[] toObjects() {
        return toObjects(false);
    }

    Object[] toObjects(boolean reverse);

    FactHandle[] toFactHandles();

    BaseTuple getParent();

    BaseTuple getTuple(int index);

    int getIndex();
}
