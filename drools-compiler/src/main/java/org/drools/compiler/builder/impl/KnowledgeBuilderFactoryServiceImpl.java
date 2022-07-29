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
*/

package org.drools.compiler.builder.impl;

import java.util.Properties;

import org.drools.compiler.builder.conf.DecisionTableConfigurationImpl;
import org.drools.kiesession.rulebase.InternalKnowledgeBase;
import org.drools.wiring.api.classloader.ProjectClassLoader;
import org.kie.api.KieBase;
import org.kie.internal.builder.DecisionTableConfiguration;
import org.kie.internal.builder.KnowledgeBuilder;
import org.kie.internal.builder.KnowledgeBuilderConfiguration;
import org.kie.internal.builder.KnowledgeBuilderFactoryService;

public class KnowledgeBuilderFactoryServiceImpl implements KnowledgeBuilderFactoryService {

    @Override
    public KnowledgeBuilderConfiguration newKnowledgeBuilderConfiguration() {
        ClassLoader projClassLoader = getClassLoader(null);
        return new CompositeBuilderConfiguration(null, projClassLoader,
                                                 KnowledgeBuilderConfigurationFactories.baseConf,
                                                 KnowledgeBuilderConfigurationFactories.ruleConf,
                                                 KnowledgeBuilderConfigurationFactories.flowConf);
    }

    @Override
    public KnowledgeBuilderConfiguration newKnowledgeBuilderConfiguration(ClassLoader classLoader) {
        ClassLoader projClassLoader = getClassLoader(classLoader);
        return new CompositeBuilderConfiguration(null, projClassLoader,
                                                 KnowledgeBuilderConfigurationFactories.baseConf,
                                                 KnowledgeBuilderConfigurationFactories.ruleConf,
                                                 KnowledgeBuilderConfigurationFactories.flowConf);
    }

    private ClassLoader getClassLoader(ClassLoader classLoader) {
        ClassLoader projClassLoader = classLoader instanceof ProjectClassLoader ? classLoader : ProjectClassLoader.getClassLoader(classLoader, getClass());
        return projClassLoader;
    }

    @Override
    public KnowledgeBuilderConfiguration newKnowledgeBuilderConfiguration(Properties properties, ClassLoader classLoader) {
        ClassLoader projClassLoader = getClassLoader(classLoader);
        return new CompositeBuilderConfiguration(properties, projClassLoader,
                                                 KnowledgeBuilderConfigurationFactories.baseConf,
                                                 KnowledgeBuilderConfigurationFactories.ruleConf,
                                                 KnowledgeBuilderConfigurationFactories.flowConf);
    }

    @Override
    public DecisionTableConfiguration newDecisionTableConfiguration() {
        return new DecisionTableConfigurationImpl();
    }

    @Override
    public KnowledgeBuilder newKnowledgeBuilder() {
        return new KnowledgeBuilderImpl( );
    }

    @Override
    public KnowledgeBuilder newKnowledgeBuilder(KnowledgeBuilderConfiguration conf) {
        return new KnowledgeBuilderImpl(conf);
    }

    @Override
    public KnowledgeBuilder newKnowledgeBuilder(KieBase kbase) {
        if ( kbase != null ) {
            return new KnowledgeBuilderImpl( (InternalKnowledgeBase)kbase );
        } else {
            return new KnowledgeBuilderImpl();
        }
    }

    @Override
    public KnowledgeBuilder newKnowledgeBuilder(KieBase kbase,
                                                KnowledgeBuilderConfiguration conf) {
        if ( kbase != null ) {
            return new KnowledgeBuilderImpl( (InternalKnowledgeBase)kbase, (KnowledgeBuilderConfigurationImpl) conf );
        } else {
            return new KnowledgeBuilderImpl((KnowledgeBuilderConfigurationImpl) conf );
        }        
    }
}
