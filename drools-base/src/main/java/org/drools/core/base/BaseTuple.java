package org.drools.core.base;

import org.drools.core.rule.Declaration;
import org.kie.api.runtime.rule.FactHandle;

public interface BaseTuple {
    FactHandle get(Declaration declaration);

    FactHandle getFactHandle();

    Object getObject(Declaration declaration);

    Object getObject(int tupleIndex);

    Object getContextObject();

    int size();
}
