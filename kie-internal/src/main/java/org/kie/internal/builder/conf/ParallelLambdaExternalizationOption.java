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

package org.kie.internal.builder.conf;

import org.kie.api.conf.OptionKey;

/**
 * An Enum for ParallelLambdaExternalizationOption option.
 *
 * drools.parallelLambdaExternalization = &lt;true|false&gt;
 *
 * DEFAULT = true
 */
public enum ParallelLambdaExternalizationOption implements SingleValueRuleBuilderOption {

    ENABLED(true),
    DISABLED(false);

    /**
     * The property name for the parallel lambda externalization
     */
    public static final String PROPERTY_NAME = "drools.parallelLambdaExternalization";

    public static OptionKey<ParallelLambdaExternalizationOption> KEY = new OptionKey<>(TYPE, PROPERTY_NAME);

    private boolean value;

    ParallelLambdaExternalizationOption(final boolean value ) {
        this.value = value;
    }

    /**
     * {@inheritDoc}
     */
    public String getPropertyName() {
        return PROPERTY_NAME;
    }

    public boolean isLambdaExternalizationParallel() {
        return this.value;
    }

}
