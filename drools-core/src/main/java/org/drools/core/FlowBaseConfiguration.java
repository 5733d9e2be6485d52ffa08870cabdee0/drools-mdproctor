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
import org.kie.api.conf.KieBaseOption;
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
 * FlowBaseConfiguration
 *
 * A class to store FlowBase related configuration. It must be used at flow base instantiation time
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
 * <pre>
 * </pre>
 */
public class FlowBaseConfiguration
    implements
    KieBaseConfiguration,
    Externalizable {

    public static final Configuration<FlowBaseConfiguration> KEY = new Configuration<>("Rule");

    private static final long serialVersionUID = 510l;

    protected static final transient Logger logger = LoggerFactory.getLogger(FlowBaseConfiguration.class);

    private ChainedProperties chainedProperties;

    private boolean immutable;
    private List<Map<String, Object>> workDefinitions;

    private transient ClassLoader classLoader;


    public void writeExternal(ObjectOutput out) throws IOException {
        // avoid serializing user defined system properties
    }

    public void readExternal(ObjectInput in) throws IOException,
            ClassNotFoundException {
    }


    /**
     * A constructor that sets the classloader to be used as the parent classloader
     * of this rule base classloaders, and the properties to be used
     * as base configuration options
     *
     * @param classLoader
     * @param chainedProperties
     */
    public FlowBaseConfiguration(ClassLoader classLoader,
                                 ChainedProperties chainedProperties) {
        init( classLoader,
              chainedProperties );
    }
    
    private void init(ClassLoader classLoader, ChainedProperties chainedProperties) {
        setClassLoader( classLoader);
        init(chainedProperties);
    }
    
    private void init(ChainedProperties chainedProperties) {
        this.immutable = false;
        this.chainedProperties = chainedProperties;
        initWorkDefinitions();
    }

    public ClassLoader getClassLoader() {
        return this.classLoader;
    }

    public void setClassLoader(ClassLoader classLoader) {
        this.classLoader = ProjectClassLoader.getClassLoader( classLoader, getClass() );
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

    public List<Map<String, Object>> getWorkDefinitions() {
        if ( this.workDefinitions == null ) {
            initWorkDefinitions();
        }
        return this.workDefinitions;

    }

    private void initWorkDefinitions() {
        this.workDefinitions = new ArrayList<Map<String, Object>>();

        // split on each space
        String locations[] = this.chainedProperties.getProperty( "drools.workDefinitions",
                                                                 "" ).split( "\\s" );

        // load each SemanticModule
        for ( String factoryLocation : locations ) {
            // trim leading/trailing spaces and quotes
            factoryLocation = factoryLocation.trim();
            if ( factoryLocation.startsWith( "\"" ) ) {
                factoryLocation = factoryLocation.substring( 1 );
            }
            if ( factoryLocation.endsWith( "\"" ) ) {
                factoryLocation = factoryLocation.substring( 0,
                                                             factoryLocation.length() - 1 );
            }
            if ( !factoryLocation.equals( "" ) ) {
                loadWorkItems( factoryLocation );
            }
        }
    }

    private void loadWorkItems(String location) {
        String content = ConfFileUtils.URLContentsToString( ConfFileUtils.getURL( location,
                                                                                  null,
                                                                                  FlowBaseConfiguration.class));
        try {
            this.workDefinitions.addAll(
                (List<Map<String, Object>>) MVELExecutor.get().eval( content, new HashMap() ) );
        } catch ( Throwable t ) {
            logger.error("Error occurred while loading work definitions " + location
                    + "\nContinuing without reading these work definitions", t);
            throw new RuntimeException( "Could not parse work definitions " + location + ": " + t.getMessage() );
        }
    }

    @SuppressWarnings("unchecked")
    public <T extends SingleValueKieBaseOption> T getOption(OptionKey<T> option) {
        return null;
    }

    public void setOption(KieBaseOption option) {
    }

    @Override public <C extends MultiValueKieBaseOption> C getOption(OptionKey<C> optionKey, String subKey) {
//        switch (optionKey.name()) {
//            case WorkDefinitionFunctionOption.PROPERTY_NAME: {
//                return (C) new WorkDefinitionFunctionOption( subKey, work);
//            }
//        }
        return null;
    }

    public ChainedProperties getChainedProperties() {
        return chainedProperties;
    }
}
