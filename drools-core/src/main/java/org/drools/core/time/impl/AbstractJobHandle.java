/*
 * Copyright 2015 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/

package org.drools.core.time.impl;

import org.drools.base.time.JobHandle;
import org.drools.base.util.LinkedListNode;

public abstract class AbstractJobHandle implements JobHandle,
                                                   LinkedListNode<AbstractJobHandle> {

    private AbstractJobHandle previous;
    private AbstractJobHandle next;

    @Override
    public AbstractJobHandle getPrevious() {
        return previous;
    }

    @Override
    public void setPrevious(AbstractJobHandle previous) {
        this.previous = previous;
    }

    @Override
    public void nullPrevNext() {
        previous = null;
        next = null;
    }

    @Override
    public void setNext(AbstractJobHandle next) {
        this.next = next;
    }

    @Override
    public AbstractJobHandle getNext() {
        return next;
    }
}
