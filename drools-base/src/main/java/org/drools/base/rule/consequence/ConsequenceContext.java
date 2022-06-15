/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates.
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

package org.drools.base.rule.consequence;

import org.drools.base.base.BaseTuple;
import org.drools.base.rule.Declaration;
import org.drools.core.util.bitmask.BitMask;
import org.kie.api.definition.rule.Rule;
import org.kie.api.runtime.rule.FactHandle;
import org.kie.api.runtime.rule.Match;

public interface ConsequenceContext {

    void reset();


    /**
     * Asserts an object
     *
     * @param object -
     *            the object to be asserted
     */
    FactHandle insert(Object object) ;

    FactHandle insertAsync(Object object );

    /**
     * Asserts an object specifying that it implement the onPropertyChange
     * listener
     *
     * @param object -
     *            the object to be asserted
     * @param dynamic -
     *            specifies the object implements onPropertyChangeListener
     */
    FactHandle insert(Object object, boolean dynamic);

    FactHandle insertLogical(Object object) ;

    FactHandle getFactHandle(Object object);

    void update(FactHandle handle, Object newObject);

    void update(FactHandle newObject);
    void update(FactHandle newObject, BitMask mask, Class<?> modifiedClass);

    void update(Object newObject);
    void update(Object newObject, BitMask mask, Class<?> modifiedClass);

    /**
     * @deprecated Use delete
     */
    void retract(FactHandle handle) ;

    /**
     * @deprecated Use delete
     */
    void retract(Object handle);


    void delete(Object handle);
    void delete(Object object, FactHandle.State fhState);

    void delete(FactHandle handle);
    void delete(FactHandle handle, FactHandle.State fhState);

    Object get(Declaration declaration);

    /**
     * @return - The rule name
     */
    Rule getRule();

    BaseTuple getTuple();

    Match getMatch();

    void setFocus(String focus);

    Declaration getDeclaration(String identifier);

    void halt();

    <T> T getContext(Class<T> contextClass);

    ClassLoader getProjectClassLoader();
}
