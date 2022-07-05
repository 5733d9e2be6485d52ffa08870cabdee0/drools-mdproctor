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

public abstract class RuleSessionConfiguration implements KieSessionConfiguration, Externalizable {

//    public static RuleSessionConfiguration newInstance() {
//        return new SessionConfigurationImpl();
//    }
//
//    public static RuleSessionConfiguration newInstance(Properties properties) {
//        return new SessionConfigurationImpl(properties);
//    }
    public abstract void setDirectFiring(boolean directFiring);
    public abstract boolean isDirectFiring();
    public abstract void setThreadSafe(boolean threadSafe);
    public abstract boolean isThreadSafe();
    public abstract void setAccumulateNullPropagation(boolean accumulateNullPropagation);
    public abstract boolean isAccumulateNullPropagation();

    public abstract void setForceEagerActivationFilter(ForceEagerActivationFilter forceEagerActivationFilter);
    public abstract ForceEagerActivationFilter getForceEagerActivationFilter();

    public final boolean hasForceEagerActivationFilter() {
        try {
            return getForceEagerActivationFilter().accept(null);
        } catch (Exception e) {
            return true;
        }
    }

    public abstract RuleSessionConfiguration addDefaultProperties(Properties properties);

    public abstract void setTimedRuleExecutionFilter(TimedRuleExecutionFilter timedRuleExecutionFilter);
    public abstract TimedRuleExecutionFilter getTimedRuleExecutionFilter();

    public abstract BeliefSystemType getBeliefSystemType();
    public abstract void setBeliefSystemType(BeliefSystemType beliefSystemType);

    public abstract QueryListenerOption getQueryListenerOption();
    public abstract void setQueryListenerOption( QueryListenerOption queryListener );

    public final <T extends KieSessionOption> void setOption(T option) {
        switch (option.name()) {
            case DirectFiringOption.PROPERTY_NAME: {
                setDirectFiring(((DirectFiringOption) option).isDirectFiring());
            }
            case ThreadSafeOption.PROPERTY_NAME: {
                setThreadSafe(((ThreadSafeOption) option).isThreadSafe());
            }
            case AccumulateNullPropagationOption.PROPERTY_NAME: {
                setAccumulateNullPropagation(((AccumulateNullPropagationOption) option).isAccumulateNullPropagation());
            }
            case ForceEagerActivationOption.PROPERTY_NAME: {
                setForceEagerActivationFilter(((ForceEagerActivationOption) option).getFilter());
            }
            case TimedRuleExecutionOption.PROPERTY_NAME: {
                setTimedRuleExecutionFilter(((TimedRuleExecutionOption) option).getFilter());
            }
            case QueryListenerOption.PROPERTY_NAME: {
                setQueryListenerOption((QueryListenerOption) option);
            }
            case BeliefSystemTypeOption.PROPERTY_NAME: {
                setBeliefSystemType(((BeliefSystemType.resolveBeliefSystemType(((BeliefSystemTypeOption) option).getBeliefSystemType()))));
            }
        }
    }

    @SuppressWarnings("unchecked")
    public final <T extends SingleValueKieSessionOption> T getOption(OptionKey<T> option) {
        switch (option.name()) {
            case DirectFiringOption.PROPERTY_NAME: {
                return (T) (isDirectFiring() ? DirectFiringOption.YES : DirectFiringOption.NO);
            }
            case ThreadSafeOption.PROPERTY_NAME: {
                return (T) (isThreadSafe() ? ThreadSafeOption.YES : ThreadSafeOption.NO);
            }
            case AccumulateNullPropagationOption.PROPERTY_NAME: {
                return (T) (isAccumulateNullPropagation() ? AccumulateNullPropagationOption.YES : AccumulateNullPropagationOption.NO);
            }
            case QueryListenerOption.PROPERTY_NAME: {
                return (T) getQueryListenerOption();
            }
            case BeliefSystemTypeOption.PROPERTY_NAME: {
                return (T) BeliefSystemTypeOption.get( this.getBeliefSystemType().getId() );
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
            case DirectFiringOption.PROPERTY_NAME: {
                setDirectFiring(!StringUtils.isEmpty(value) && Boolean.parseBoolean(value));
            }
            case ThreadSafeOption.PROPERTY_NAME: {
                setThreadSafe(StringUtils.isEmpty(value) || Boolean.parseBoolean(value));
            }
            case AccumulateNullPropagationOption.PROPERTY_NAME: {
                setAccumulateNullPropagation(!StringUtils.isEmpty(value) && Boolean.parseBoolean(value));
            }
            case ForceEagerActivationOption.PROPERTY_NAME: {
                setForceEagerActivationFilter(ForceEagerActivationOption.resolve(StringUtils.isEmpty(value) ? "false" : value).getFilter());
            }
            case TimedRuleExecutionOption.PROPERTY_NAME: {
                setTimedRuleExecutionFilter(TimedRuleExecutionOption.resolve(StringUtils.isEmpty(value) ? "false" : value).getFilter());
            }
            case QueryListenerOption.PROPERTY_NAME: {
                String property = StringUtils.isEmpty(value) ? QueryListenerOption.STANDARD.getAsString() : value;
                setQueryListenerOption(QueryListenerOption.determineQueryListenerClassOption(property));
            }
            case BeliefSystemTypeOption.PROPERTY_NAME: {
                setBeliefSystemType(StringUtils.isEmpty(value) ? BeliefSystemType.SIMPLE : BeliefSystemType.resolveBeliefSystemType(value));
            }
        }
    }

    public final String getProperty(String name) {
        name = name.trim();
        if ( StringUtils.isEmpty( name ) ) {
            return null;
        }

        switch(name) {
            case DirectFiringOption.PROPERTY_NAME: {
                return Boolean.toString(isDirectFiring());
            } case ThreadSafeOption.PROPERTY_NAME: {
                return Boolean.toString(isThreadSafe());
            } case AccumulateNullPropagationOption.PROPERTY_NAME: {
                return Boolean.toString(isAccumulateNullPropagation());
            } case QueryListenerOption.PROPERTY_NAME: {
                return getQueryListenerOption().getAsString();
            } case BeliefSystemTypeOption.PROPERTY_NAME: {
                return getBeliefSystemType().getId();
            }
        }
        return null;
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RuleSessionConfiguration that = (RuleSessionConfiguration) o;

        return getBeliefSystemType() == that.getBeliefSystemType();
    }

    @Override
    public final int hashCode() {
        int result = getBeliefSystemType().hashCode();
        return result;
    }
}
