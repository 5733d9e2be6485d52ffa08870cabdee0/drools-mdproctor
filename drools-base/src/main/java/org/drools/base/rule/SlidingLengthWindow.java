/*
 * Copyright 2010 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.drools.base.rule;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * A length window behavior implementation
 */
public class SlidingLengthWindow
    implements
    Externalizable,
    Behavior {

    protected int size;

    public SlidingLengthWindow() {
        this( 0 );
    }

    /**
     * @param size
     */
    public SlidingLengthWindow(final int size) {
        super();
        this.size = size;
    }

    /**
     * @inheritDoc
     *
     * @see java.io.Externalizable#readExternal(java.io.ObjectInput)
     */
    public void readExternal(final ObjectInput in) throws IOException,
                                                  ClassNotFoundException {
        this.size = in.readInt();
    }

    /**
     * @inheritDoc
     *
     * @see java.io.Externalizable#writeExternal(java.io.ObjectOutput)
     */
    public void writeExternal(final ObjectOutput out) throws IOException {
        out.writeInt( this.size );

    }

    public Behavior.BehaviorType getType() {
        return Behavior.BehaviorType.LENGTH_WINDOW;
    }

    /**
     * @return the size
     */
    public long getSize() {
        return size;
    }

    /**
     * @param size the size to set
     */
    public void setSize(final int size) {
        this.size = size;
    }

    /**
     * Length windows don't change expiration offset, so
     * always return -1
     */
    public long getExpirationOffset() {
        return -1;
    }

    public String toString() {
        return "SlidingLengthWindow( size=" + size + " )";
    }


}
