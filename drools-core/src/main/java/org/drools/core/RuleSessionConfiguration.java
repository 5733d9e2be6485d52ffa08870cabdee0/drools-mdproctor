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
import java.util.Set;

import org.drools.util.StringUtils;
import org.kie.api.conf.ConfigurationKey;
import org.kie.api.conf.OptionKey;
import org.kie.api.runtime.conf.KieSessionConfiguration;
import org.kie.api.runtime.conf.AccumulateNullPropagationOption;
import org.kie.api.runtime.conf.BeliefSystemTypeOption;
import org.kie.api.runtime.conf.DirectFiringOption;
import org.kie.api.runtime.conf.KieSessionOption;
import org.kie.api.runtime.conf.MultiValueKieSessionOption;
import org.kie.api.runtime.conf.QueryListenerOption;
import org.kie.api.runtime.conf.SingleValueKieSessionOption;
import org.kie.api.runtime.conf.ThreadSafeOption;
import org.kie.api.runtime.conf.TimedRuleExecutionFilter;
import org.kie.api.runtime.conf.TimedRuleExecutionOption;
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
    public static final ConfigurationKey<RuleSessionConfiguration> KEY = new ConfigurationKey<>("Rule");

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

    public abstract void setTimedRuleExecutionFilter(TimedRuleExecutionFilter timedRuleExecutionFilter);
    public abstract TimedRuleExecutionFilter getTimedRuleExecutionFilter();

    public abstract BeliefSystemType getBeliefSystemType();
    public abstract void setBeliefSystemType(BeliefSystemType beliefSystemType);

    public abstract QueryListenerOption getQueryListenerOption();
    public abstract void setQueryListenerOption( QueryListenerOption queryListener );

    public final void setProperty(String name,
                                  String value) {
        name = name.trim();
        if ( StringUtils.isEmpty( name ) ) {
            return;
        }
        switch(name) {
            case DirectFiringOption.PROPERTY_NAME: {
                setDirectFiring(!StringUtils.isEmpty(value) && Boolean.parseBoolean(value));
                break;
            }
            case ThreadSafeOption.PROPERTY_NAME: {
                setThreadSafe(StringUtils.isEmpty(value) || Boolean.parseBoolean(value));
                break;
            }
            case AccumulateNullPropagationOption.PROPERTY_NAME: {
                setAccumulateNullPropagation(!StringUtils.isEmpty(value) && Boolean.parseBoolean(value));
                break;
            }
            case ForceEagerActivationOption.PROPERTY_NAME: {
                setForceEagerActivationFilter(ForceEagerActivationOption.resolve(StringUtils.isEmpty(value) ? "false" : value).getFilter());
                break;
            }
            case TimedRuleExecutionOption.PROPERTY_NAME: {
                setTimedRuleExecutionFilter(TimedRuleExecutionOption.resolve(StringUtils.isEmpty(value) ? "false" : value).getFilter());
                break;
            }
            case QueryListenerOption.PROPERTY_NAME: {
                String property = StringUtils.isEmpty(value) ? QueryListenerOption.STANDARD.getAsString() : value;
                setQueryListenerOption(QueryListenerOption.determineQueryListenerClassOption(property));
                break;
            }
            case BeliefSystemTypeOption.PROPERTY_NAME: {
                setBeliefSystemType(StringUtils.isEmpty(value) ? BeliefSystemType.SIMPLE : BeliefSystemType.resolveBeliefSystemType(value));
                break;
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
