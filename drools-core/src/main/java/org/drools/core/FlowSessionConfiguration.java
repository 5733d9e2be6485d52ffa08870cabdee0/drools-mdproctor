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

public abstract class FlowSessionConfiguration implements KieSessionConfiguration, Externalizable {

//    public static FlowSessionConfiguration newInstance() {
//        return new SessionConfigurationImpl();
//    }
//
//    public static FlowSessionConfiguration newInstance(Properties properties) {
//        return new SessionConfigurationImpl(properties);
//    }

    public abstract TimerJobFactoryType getTimerJobFactoryType();
    public abstract void setTimerJobFactoryType(TimerJobFactoryType timerJobFactoryType);

    public final TimerJobFactoryManager getTimerJobFactoryManager() {
        return getTimerJobFactoryType().createInstance();
    }

    public abstract Map<String, WorkItemHandler> getWorkItemHandlers();
    public abstract Map<String, WorkItemHandler> getWorkItemHandlers(Map<String, Object> params);
    public abstract WorkItemManagerFactory getWorkItemManagerFactory();
    public abstract void setWorkItemManagerFactory(WorkItemManagerFactory workItemManagerFactory);

    public abstract String getProcessInstanceManagerFactory();

    public abstract String getSignalManagerFactory();


    public final <T extends KieSessionOption> void setOption(T option) {
        switch (option.name()) {
            case TimerJobFactoryOption.PROPERTY_NAME: {
                setTimerJobFactoryType(TimerJobFactoryType.resolveTimerJobFactoryType(((TimerJobFactoryOption) option).getTimerJobType()));
            }
            case WorkItemHandlerOption.PROPERTY_NAME: {
                getWorkItemHandlers().put(((WorkItemHandlerOption) option).getName(),
                                          ((WorkItemHandlerOption) option).getHandler());
            }
        }
    }

    @SuppressWarnings("unchecked")
    public final <T extends SingleValueKieSessionOption> T getOption(OptionKey<T> option) {
        switch (option.name()) {
            case TimerJobFactoryOption.PROPERTY_NAME: {
                return (T) TimerJobFactoryOption.get( getTimerJobFactoryType().toExternalForm() );
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public final <T extends MultiValueKieSessionOption> T getOption(OptionKey<T> option,
                                                                    String subKey) {
        switch(option.name()) {
            case WorkItemHandlerOption.PROPERTY_NAME: {
                return (T) WorkItemHandlerOption.get(subKey,
                                                     getWorkItemHandlers().get(subKey));
            }
        }
        return null;
    }

    @Override public <C extends MultiValueKieSessionOption> Set<String> getOptionSubKeys(OptionKey<C> optionKey) {
        return getWorkItemHandlers().keySet();
    }

    public final void setProperty(String name,
                                  String value) {
        name = name.trim();
        if ( StringUtils.isEmpty( name ) ) {
            return;
        }
        switch(name) {
            case TimerJobFactoryOption.PROPERTY_NAME: {
                setTimerJobFactoryType(TimerJobFactoryType.resolveTimerJobFactoryType(StringUtils.isEmpty(value) ? "default" : value));
            }
        }
    }

    public final String getProperty(String name) {
        name = name.trim();
        if ( StringUtils.isEmpty( name ) ) {
            return null;
        }

        switch(name) {
            case TimerJobFactoryOption.PROPERTY_NAME: {
                return getTimerJobFactoryType().toExternalForm();
            }
        }
        return null;
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FlowSessionConfiguration that = (FlowSessionConfiguration) o;

        return getTimerJobFactoryType() == that.getTimerJobFactoryType();
    }

    @Override
    public final int hashCode() {
        int result = getTimerJobFactoryType().hashCode();
        return result;
    }
}
