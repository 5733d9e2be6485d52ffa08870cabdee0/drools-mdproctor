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

package org.drools.core;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import org.drools.wiring.api.classloader.ProjectClassLoader;
import org.kie.api.conf.CompositeConfiguration;
import org.kie.api.conf.ConfigurationKey;
import org.kie.api.conf.OptionsConfiguration;
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
import org.kie.internal.utils.ChainedProperties;

/**
 * SessionConfiguration
 *
 * A class to store Session related configuration. It must be used at session instantiation time
 * or not used at all.
 * This class will automatically load default values from system properties, so if you want to set
 * a default configuration value for all your new sessions, you can simply set the property as
 * a System property.
 *
 * After the Session is created, it makes the configuration immutable and there is no way to make it
 * mutable again. This is to avoid inconsistent behavior inside session.
 *
 * NOTE: This API is under review and may change in the future.
 * 
 * 
 * drools.keepReference = <true|false>
 * drools.clockType = <pseudo|realtime|heartbeat|implicit>
 */
public class RuleSessionConfigurationImpl extends RuleSessionConfiguration {

    private static final long              serialVersionUID = 510l;

    private CompositeConfiguration<KieSessionOption, SingleValueKieSessionOption, MultiValueKieSessionOption> compConfig;

    private ChainedProperties              chainedProperties;

    private volatile boolean               immutable;

    private boolean                        directFiring;

    private boolean                        threadSafe;

    private boolean                        accumulateNullPropagation;

    private ForceEagerActivationFilter     forceEagerActivationFilter;
    private TimedRuleExecutionFilter       timedRuleExecutionFilter;

    private BeliefSystemType               beliefSystemType;

    private QueryListenerOption            queryListener;
    private transient ClassLoader          classLoader;

    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeObject( chainedProperties );
        out.writeBoolean(immutable);
        out.writeObject( queryListener );
    }

    @SuppressWarnings("unchecked")
    public void readExternal(ObjectInput in) throws IOException,
                                            ClassNotFoundException {
        chainedProperties = (ChainedProperties) in.readObject();
        immutable = in.readBoolean();
        queryListener = (QueryListenerOption) in.readObject();
    }

    public RuleSessionConfigurationImpl(CompositeConfiguration<KieSessionOption, SingleValueKieSessionOption, MultiValueKieSessionOption> compConfig,
                                        ClassLoader classLoader,
                                        ChainedProperties chainedProperties) {
        this.compConfig = compConfig;
        setClassLoader( classLoader);
        init(chainedProperties );
    }

    @Override public ClassLoader getClassLoader() {
        return this.classLoader;
    }

    private void init(ChainedProperties chainedProperties) {
        this.immutable = false;

        this.chainedProperties = chainedProperties;

        setDirectFiring(Boolean.parseBoolean(getPropertyValue(DirectFiringOption.PROPERTY_NAME, "false")));

        setThreadSafe(Boolean.parseBoolean(getPropertyValue(ThreadSafeOption.PROPERTY_NAME, "true")));

        setAccumulateNullPropagation(Boolean.parseBoolean(getPropertyValue(AccumulateNullPropagationOption.PROPERTY_NAME, "false")));

        setForceEagerActivationFilter(ForceEagerActivationOption.resolve( getPropertyValue( ForceEagerActivationOption.PROPERTY_NAME, "false" ) ).getFilter());

        setTimedRuleExecutionFilter(TimedRuleExecutionOption.resolve( getPropertyValue( TimedRuleExecutionOption.PROPERTY_NAME, "false" ) ).getFilter());

        setBeliefSystemType( BeliefSystemType.resolveBeliefSystemType( getPropertyValue( BeliefSystemTypeOption.PROPERTY_NAME, BeliefSystemType.SIMPLE.getId() ) ) );

        setQueryListenerOption( QueryListenerOption.determineQueryListenerClassOption( getPropertyValue( QueryListenerOption.PROPERTY_NAME, QueryListenerOption.STANDARD.getAsString() ) ) );
    }

    public void setClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    /**
     * Makes the configuration object immutable. Once it becomes immutable,
     * there is no way to make it mutable again.
     * This is done to keep consistency.
     */
    public void makeImmutable() {
        this.immutable = true;
    }

    /**
     * Returns true if this configuration object is immutable or false otherwise.
     */
    public boolean isImmutable() {
        return this.immutable;
    }

    private void checkCanChange() {
        if ( this.immutable ) {
            throw new UnsupportedOperationException( "Can't set a property after configuration becomes immutable" );
        }
    }

    public void setDirectFiring(boolean directFiring) {
        checkCanChange(); // throws an exception if a change isn't possible;
        this.directFiring = directFiring;
    }

    public boolean isDirectFiring() {
        return this.directFiring;
    }

    public void setThreadSafe(boolean threadSafe) {
        checkCanChange(); // throws an exception if a change isn't possible;
        this.threadSafe = threadSafe;
    }

    public boolean isThreadSafe() {
        return this.threadSafe;
    }

    public void setAccumulateNullPropagation(boolean accumulateNullPropagation) {
        checkCanChange(); // throws an exception if a change isn't possible;
        this.accumulateNullPropagation = accumulateNullPropagation;
    }

    public boolean isAccumulateNullPropagation() {
        return this.accumulateNullPropagation;
    }

    public void setForceEagerActivationFilter(ForceEagerActivationFilter forceEagerActivationFilter) {
        checkCanChange(); // throws an exception if a change isn't possible;
        this.forceEagerActivationFilter = forceEagerActivationFilter;
    }

    public ForceEagerActivationFilter getForceEagerActivationFilter() {
        return this.forceEagerActivationFilter;
    }

    public void setTimedRuleExecutionFilter(TimedRuleExecutionFilter timedRuleExecutionFilter) {
        checkCanChange(); // throws an exception if a change isn't possible;
        this.timedRuleExecutionFilter = timedRuleExecutionFilter;
    }

    public TimedRuleExecutionFilter getTimedRuleExecutionFilter() {
        return this.timedRuleExecutionFilter;
    }

    public BeliefSystemType getBeliefSystemType() {
        return this.beliefSystemType;
    }
    
    public void setBeliefSystemType(BeliefSystemType beliefSystemType) {
        checkCanChange(); // throws an exception if a change isn't possible;
        this.beliefSystemType = beliefSystemType;
    }

    private void setQueryListenerClass(QueryListenerOption option) {
        checkCanChange();
        this.queryListener = option;
    }

    public String getPropertyValue( String name, String defaultValue ) {
        return this.chainedProperties.getProperty( name, defaultValue );
    }

    public QueryListenerOption getQueryListenerOption() {
        return this.queryListener;
    }

    public void setQueryListenerOption( QueryListenerOption queryListener ) {
        checkCanChange();
        this.queryListener = queryListener;
    }

    @Override public <X extends OptionsConfiguration<KieSessionOption, SingleValueKieSessionOption, MultiValueKieSessionOption>> X as(ConfigurationKey<X> key) {
        return compConfig.as(key);
    }
}
