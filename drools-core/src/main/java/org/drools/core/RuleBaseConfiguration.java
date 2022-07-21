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
import java.util.HashMap;
import java.util.Map;

import org.drools.core.common.AgendaGroupFactory;
import org.drools.core.reteoo.RuntimeComponentFactory;
import org.drools.base.rule.consequence.ConflictResolver;
import org.drools.core.runtime.rule.impl.DefaultConsequenceExceptionHandler;
import org.drools.base.util.index.IndexConfiguration;
import org.drools.util.StringUtils;
import org.drools.wiring.api.classloader.ProjectClassLoader;
import org.kie.api.conf.CompositeConfiguration;
import org.kie.api.conf.KieBaseConfiguration;
import org.kie.api.conf.BetaRangeIndexOption;
import org.kie.api.conf.ConfigurationKey;
import org.kie.api.conf.DeclarativeAgendaOption;
import org.kie.api.conf.EqualityBehaviorOption;
import org.kie.api.conf.EventProcessingOption;
import org.kie.api.conf.KieBaseOption;
import org.kie.api.conf.MultiValueKieBaseOption;
import org.kie.api.conf.OptionKey;
import org.kie.api.conf.OptionsConfiguration;
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
public class RuleBaseConfiguration
    implements
    KieBaseConfiguration,
    IndexConfiguration,
    Externalizable {

    public static final ConfigurationKey<RuleBaseConfiguration> KEY = new ConfigurationKey<>("Rule");

    private static final long serialVersionUID = 510l;

    public static final boolean DEFAULT_PHREAK = true;
    public static final boolean DEFAULT_SESSION_CACHE = true;

    public static final String DEFAULT_SIGN_ON_SERIALIZATION = "false";

    protected static final transient Logger logger = LoggerFactory.getLogger(RuleBaseConfiguration.class);

    CompositeConfiguration<KieBaseOption, SingleValueKieBaseOption, MultiValueKieBaseOption> compConfig;

    private ChainedProperties chainedProperties;

    private boolean immutable;

    private boolean          sequential;
    private SequentialAgenda sequentialAgenda;

    private boolean         maintainTms;
    private boolean         removeIdentities;
    private boolean         shareAlphaNodes;
    private boolean         shareBetaNodes;
    private int             permGenThreshold;
    private int             jittingThreshold;
    private int             alphaNodeHashingThreshold;
    private int             alphaNodeRangeIndexThreshold;
    private boolean         betaNodeRangeIndexEnabled;
    private int             compositeKeyDepth;
    private boolean         indexLeftBetaMemory;
    private boolean         indexRightBetaMemory;
    private AssertBehaviour assertBehaviour;
    private String          consequenceExceptionHandler;
    private String          ruleBaseUpdateHandler;

    private boolean declarativeAgenda;

    private EventProcessingOption eventProcessingMode;

    private IndexPrecedenceOption indexPrecedenceOption;

    // if "true", rulebase builder will try to split
    // the rulebase into multiple partitions that can be evaluated
    // in parallel by using multiple internal threads
    private boolean multithread;
    private int     maxThreads;

    private ConflictResolver conflictResolver;

    private Map<String, ActivationListenerFactory> activationListeners;

    private transient ClassLoader classLoader;

    private int sessionPoolSize;

//    private static class DefaultRuleBaseConfigurationHolder {
//        private static final RuleBaseConfiguration defaultConf = new RuleBaseConfiguration();
//    }
//
//    public static RuleBaseConfiguration getDefaultInstance() {
//        return DefaultRuleBaseConfigurationHolder.defaultConf;
//    }

    public void writeExternal(ObjectOutput out) throws IOException {
        // avoid serializing user defined system properties
        out.writeBoolean(sequential);
        out.writeObject(sequentialAgenda);
        out.writeBoolean(maintainTms);
        out.writeBoolean(removeIdentities);
        out.writeBoolean(shareAlphaNodes);
        out.writeBoolean(shareBetaNodes);
        out.writeInt(permGenThreshold);
        out.writeInt(jittingThreshold);
        out.writeInt(alphaNodeHashingThreshold);
        out.writeInt(alphaNodeRangeIndexThreshold);
        out.writeBoolean(betaNodeRangeIndexEnabled);
        out.writeInt(compositeKeyDepth);
        out.writeBoolean(indexLeftBetaMemory);
        out.writeBoolean(indexRightBetaMemory);
        out.writeObject(indexPrecedenceOption);
        out.writeObject(assertBehaviour);
        out.writeObject(consequenceExceptionHandler);
        out.writeObject(ruleBaseUpdateHandler);
        out.writeObject(conflictResolver);
        out.writeBoolean(multithread);
        out.writeInt(maxThreads);
        out.writeObject(eventProcessingMode);
        out.writeBoolean(declarativeAgenda);
        out.writeInt(sessionPoolSize);
    }

    public void readExternal(ObjectInput in) throws IOException,
            ClassNotFoundException {
        sequential = in.readBoolean();
        sequentialAgenda = (SequentialAgenda) in.readObject();
        maintainTms = in.readBoolean();
        removeIdentities = in.readBoolean();
        shareAlphaNodes = in.readBoolean();
        shareBetaNodes = in.readBoolean();
        permGenThreshold = in.readInt();
        jittingThreshold = in.readInt();
        alphaNodeHashingThreshold = in.readInt();
        alphaNodeRangeIndexThreshold = in.readInt();
        betaNodeRangeIndexEnabled = in.readBoolean();
        compositeKeyDepth = in.readInt();
        indexLeftBetaMemory = in.readBoolean();
        indexRightBetaMemory = in.readBoolean();
        indexPrecedenceOption = (IndexPrecedenceOption) in.readObject();
        assertBehaviour = (AssertBehaviour) in.readObject();
        consequenceExceptionHandler = (String) in.readObject();
        ruleBaseUpdateHandler = (String) in.readObject();
        conflictResolver = (ConflictResolver) in.readObject();
        multithread = in.readBoolean();
        maxThreads = in.readInt();
        eventProcessingMode = (EventProcessingOption) in.readObject();
        declarativeAgenda = in.readBoolean();
        sessionPoolSize = in.readInt();
    }


    public void setProperty(String name,
                            String value) {
        name = name.trim();
        if (StringUtils.isEmpty(name)) {
            return;
        }

        switch(name) {
            case SequentialAgendaOption.PROPERTY_NAME: {
                setSequentialAgenda(SequentialAgenda.determineSequentialAgenda(StringUtils.isEmpty(value) ? "sequential" : value));
            } 
            case SequentialOption.PROPERTY_NAME: {
                setSequential(StringUtils.isEmpty(value) ? false : Boolean.valueOf(value));
            }
            case RemoveIdentitiesOption.PROPERTY_NAME: {
                setRemoveIdentities(StringUtils.isEmpty(value) ? false : Boolean.valueOf(value));
            }
            case ShareAlphaNodesOption.PROPERTY_NAME: {
                setShareAlphaNodes(StringUtils.isEmpty(value) ? false : Boolean.valueOf(value));
            }
            case ShareBetaNodesOption.PROPERTY_NAME: {
                setShareBetaNodes(StringUtils.isEmpty(value) ? false : Boolean.valueOf(value));
            }
            case ConstraintJittingThresholdOption.PROPERTY_NAME: {
                setJittingThreshold(StringUtils.isEmpty(value) ? ConstraintJittingThresholdOption.DEFAULT_VALUE : Integer.parseInt(value));
            }
            case AlphaThresholdOption.PROPERTY_NAME: {
                setAlphaNodeHashingThreshold(StringUtils.isEmpty(value) ? 3 : Integer.parseInt(value));
            }
            case AlphaRangeIndexThresholdOption.PROPERTY_NAME: {
                setAlphaNodeRangeIndexThreshold(StringUtils.isEmpty(value) ? AlphaRangeIndexThresholdOption.DEFAULT_VALUE : Integer.parseInt(value));
            }
            case BetaRangeIndexOption.PROPERTY_NAME: {
                setBetaNodeRangeIndexEnabled(StringUtils.isEmpty(value) ? false : Boolean.valueOf(value));
            }
            case SessionsPoolOption.PROPERTY_NAME: {
                setSessionPoolSize(StringUtils.isEmpty(value) ? -1 : Integer.parseInt(value));
            }
            case CompositeKeyDepthOption.PROPERTY_NAME: {
                setCompositeKeyDepth(StringUtils.isEmpty(value) ? 3 : Integer.parseInt(value));
            }
            case IndexLeftBetaMemoryOption.PROPERTY_NAME: {
                setIndexLeftBetaMemory(StringUtils.isEmpty(value) ? true : Boolean.valueOf(value));
            }
            case IndexRightBetaMemoryOption.PROPERTY_NAME: {
                setIndexRightBetaMemory(StringUtils.isEmpty(value) ? true : Boolean.valueOf(value));
            }
            case IndexPrecedenceOption.PROPERTY_NAME: {
                setIndexPrecedenceOption(StringUtils.isEmpty(value) ? IndexPrecedenceOption.EQUALITY_PRIORITY : IndexPrecedenceOption.determineIndexPrecedence(value));
            }
            case EqualityBehaviorOption.PROPERTY_NAME: {
                setAssertBehaviour(AssertBehaviour.determineAssertBehaviour(StringUtils.isEmpty(value) ? "identity" : value));
            }
            case ConsequenceExceptionHandlerOption.PROPERTY_NAME: {
                setConsequenceExceptionHandler(StringUtils.isEmpty(value) ? DefaultConsequenceExceptionHandler.class.getName() : value);
            }
            case "drools.ruleBaseUpdateHandler": {
                setRuleBaseUpdateHandler(StringUtils.isEmpty(value) ? "" : value);
            }
            case MultithreadEvaluationOption.PROPERTY_NAME: {
                setMultithreadEvaluation(StringUtils.isEmpty(value) ? false : Boolean.valueOf(value));
            }
            case MaxThreadsOption.PROPERTY_NAME: {
                setMaxThreads(StringUtils.isEmpty(value) ? 3 : Integer.parseInt(value));
            }
            case EventProcessingOption.PROPERTY_NAME: {
                setEventProcessingMode(EventProcessingOption.determineEventProcessingMode(StringUtils.isEmpty(value) ? "cloud" : value));
            }
        }
    }

    public String getProperty(String name) {
        name = name.trim();
        if ( StringUtils.isEmpty( name ) ) {
            return null;
        }
        switch (name) {
            case SequentialAgendaOption.PROPERTY_NAME: {
                return getSequentialAgenda().toExternalForm();
            }
            case SequentialOption.PROPERTY_NAME: {
                return Boolean.toString(isSequential());
            }
            case RemoveIdentitiesOption.PROPERTY_NAME: {
                return Boolean.toString(isRemoveIdentities());
            }
            case ShareAlphaNodesOption.PROPERTY_NAME: {
                return Boolean.toString(isShareAlphaNodes());
            }
            case ShareBetaNodesOption.PROPERTY_NAME: {
                return Boolean.toString(isShareBetaNodes());
            }
            case ConstraintJittingThresholdOption.PROPERTY_NAME: {
                return Integer.toString(getJittingThreshold());
            }
            case AlphaThresholdOption.PROPERTY_NAME: {
                return Integer.toString(getAlphaNodeHashingThreshold());
            }
            case AlphaRangeIndexThresholdOption.PROPERTY_NAME: {
                return Integer.toString(getAlphaNodeRangeIndexThreshold());
            }
            case BetaRangeIndexOption.PROPERTY_NAME: {
                return Boolean.toString(isBetaNodeRangeIndexEnabled());
            }
            case SessionsPoolOption.PROPERTY_NAME: {
                return Integer.toString(getSessionPoolSize());
            }
            case CompositeKeyDepthOption.PROPERTY_NAME: {
                return Integer.toString(getCompositeKeyDepth());
            }
            case IndexLeftBetaMemoryOption.PROPERTY_NAME: {
                return Boolean.toString(isIndexLeftBetaMemory());
            }
            case IndexRightBetaMemoryOption.PROPERTY_NAME: {
                return Boolean.toString(isIndexRightBetaMemory());
            }
            case IndexPrecedenceOption.PROPERTY_NAME: {
                return getIndexPrecedenceOption().getValue();
            }
            case EqualityBehaviorOption.PROPERTY_NAME: {
                return getAssertBehaviour().toExternalForm();
            }
            case ConsequenceExceptionHandlerOption.PROPERTY_NAME: {
                return getConsequenceExceptionHandler();
            }
            case "drools.ruleBaseUpdateHandler": {
                return getRuleBaseUpdateHandler();
            }
            case MultithreadEvaluationOption.PROPERTY_NAME: {
                return Boolean.toString(isMultithreadEvaluation());
            }
            case MaxThreadsOption.PROPERTY_NAME: {
                return Integer.toString(getMaxThreads());
            }
            case EventProcessingOption.PROPERTY_NAME: {
                return getEventProcessingMode().getMode();
            }
        }

        return null;
    }

    /**
     * A constructor that sets the classloader to be used as the parent classloader
     * of this rule base classloaders, and the properties to be used
     * as base configuration options
     *
     * @param classLoader
     * @param chainedProperties
     */
    public RuleBaseConfiguration(CompositeConfiguration<KieBaseOption, SingleValueKieBaseOption, MultiValueKieBaseOption> compConfig,
                                 ClassLoader classLoader,
                                 ChainedProperties chainedProperties) {
        this.compConfig = compConfig;
        setClassLoader( classLoader);
        init(chainedProperties);
    }
    
    private void init(ChainedProperties chainedProperties) {
        this.immutable = false;

        this.chainedProperties = chainedProperties;

        setRemoveIdentities(Boolean.parseBoolean(this.chainedProperties.getProperty("drools.removeIdentities", "false")));

        setShareAlphaNodes(Boolean.parseBoolean(this.chainedProperties.getProperty(ShareAlphaNodesOption.PROPERTY_NAME, "true")));

        setShareBetaNodes(Boolean.parseBoolean(this.chainedProperties.getProperty(ShareBetaNodesOption.PROPERTY_NAME, "true")));

        setJittingThreshold( Integer.parseInt( this.chainedProperties.getProperty( ConstraintJittingThresholdOption.PROPERTY_NAME, "" + ConstraintJittingThresholdOption.DEFAULT_VALUE)));

        setAlphaNodeHashingThreshold(Integer.parseInt(this.chainedProperties.getProperty(AlphaThresholdOption.PROPERTY_NAME, "3")));

        setAlphaNodeRangeIndexThreshold(Integer.parseInt(this.chainedProperties.getProperty(AlphaRangeIndexThresholdOption.PROPERTY_NAME, "" + AlphaRangeIndexThresholdOption.DEFAULT_VALUE)));

        setBetaNodeRangeIndexEnabled(Boolean.parseBoolean(this.chainedProperties.getProperty(BetaRangeIndexOption.PROPERTY_NAME, "false")));

        setSessionPoolSize(Integer.parseInt(this.chainedProperties.getProperty( SessionsPoolOption.PROPERTY_NAME, "-1")));

        setCompositeKeyDepth(Integer.parseInt(this.chainedProperties.getProperty(CompositeKeyDepthOption.PROPERTY_NAME, "3")));

        setIndexLeftBetaMemory(Boolean.parseBoolean(this.chainedProperties.getProperty(IndexLeftBetaMemoryOption.PROPERTY_NAME, "true")));

        setIndexRightBetaMemory(Boolean.parseBoolean(this.chainedProperties.getProperty(IndexRightBetaMemoryOption.PROPERTY_NAME, "true")));

        setIndexPrecedenceOption(IndexPrecedenceOption.determineIndexPrecedence(this.chainedProperties.getProperty(IndexPrecedenceOption.PROPERTY_NAME, "equality")));

        setAssertBehaviour(AssertBehaviour.determineAssertBehaviour(this.chainedProperties.getProperty(EqualityBehaviorOption.PROPERTY_NAME, "identity")));

        setConsequenceExceptionHandler(this.chainedProperties.getProperty(ConsequenceExceptionHandlerOption.PROPERTY_NAME, "org.drools.core.runtime.rule.impl.DefaultConsequenceExceptionHandler"));

        setRuleBaseUpdateHandler(this.chainedProperties.getProperty("drools.ruleBaseUpdateHandler", ""));

        setSequentialAgenda(SequentialAgenda.determineSequentialAgenda(this.chainedProperties.getProperty(SequentialAgendaOption.PROPERTY_NAME, "sequential")));

        setSequential(Boolean.parseBoolean(this.chainedProperties.getProperty(SequentialOption.PROPERTY_NAME, "false")));

        setMultithreadEvaluation(Boolean.parseBoolean(this.chainedProperties.getProperty(MultithreadEvaluationOption.PROPERTY_NAME,
                                                                                         "false")));

        setMaxThreads( Integer.parseInt( this.chainedProperties.getProperty( MaxThreadsOption.PROPERTY_NAME,
                                                                             "3" ) ) );

        setEventProcessingMode( EventProcessingOption.determineEventProcessingMode( this.chainedProperties.getProperty( EventProcessingOption.PROPERTY_NAME,
                                                                                                                        "cloud" ) ) );

        setDeclarativeAgendaEnabled( Boolean.parseBoolean(this.chainedProperties.getProperty(DeclarativeAgendaOption.PROPERTY_NAME,
                                                                                             "false")) );
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

    public void setSequential(boolean sequential) {
        this.sequential = sequential;
    }

    public boolean isSequential() {
        return this.sequential;
    }

    public boolean isMaintainTms() {
        return this.maintainTms;
    }

    public void setMaintainTms(final boolean maintainTms) {
        checkCanChange(); // throws an exception if a change isn't possible;
        this.maintainTms = maintainTms;
    }

    public boolean isRemoveIdentities() {
        return this.removeIdentities;
    }

    public void setRemoveIdentities(final boolean removeIdentities) {
        checkCanChange(); // throws an exception if a change isn't possible;
        this.removeIdentities = removeIdentities;
    }

    public boolean isShareAlphaNodes() {
        return this.shareAlphaNodes;
    }

    public void setShareAlphaNodes(final boolean shareAlphaNodes) {
        checkCanChange(); // throws an exception if a change isn't possible;
        this.shareAlphaNodes = shareAlphaNodes;
    }

    public boolean isShareBetaNodes() {
        return this.shareBetaNodes;
    }

    public void setShareBetaNodes(final boolean shareBetaNodes) {
        checkCanChange(); // throws an exception if a change isn't possible;
        this.shareBetaNodes = shareBetaNodes;
    }

    public int getJittingThreshold() {
        return jittingThreshold;
    }

    public void setJittingThreshold( int jittingThreshold ) {
        checkCanChange(); // throws an exception if a change isn't possible;
        this.jittingThreshold = jittingThreshold;
    }

    public int getAlphaNodeHashingThreshold() {
        return this.alphaNodeHashingThreshold;
    }

    public void setAlphaNodeHashingThreshold(final int alphaNodeHashingThreshold) {
        checkCanChange(); // throws an exception if a change isn't possible;
        this.alphaNodeHashingThreshold = alphaNodeHashingThreshold;
    }

    public int getAlphaNodeRangeIndexThreshold() {
        return this.alphaNodeRangeIndexThreshold;
    }

    public void setAlphaNodeRangeIndexThreshold(final int alphaNodeRangeIndexThreshold) {
        checkCanChange();
        this.alphaNodeRangeIndexThreshold = alphaNodeRangeIndexThreshold;
    }

    public boolean isBetaNodeRangeIndexEnabled() {
        return this.betaNodeRangeIndexEnabled;
    }

    public void setBetaNodeRangeIndexEnabled(final boolean betaNodeRangeIndexEnabled) {
        checkCanChange();
        this.betaNodeRangeIndexEnabled = betaNodeRangeIndexEnabled;
    }

    public int getSessionPoolSize() {
        return this.sessionPoolSize;
    }

    public void setSessionPoolSize(final int sessionPoolSize) {
        checkCanChange(); // throws an exception if a change isn't possible;
        this.sessionPoolSize = sessionPoolSize;
    }

    public AssertBehaviour getAssertBehaviour() {
        return this.assertBehaviour;
    }

    public void setAssertBehaviour(final AssertBehaviour assertBehaviour) {
        checkCanChange(); // throws an exception if a change isn't possible;
        this.assertBehaviour = assertBehaviour;
    }

    public EventProcessingOption getEventProcessingMode() {
        return this.eventProcessingMode;
    }

    public void setEventProcessingMode(final EventProcessingOption mode) {
        checkCanChange(); // throws an exception if a change isn't possible;
        this.eventProcessingMode = mode;
    }

    public int getCompositeKeyDepth() {
        return this.compositeKeyDepth;
    }

    public void setCompositeKeyDepth(final int compositeKeyDepth) {
        if ( !this.immutable ) {
            if ( compositeKeyDepth > 3 ) {
                throw new UnsupportedOperationException( "compositeKeyDepth cannot be greater than 3" );
            }
            this.compositeKeyDepth = compositeKeyDepth;
        } else {
            throw new UnsupportedOperationException( "Can't set a property after configuration becomes immutable" );
        }
    }

    public boolean isIndexLeftBetaMemory() {
        return this.indexLeftBetaMemory;
    }

    public void setIndexLeftBetaMemory(final boolean indexLeftBetaMemory) {
        checkCanChange(); // throws an exception if a change isn't possible;
        this.indexLeftBetaMemory = indexLeftBetaMemory;
    }

    public boolean isIndexRightBetaMemory() {
        return this.indexRightBetaMemory;
    }

    public void setIndexRightBetaMemory(final boolean indexRightBetaMemory) {
        checkCanChange(); // throws an exception if a change isn't possible;
        this.indexRightBetaMemory = indexRightBetaMemory;
    }

    public IndexPrecedenceOption getIndexPrecedenceOption() {
        return this.indexPrecedenceOption;
    }

    public void setIndexPrecedenceOption(final IndexPrecedenceOption precedence) {
        checkCanChange(); // throws an exception if a change isn't possible;
        this.indexPrecedenceOption = precedence;
    }

    public String getConsequenceExceptionHandler() {
        return consequenceExceptionHandler;
    }

    public void setConsequenceExceptionHandler(String consequenceExceptionHandler) {
        checkCanChange(); // throws an exception if a change isn't possible;
        this.consequenceExceptionHandler = consequenceExceptionHandler;
    }

    public String getRuleBaseUpdateHandler() {
        return ruleBaseUpdateHandler;
    }

    public void setRuleBaseUpdateHandler(String ruleBaseUpdateHandler) {
        checkCanChange(); // throws an exception if a change isn't possible;
        this.ruleBaseUpdateHandler = ruleBaseUpdateHandler;
    }

    public AgendaGroupFactory getAgendaGroupFactory() {
        return RuntimeComponentFactory.get().getAgendaGroupFactory();
    }

    public SequentialAgenda getSequentialAgenda() {
        return this.sequentialAgenda;
    }

    public void setSequentialAgenda(final SequentialAgenda sequentialAgenda) {
        checkCanChange(); // throws an exception if a change isn't possible;
        this.sequentialAgenda = sequentialAgenda;
    }

    /**
     * Defines if the RuleBase should be executed using a pool of
     * threads for evaluating the rules ("true"), or if the rulebase 
     * should work in classic single thread mode ("false").
     * 
     * @param enableMultithread true for multi-thread or 
     *                     false for single-thread. Default is false.
     */
    public void setMultithreadEvaluation(boolean enableMultithread) {
        checkCanChange();
        this.multithread = enableMultithread;
    }

    public void enforceSingleThreadEvaluation() {
        this.multithread = false;
    }

    /**
     * Returns true if the partitioning of the rulebase is enabled
     * and false otherwise. Default is false.
     * 
     * @return
     */
    public boolean isMultithreadEvaluation() {
        return this.multithread;
    }

    /**
     * If multi-thread evaluation is enabled, this parameter configures the 
     * maximum number of threads each session can use for concurrent Rete
     * propagation. 
     * 
     * @param maxThreads the maximum number of threads to use. If 0 or a 
     *                   negative number is set, the engine will use number
     *                   of threads equal to the number of partitions in the
     *                   rule base. Default number of threads is 0. 
     */
    public void setMaxThreads(final int maxThreads) {
        this.maxThreads = maxThreads;
    }

    /**
     * Returns the configured number of maximum threads to use for concurrent
     * propagation when multi-thread evaluation is enabled. Default is zero.
     * 
     * @return
     */
    public int getMaxThreads() {
        return this.maxThreads;
    }

    public boolean isDeclarativeAgenda() {
        return this.declarativeAgenda;
    }
    
    /**
     * Enable declarative agenda
     * @param enabled
     */
    public void setDeclarativeAgendaEnabled(boolean enabled) {
        checkCanChange(); // throws an exception if a change isn't possible;
        this.declarativeAgenda = enabled;
    }    

    public List<Map<String, Object>> getWorkDefinitions() {
        if ( this.workDefinitions == null ) {
            initWorkDefinitions();
        }
        return this.workDefinitions;

    }

    private void initWorkDefinitions() {
        this.workDefinitions = new ArrayList<>();

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
                                                                                  RuleBaseConfiguration.class ) );
        try {
            this.workDefinitions.addAll(
                (List<Map<String, Object>>) MVELExecutor.get().eval( content, new HashMap() ) );
        } catch ( Throwable t ) {
            logger.error("Error occurred while loading work definitions " + location
                    + "\nContinuing without reading these work definitions", t);
            throw new RuntimeException( "Could not parse work definitions " + location + ": " + t.getMessage() );
        }
    }

    public boolean isAdvancedProcessRuleIntegration() {
        return advancedProcessRuleIntegration;
    }

    public void setAdvancedProcessRuleIntegration(boolean advancedProcessRuleIntegration) {
        this.advancedProcessRuleIntegration = advancedProcessRuleIntegration;
    }
    
    public void addActivationListener(String name, ActivationListenerFactory factory) {
        if ( this.activationListeners == null ) {
            this.activationListeners = new HashMap<>();
        }
        this.activationListeners.put( name, factory );
    }
    
    public ActivationListenerFactory getActivationListenerFactory(String name) {
        ActivationListenerFactory factory = null;
        if ( this.activationListeners != null ) {
            factory = this.activationListeners.get( name );
        }
        
        if ( factory != null ) {
            return factory;
        } else {
            if ( "query".equals( name )) {
                return QueryActivationListenerFactory.INSTANCE;
            } else  if ( "agenda".equals( name ) || "direct".equals( name ) ) {
                return RuleActivationListenerFactory.INSTANCE;
            } 
        } 
        
        throw new IllegalArgumentException( "ActivationListenerFactory not found for '" + name + "'" );
    }

    public ClassLoader getClassLoader() {
        return this.classLoader;
    }

    public void setClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    public static class AssertBehaviour
            implements
            Externalizable {
        private static final long serialVersionUID = 510l;

        public static final AssertBehaviour IDENTITY = new AssertBehaviour(0);
        public static final AssertBehaviour EQUALITY = new AssertBehaviour(1);

        private int value;

        public void readExternal(ObjectInput in) throws IOException,
                ClassNotFoundException {
            value = in.readInt();
        }

        public void writeExternal(ObjectOutput out) throws IOException {
            out.writeInt(value);
        }

        public AssertBehaviour() {

        }

        private AssertBehaviour(final int value) {
            this.value = value;
        }

        public boolean equals(Object obj) {
            if (obj == this) return true;
            else if (obj instanceof AssertBehaviour) {
                AssertBehaviour that = (AssertBehaviour) obj;

                return value == that.value;
            }
            return false;
        }

        public static AssertBehaviour determineAssertBehaviour(final String value) {
            if ("IDENTITY".equalsIgnoreCase(value)) {
                return IDENTITY;
            } else if ("EQUALITY".equalsIgnoreCase(value)) {
                return EQUALITY;
            } else {
                throw new IllegalArgumentException("Illegal enum value '" + value + "' for AssertBehaviour");
            }
        }

        private Object readResolve() throws java.io.ObjectStreamException {
            switch (this.value) {
                case 0:
                    return IDENTITY;
                case 1:
                    return EQUALITY;
                default:
                    throw new IllegalArgumentException("Illegal enum value '" + this.value + "' for AssertBehaviour");
            }
        }

        public String toExternalForm() {
            return (this.value == 0) ? "identity" : "equality";
        }

        public String toString() {
            return "AssertBehaviour : " + ((this.value == 0) ? "identity" : "equality");
        }
    }

    public static class SequentialAgenda
            implements
            Externalizable {
        private static final long serialVersionUID = 510l;

        public static final SequentialAgenda SEQUENTIAL = new SequentialAgenda(0);
        public static final SequentialAgenda DYNAMIC    = new SequentialAgenda(1);

        private int value;

        public void readExternal(ObjectInput in) throws IOException,
                ClassNotFoundException {
            value = in.readInt();
        }

        public void writeExternal(ObjectOutput out) throws IOException {
            out.writeInt(value);
        }

        public SequentialAgenda() {

        }

        private SequentialAgenda(final int value) {
            this.value = value;
        }

        public static SequentialAgenda determineSequentialAgenda(final String value) {
            if ("sequential".equalsIgnoreCase(value)) {
                return SEQUENTIAL;
            } else if ("dynamic".equalsIgnoreCase(value)) {
                return DYNAMIC;
            } else {
                throw new IllegalArgumentException("Illegal enum value '" + value + "' for SequentialAgenda");
            }
        }

        private Object readResolve() throws java.io.ObjectStreamException {
            switch (this.value) {
                case 0:
                    return SEQUENTIAL;
                case 1:
                    return DYNAMIC;
                default:
                    throw new IllegalArgumentException("Illegal enum value '" + this.value + "' for SequentialAgenda");
            }
        }

        public String toExternalForm() {
            return (this.value == 0) ? "sequential" : "dynamic";
        }

        public String toString() {
            return "SequentialAgenda : " + ((this.value == 0) ? "sequential" : "dynamic");
        }
    }

    @SuppressWarnings("unchecked")
    public <T extends SingleValueKieBaseOption> T getOption(OptionKey<T> option) {
        switch(option.name()) {
            case SequentialOption.PROPERTY_NAME: {
                return (T) (this.sequential ? SequentialOption.YES : SequentialOption.NO);
            }
            case RemoveIdentitiesOption.PROPERTY_NAME: {
                return (T) (this.removeIdentities ? RemoveIdentitiesOption.YES : RemoveIdentitiesOption.NO);
            }
            case ShareAlphaNodesOption.PROPERTY_NAME: {
                return (T) (this.shareAlphaNodes ? ShareAlphaNodesOption.YES : ShareAlphaNodesOption.NO);
            }
            case ShareBetaNodesOption.PROPERTY_NAME: {
                return (T) (this.shareBetaNodes ? ShareBetaNodesOption.YES : ShareBetaNodesOption.NO);
            }
            case IndexRightBetaMemoryOption.PROPERTY_NAME: {
                return (T) (this.indexRightBetaMemory ? IndexRightBetaMemoryOption.YES : IndexRightBetaMemoryOption.NO);
            }
            case IndexLeftBetaMemoryOption.PROPERTY_NAME: {
                return (T) (this.indexLeftBetaMemory ? IndexLeftBetaMemoryOption.YES : IndexLeftBetaMemoryOption.NO);
            }
            case IndexPrecedenceOption.PROPERTY_NAME: {
                return (T) getIndexPrecedenceOption();
            }
            case EqualityBehaviorOption.PROPERTY_NAME: {
                return (T) ((this.assertBehaviour == AssertBehaviour.IDENTITY) ? EqualityBehaviorOption.IDENTITY : EqualityBehaviorOption.EQUALITY);
            }
            case SequentialAgendaOption.PROPERTY_NAME: {
                return (T) ((this.sequentialAgenda == SequentialAgenda.SEQUENTIAL) ? SequentialAgendaOption.SEQUENTIAL : SequentialAgendaOption.DYNAMIC);
            }
            case ConstraintJittingThresholdOption.PROPERTY_NAME: {
                return (T) ConstraintJittingThresholdOption.get(jittingThreshold);
            }
            case AlphaThresholdOption.PROPERTY_NAME: {
                return (T) AlphaThresholdOption.get(alphaNodeHashingThreshold);
            }
            case AlphaRangeIndexThresholdOption.PROPERTY_NAME: {
                return (T) AlphaRangeIndexThresholdOption.get(alphaNodeRangeIndexThreshold);
            }
            case BetaRangeIndexOption.PROPERTY_NAME: {
                return (T) (this.betaNodeRangeIndexEnabled ? BetaRangeIndexOption.ENABLED : BetaRangeIndexOption.DISABLED);
            }
            case SessionsPoolOption.PROPERTY_NAME: {
                return (T) SessionsPoolOption.get(sessionPoolSize);
            }
            case CompositeKeyDepthOption.PROPERTY_NAME: {
                return (T) CompositeKeyDepthOption.get(compositeKeyDepth);
            }
            case ConsequenceExceptionHandlerOption.PROPERTY_NAME: {
                Class<? extends ConsequenceExceptionHandler> handler;
                try {
                    handler = (Class<? extends ConsequenceExceptionHandler>) Class.forName(consequenceExceptionHandler);
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException("Unable to resolve ConsequenceExceptionHandler class: " + consequenceExceptionHandler,
                                               e);
                }
                return (T) ConsequenceExceptionHandlerOption.get(handler);
            }
            case EventProcessingOption.PROPERTY_NAME: {
                return (T) getEventProcessingMode();
            }
            case MaxThreadsOption.PROPERTY_NAME: {
                return (T) MaxThreadsOption.get(getMaxThreads());
            }
            case MultithreadEvaluationOption.PROPERTY_NAME: {
                return (T) (this.multithread ? MultithreadEvaluationOption.YES : MultithreadEvaluationOption.NO);
            }
            case DeclarativeAgendaOption.PROPERTY_NAME: {
                return (T) (this.isDeclarativeAgenda() ? DeclarativeAgendaOption.ENABLED : DeclarativeAgendaOption.DISABLED);
            }
        }
        return null;

    }

    public void setOption(KieBaseOption option) {
        switch (option.propertyName()) {
            case SequentialOption.PROPERTY_NAME : {
                setSequential(((SequentialOption) option).isSequential());
            }
            case RemoveIdentitiesOption.PROPERTY_NAME: {
                setRemoveIdentities(((RemoveIdentitiesOption) option).isRemoveIdentities());
            }
            case ShareAlphaNodesOption.PROPERTY_NAME: {
                setShareAlphaNodes(((ShareAlphaNodesOption) option).isShareAlphaNodes());
            }
            case ShareBetaNodesOption.PROPERTY_NAME: {
                setShareBetaNodes(((ShareBetaNodesOption) option).isShareBetaNodes());
            }
            case IndexLeftBetaMemoryOption.PROPERTY_NAME: {
                setIndexLeftBetaMemory(((IndexLeftBetaMemoryOption) option).isIndexLeftBetaMemory());
            }
            case IndexRightBetaMemoryOption.PROPERTY_NAME: {
                setIndexRightBetaMemory(((IndexRightBetaMemoryOption) option).isIndexRightBetaMemory());
            }
            case IndexPrecedenceOption.PROPERTY_NAME: {
                setIndexPrecedenceOption((IndexPrecedenceOption) option);
            }
            case EqualityBehaviorOption.PROPERTY_NAME: {
                setAssertBehaviour((option == EqualityBehaviorOption.IDENTITY) ? AssertBehaviour.IDENTITY : AssertBehaviour.EQUALITY);
            }
            case SequentialAgendaOption.PROPERTY_NAME: {
                setSequentialAgenda((option == SequentialAgendaOption.SEQUENTIAL) ? SequentialAgenda.SEQUENTIAL : SequentialAgenda.DYNAMIC);
            }
            case ConstraintJittingThresholdOption.PROPERTY_NAME: {
                setJittingThreshold( ( (ConstraintJittingThresholdOption) option ).getThreshold());
            }
            case AlphaThresholdOption.PROPERTY_NAME: {
                setAlphaNodeHashingThreshold( ( (AlphaThresholdOption) option ).getThreshold());
            }
            case AlphaRangeIndexThresholdOption.PROPERTY_NAME: {
                setAlphaNodeRangeIndexThreshold( ( (AlphaRangeIndexThresholdOption) option ).getThreshold());
            }
            case BetaRangeIndexOption.PROPERTY_NAME: {
                setBetaNodeRangeIndexEnabled( ( (BetaRangeIndexOption) option ).isBetaRangeIndexEnabled());
            }
            case SessionsPoolOption.PROPERTY_NAME: {
                setSessionPoolSize( ( ( SessionsPoolOption ) option ).getSize());
            }
            case CompositeKeyDepthOption.PROPERTY_NAME: {
                setCompositeKeyDepth( ( (CompositeKeyDepthOption) option ).getDepth());
            }
            case ConsequenceExceptionHandlerOption.PROPERTY_NAME: {
                setConsequenceExceptionHandler( ( (ConsequenceExceptionHandlerOption) option ).getHandler().getName());
            }
            case EventProcessingOption.PROPERTY_NAME: {
                setEventProcessingMode( (EventProcessingOption) option);
            }
            case MaxThreadsOption.PROPERTY_NAME: {
                setMaxThreads( ( (MaxThreadsOption) option ).getMaxThreads());
            }
            case MultithreadEvaluationOption.PROPERTY_NAME: {
                setMultithreadEvaluation( ( (MultithreadEvaluationOption) option ).isMultithreadEvaluation());
            }
            case DeclarativeAgendaOption.PROPERTY_NAME: {
                setDeclarativeAgendaEnabled(((DeclarativeAgendaOption) option).isDeclarativeAgendaEnabled());
            }
        }
    }

    @Override public <X extends OptionsConfiguration<KieBaseOption, SingleValueKieBaseOption, MultiValueKieBaseOption>> X as(ConfigurationKey<X> key) {
        return compConfig.as(key);
    }

    public ChainedProperties getChainedProperties() {
        return chainedProperties;
    }
}
