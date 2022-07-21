/*
 * Copyright 2005 Red Hat, Inc. and/or its affiliates.
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

package org.drools.compiler.builder.impl;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import org.drools.base.definitions.InternalKnowledgePackage;
import org.drools.compiler.compiler.Dialect;
import org.drools.compiler.compiler.DialectCompiletimeRegistry;
import org.drools.compiler.compiler.DialectConfiguration;
import org.drools.compiler.compiler.PackageRegistry;
import org.drools.compiler.kie.builder.impl.InternalKieModule.CompilationCache;
import org.drools.compiler.rule.builder.ConstraintBuilder;
import org.drools.compiler.rule.builder.EvaluatorDefinition;
import org.drools.compiler.rule.builder.util.AccumulateUtil;
import org.drools.drl.parser.DrlParser;
import org.drools.util.StringUtils;
import org.drools.wiring.api.classloader.ProjectClassLoader;
import org.kie.api.conf.CompositeConfiguration;
import org.kie.api.conf.ConfigurationKey;
import org.kie.api.conf.OptionKey;
import org.kie.api.conf.OptionsConfiguration;
import org.kie.api.runtime.rule.AccumulateFunction;
import org.kie.internal.builder.KnowledgeBuilderConfiguration;
import org.kie.internal.builder.ResultSeverity;
import org.kie.internal.builder.conf.AccumulateFunctionOption;
import org.kie.internal.builder.conf.AlphaNetworkCompilerOption;
import org.kie.internal.builder.conf.DefaultDialectOption;
import org.kie.internal.builder.conf.DefaultPackageNameOption;
import org.kie.internal.builder.conf.DumpDirOption;
import org.kie.internal.builder.conf.EvaluatorOption;
import org.kie.internal.builder.conf.ExternaliseCanonicalModelLambdaOption;
import org.kie.internal.builder.conf.GroupDRLsInKieBasesByFolderOption;
import org.kie.internal.builder.conf.KBuilderSeverityOption;
import org.kie.internal.builder.conf.KnowledgeBuilderOption;
import org.kie.internal.builder.conf.LanguageLevelOption;
import org.kie.internal.builder.conf.MultiValueKieBuilderOption;
import org.kie.internal.builder.conf.ParallelLambdaExternalizationOption;
import org.kie.internal.builder.conf.ParallelRulesBuildThresholdOption;
import org.kie.internal.builder.conf.ProcessStringEscapesOption;
import org.kie.internal.builder.conf.PropertySpecificOption;
import org.kie.internal.builder.conf.SingleValueKieBuilderOption;
import org.kie.internal.builder.conf.TrimCellsInDTableOption;
import org.kie.internal.utils.ChainedProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class configures the package compiler.
 * Dialects and their DialectConfigurations  are handled by the DialectRegistry
 * Normally you will not need to look at this class, unless you want to override the defaults.
 *
 * This class is not thread safe and it also contains state. Once it is created and used
 * in one or more PackageBuilders it should be considered immutable. Do not modify its
 * properties while it is being used by a PackageBuilder.
 *
 * drools.dialect.default = <String>
 * drools.accumulate.function.<function name> = <qualified class>
 * drools.evaluator.<ident> = <qualified class>
 * drools.dump.dir = <String>
 * drools.classLoaderCacheEnabled = true|false
 * drools.parallelRulesBuildThreshold = <int>
 *
 * default dialect is java.
 * Available preconfigured Accumulate functions are:
 * drools.accumulate.function.average = org.kie.base.accumulators.AverageAccumulateFunction
 * drools.accumulate.function.max = org.kie.base.accumulators.MaxAccumulateFunction
 * drools.accumulate.function.min = org.kie.base.accumulators.MinAccumulateFunction
 * drools.accumulate.function.count = org.kie.base.accumulators.CountAccumulateFunction
 * drools.accumulate.function.sum = org.kie.base.accumulators.SumAccumulateFunction
 * 
 * drools.parser.processStringEscapes = true|false
 * 
 * 
 * drools.problem.severity.<ident> = ERROR|WARNING|INFO
 * 
 */
public class KnowledgeBuilderFlowConfigurationImpl
        implements
        KnowledgeBuilderConfiguration {

    public static final ConfigurationKey<KnowledgeBuilderFlowConfigurationImpl> KEY = new ConfigurationKey<>("Flow");

    CompositeConfiguration<KnowledgeBuilderOption, SingleValueKieBuilderOption, MultiValueKieBuilderOption> compConfig;

    private ClassLoader                       classLoader;

    private ChainedProperties                 chainedProperties;

    private static final Logger log = LoggerFactory.getLogger(KnowledgeBuilderFlowConfigurationImpl.class);


    /**
     * Programmatic properties file, added with lease precedence
     */
    public KnowledgeBuilderFlowConfigurationImpl(CompositeConfiguration<KnowledgeBuilderOption, SingleValueKieBuilderOption, MultiValueKieBuilderOption> compConfig,
                                                 ClassLoader classLoader,
                                                 ChainedProperties chainedProperties) {
        this.compConfig = compConfig;
        this.classLoader = ProjectClassLoader.getClassLoader(classLoader, getClass());
        this.chainedProperties = chainedProperties;
    }

    @Override public void makeImmutable() {
        throw new UnsupportedOperationException("Not Implemented Yet");
    }

    public void setProperty(String name,
            String value) {
    }

    public String getProperty(String name) {
        return null;
    }

    public ChainedProperties getChainedProperties() {
        return this.chainedProperties;
    }

    public ClassLoader getClassLoader() {
        return this.classLoader;
    }


    @SuppressWarnings("unchecked")
    public <T extends SingleValueKieBuilderOption> T getOption(OptionKey<T> option) {

        return null;
    }

    @SuppressWarnings("unchecked")
    public <T extends MultiValueKieBuilderOption> T getOption(OptionKey<T> option,
                                                              String subKey) {
        return null;
    }

    public <T extends MultiValueKieBuilderOption> Set<String> getOptionSubKeys(OptionKey<T> option) {
        return null;
    }

    public <T extends KnowledgeBuilderOption> void setOption(T option) {
    }

    @Override public <X extends OptionsConfiguration<KnowledgeBuilderOption, SingleValueKieBuilderOption, MultiValueKieBuilderOption>> X as(ConfigurationKey<X> configuration) {
        return compConfig.as(configuration);
    }
}
