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
 *
 */

package org.drools.core;

import java.io.Externalizable;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.drools.core.process.WorkItemManagerFactory;
import org.drools.core.time.impl.TimerJobFactoryManager;
import org.drools.util.StringUtils;
import org.kie.api.KieBase;
import org.kie.api.conf.OptionKey;
import org.kie.api.runtime.Environment;
import org.kie.api.runtime.ExecutableRunner;
import org.kie.api.runtime.KieSessionConfiguration;
import org.kie.api.runtime.conf.AccumulateNullPropagationOption;
import org.kie.api.runtime.conf.BeliefSystemTypeOption;
import org.kie.api.runtime.conf.ClockTypeOption;
import org.kie.api.runtime.conf.DirectFiringOption;
import org.kie.api.runtime.conf.KeepReferenceOption;
import org.kie.api.runtime.conf.KieSessionOption;
import org.kie.api.runtime.conf.MultiValueKieSessionOption;
import org.kie.api.runtime.conf.QueryListenerOption;
import org.kie.api.runtime.conf.SingleValueKieSessionOption;
import org.kie.api.runtime.conf.ThreadSafeOption;
import org.kie.api.runtime.conf.TimedRuleExecutionFilter;
import org.kie.api.runtime.conf.TimedRuleExecutionOption;
import org.kie.api.runtime.conf.TimerJobFactoryOption;
import org.kie.api.runtime.conf.WorkItemHandlerOption;
import org.kie.api.runtime.process.WorkItemHandler;
import org.kie.internal.runtime.conf.ForceEagerActivationFilter;
import org.kie.internal.runtime.conf.ForceEagerActivationOption;

public abstract class SessionConfiguration implements KieSessionConfiguration, Externalizable {

    public static SessionConfiguration newInstance() {
        return new SessionConfigurationImpl();
    }

    public static SessionConfiguration newInstance(Properties properties) {
        return new SessionConfigurationImpl(properties);
    }


    public abstract ClockType getClockType();
    public abstract void setClockType(ClockType clockType);

    public abstract ExecutableRunner getRunner( KieBase kbase, Environment environment );

    public final <T extends KieSessionOption> void setOption(T option) {
        switch (option.name()) {
            case ClockTypeOption.PROPERTY_NAME: {
                setClockType(ClockType.resolveClockType(((ClockTypeOption) option).getClockType()));
            }
        }
    }

    @SuppressWarnings("unchecked")
    public final <T extends SingleValueKieSessionOption> T getOption(OptionKey<T> option) {
        switch (option.name()) {
            case ClockTypeOption.PROPERTY_NAME: {
                return (T) ClockTypeOption.get( getClockType().toExternalForm() );
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public final <T extends MultiValueKieSessionOption> T getOption(OptionKey<T> option,
                                                                    String subKey) {

        return null;
    }

    @Override public <C extends MultiValueKieSessionOption> Set<String> getOptionSubKeys(OptionKey<C> optionKey) {
        return Collections.emptySet();
    }

    public final void setProperty(String name,
                                  String value) {
        name = name.trim();
        if ( StringUtils.isEmpty( name ) ) {
            return;
        }
        switch(name) {
            case ClockTypeOption.PROPERTY_NAME: {
                setClockType(ClockType.resolveClockType(StringUtils.isEmpty(value) ? "realtime" : value));
            }
        }
    }

    public final String getProperty(String name) {
        name = name.trim();
        if ( StringUtils.isEmpty( name ) ) {
            return null;
        }

        switch(name) {
            case ClockTypeOption.PROPERTY_NAME: {
                return getClockType().toExternalForm();
            }
        }
        return null;
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SessionConfiguration that = (SessionConfiguration) o;

        return getClockType() == that.getClockType();
    }

    @Override
    public final int hashCode() {
        int result = getClockType().hashCode();
        return result;
    }
}
