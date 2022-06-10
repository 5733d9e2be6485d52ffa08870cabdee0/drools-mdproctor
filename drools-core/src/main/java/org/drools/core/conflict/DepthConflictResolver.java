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

package org.drools.core.conflict;

import org.drools.core.rule.consequence.Activation;
import org.drools.base.rule.consequence.ConflictResolver;
import org.kie.api.runtime.rule.Match;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class DepthConflictResolver
        implements
        ConflictResolver, Externalizable {
    private static final long                 serialVersionUID = 510l;
    public static final DepthConflictResolver INSTANCE         = new DepthConflictResolver();

    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
    }

    public void writeExternal(ObjectOutput out) throws IOException {
    }

    public static ConflictResolver getInstance() {
        return DepthConflictResolver.INSTANCE;
    }

    public final int compare(final Match existing,
                             final Match adding) {
        final int s1 = ((Activation)existing).getSalience();
        final int s2 = ((Activation)adding).getSalience();

        if ( s1 > s2 ) {
            return 1;
        } else if ( s1 < s2 ) {
            return -1;
        }

        // we know that no two activations will have the same number
        return (int) ( ((Activation)existing).getActivationNumber() - ((Activation)adding).getActivationNumber() );
    }

}
