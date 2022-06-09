package org.drools.core.base;

import org.drools.core.rule.Declaration;
import org.kie.api.runtime.rule.FactHandle;

public interface BaseTuple {
    FactHandle get(Declaration declaration);

    FactHandle get(int index);

    FactHandle getFactHandle();

    Object getObject(Declaration declaration);

    Object getObject(int index);

    Object getContextObject();

    int size();
}
