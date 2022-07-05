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

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.drools.base.rule.consequence.ConflictResolver;
import org.drools.base.util.MVELExecutor;
import org.drools.base.util.index.IndexConfiguration;
import org.drools.core.common.AgendaGroupFactory;
import org.drools.core.reteoo.RuntimeComponentFactory;
import org.drools.core.runtime.rule.impl.DefaultConsequenceExceptionHandler;
import org.drools.core.util.ConfFileUtils;
import org.drools.util.StringUtils;
import org.drools.wiring.api.classloader.ProjectClassLoader;
import org.kie.api.KieBaseConfiguration;
import org.kie.api.conf.BetaRangeIndexOption;
import org.kie.api.conf.Configuration;
import org.kie.api.conf.DeclarativeAgendaOption;
import org.kie.api.conf.EqualityBehaviorOption;
import org.kie.api.conf.EventProcessingOption;
import org.kie.api.conf.KieBaseMutabilityOption;
import org.kie.api.conf.KieBaseOption;
import org.kie.api.conf.MBeansOption;
import org.kie.api.conf.MultiValueKieBaseOption;
import org.kie.api.conf.OptionKey;
import org.kie.api.conf.RemoveIdentitiesOption;
import org.kie.api.conf.SequentialOption;
import org.kie.api.conf.SessionsPoolOption;
import org.kie.api.conf.SingleValueKieBaseOption;
import org.kie.api.runtime.rule.ConsequenceExceptionHandler;
import org.kie.internal.conf.AlphaRangeIndexThresholdOption;
import org.kie.internal.conf.AlphaThresholdOption;
import org.kie.internal.conf.CompositeKeyDepthOption;
import org.kie.internal.conf.ConsequenceExceptionHandlerOption;
import org.kie.internal.conf.ConstraintJittingThresholdOption;
import org.kie.internal.conf.IndexLeftBetaMemoryOption;
import org.kie.internal.conf.IndexPrecedenceOption;
import org.kie.internal.conf.IndexRightBetaMemoryOption;
import org.kie.internal.conf.MaxThreadsOption;
import org.kie.internal.conf.MultithreadEvaluationOption;
import org.kie.internal.conf.SequentialAgendaOption;
import org.kie.internal.conf.ShareAlphaNodesOption;
import org.kie.internal.conf.ShareBetaNodesOption;
import org.kie.internal.utils.ChainedProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RuleBaseConfiguration
 *
 * A class to store RuleBase related configuration. It must be used at rule base instantiation time
 * or not used at all.
 * This class will automatically load default values from system properties, so if you want to set
 * a default configuration value for all your new rule bases, you can simply set the property as
 * a System property.
 *
 * After RuleBase is created, it makes the configuration immutable and there is no way to make it
 * mutable again. This is to avoid inconsistent behavior inside rulebase.
 *
 * NOTE: This API is under review and may change in the future.
 */

/**
 * Available configuration options:
 * <pre>
 * drools.maintainTms = &lt;true|false&gt;
 * drools.sequential = &lt;true|false&gt;
 * drools.sequential.agenda = &lt;sequential|dynamic&gt;
 * drools.removeIdentities = &lt;true|false&gt;
 * drools.shareAlphaNodes  = &lt;true|false&gt;
 * drools.shareBetaNodes = &lt;true|false&gt;
 * drools.alphaNodeHashingThreshold = &lt;1...n&gt;
 * drools.alphaNodeRangeIndexThreshold = &lt;1...n&gt;
 * drools.betaNodeRangeIndexEnabled = &lt;true|false&gt;
 * drools.sessionPool = &lt;1...n&gt;
 * drools.compositeKeyDepth = &lt;1..3&gt;
 * drools.indexLeftBetaMemory = &lt;true/false&gt;
 * drools.indexRightBetaMemory = &lt;true/false&gt;
 * drools.equalityBehavior = &lt;identity|equality&gt;
 * drools.conflictResolver = &lt;qualified class name&gt;
 * drools.consequenceExceptionHandler = &lt;qualified class name&gt;
 * drools.ruleBaseUpdateHandler = &lt;qualified class name&gt;
 * drools.sessionClock = &lt;qualified class name&gt;
 * drools.mbeans = &lt;enabled|disabled&gt;
 * drools.classLoaderCacheEnabled = &lt;true|false&gt;
 * drools.declarativeAgendaEnabled =  &lt;true|false&gt;
 * drools.permgenThreshold = &lt;1...n&gt;
 * drools.jittingThreshold = &lt;1...n&gt;
 * </pre>
 */
public class KieBaseConfigurationImpl
    implements
    KieBaseConfiguration,
    Externalizable {
    private static final long serialVersionUID = 510l;

    protected static final transient Logger logger = LoggerFactory.getLogger(KieBaseConfigurationImpl.class);

    public static final Configuration<KieBaseConfigurationImpl> KEY = new Configuration<>("Base");

    private ChainedProperties chainedProperties;

    private boolean immutable;

    private boolean mutabilityEnabled;

    // this property activates MBean monitoring and management
    private boolean mbeansEnabled;

    private transient ClassLoader classLoader;

//    private static class DefaultRuleBaseConfigurationHolder {
//        private static final KieBaseConfigurationImpl defaultConf = new KieBaseConfigurationImpl();
//    }
//
//    public static KieBaseConfigurationImpl getDefaultInstance() {
//        return DefaultRuleBaseConfigurationHolder.defaultConf;
//    }

    public void writeExternal(ObjectOutput out) throws IOException {
        // avoid serializing user defined system properties
        chainedProperties.filterDroolsPropertiesForSerialization();
        out.writeObject(chainedProperties);
        out.writeBoolean(immutable);
        out.writeBoolean(mutabilityEnabled);
    }

    public void readExternal(ObjectInput in) throws IOException,
            ClassNotFoundException {
        chainedProperties = (ChainedProperties) in.readObject();
        immutable = in.readBoolean();
        mutabilityEnabled = in.readBoolean();
    }

    public void setProperty(String name,
                            String value) {
        name = name.trim();
        if (StringUtils.isEmpty(name)) {
            return;
        }

        switch(name) {
            case MBeansOption.PROPERTY_NAME: {
                setMBeansEnabled( MBeansOption.isEnabled(value));
            }
            case KieBaseMutabilityOption.PROPERTY_NAME: {
                setMutabilityEnabled( StringUtils.isEmpty( value ) ? true : KieBaseMutabilityOption.determineMutability(value) == KieBaseMutabilityOption.ALLOWED );
            }
        }
    }

    public String getProperty(String name) {
        name = name.trim();
        if ( StringUtils.isEmpty( name ) ) {
            return null;
        }

        switch(name) {
            case MBeansOption.PROPERTY_NAME: {
                return isMBeansEnabled() ? "enabled" : "disabled";
            }
            case KieBaseMutabilityOption.PROPERTY_NAME: {
                return isMutabilityEnabled() ? "ALLOWED" : "DISABLED";
            }
        }

        return null;
    }

    /**
     * A constructor that sets the classloader to be used as the parent classloader
     * of this rule base classloader, and the properties to be used
     * as base configuration options
     *
     * @param classLoader
     * @param properties
     */
    public KieBaseConfigurationImpl(ClassLoader classLoader,
                                    ChainedProperties chainedProperties) {
        init( classLoader,
              chainedProperties );
    }
    
    private void init(ClassLoader classLoader,
                      ChainedProperties chainedProperties) {
        setClassLoader(classLoader);
        init(chainedProperties);
    }
    
    private void init(ChainedProperties chainedProperties) {
        this.immutable = false;

        this.chainedProperties = chainedProperties;

        setMBeansEnabled( MBeansOption.isEnabled( this.chainedProperties.getProperty( MBeansOption.PROPERTY_NAME,
                                                                                      "disabled" ) ) );

        setMutabilityEnabled( KieBaseMutabilityOption.determineMutability(
                this.chainedProperties.getProperty( KieBaseMutabilityOption.PROPERTY_NAME, "ALLOWED" )) == KieBaseMutabilityOption.ALLOWED );
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
     * @return
     */
    public boolean isImmutable() {
        return this.immutable;
    }

    private void checkCanChange() {
        if ( this.immutable ) {
            throw new UnsupportedOperationException( "Can't set a property after configuration becomes immutable" );
        }
    }


    public ClassLoader getClassLoader() {
        return this.classLoader;
    }

    public void setClassLoader(ClassLoader classLoader) {
        this.classLoader = ProjectClassLoader.getClassLoader( classLoader, getClass() );
    }

    /**
     * Defines if the RuleBase should expose management and monitoring MBeans
     *
     * @param mbeansEnabled true for multi-thread or
     *                     false for single-thread. Default is false.
     */
    public void setMBeansEnabled(boolean mbeansEnabled) {
        checkCanChange();
        this.mbeansEnabled = mbeansEnabled;
    }

    /**
     * Returns true if the management and monitoring through MBeans is active 
     *
     * @return
     */
    public boolean isMBeansEnabled() {
        return this.mbeansEnabled;
    }

    public void setMutabilityEnabled( boolean mutabilityEnabled ) {
        this.mutabilityEnabled = mutabilityEnabled;
    }

    public boolean isMutabilityEnabled() {
        return mutabilityEnabled;
    }


    @SuppressWarnings("unchecked")
    public <T extends SingleValueKieBaseOption> T getOption(OptionKey<T> option) {
        switch(option.name()) {
            case MBeansOption.PROPERTY_NAME: {
                return (T) (this.isMBeansEnabled() ? MBeansOption.ENABLED : MBeansOption.DISABLED);
            }
            case KieBaseMutabilityOption.PROPERTY_NAME: {
                return (T) (this.isMutabilityEnabled() ? KieBaseMutabilityOption.ALLOWED : KieBaseMutabilityOption.DISABLED);
            }
        }
        return null;

    }

    public void setOption(KieBaseOption option) {
        switch(option.getPropertyName()) {
            case MBeansOption.PROPERTY_NAME: {
                setMBeansEnabled( ( (MBeansOption) option ).isEnabled());
            }
            case KieBaseMutabilityOption.PROPERTY_NAME: {
                setMutabilityEnabled(option == KieBaseMutabilityOption.ALLOWED);
            }
        }
    }

    @Override public <C extends MultiValueKieBaseOption> C getOption(OptionKey<C> optionKey, String subKey) {
        return null;
    }

    public ChainedProperties getChainedProperties() {
        return chainedProperties;
    }
}
