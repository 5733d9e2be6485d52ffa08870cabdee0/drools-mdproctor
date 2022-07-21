package org.drools.compiler.builder.impl;

import java.util.Properties;

import org.kie.api.conf.CompositeConfiguration;
import org.kie.api.conf.ConfigurationFactory;
import org.kie.api.conf.KieBaseConfiguration;
import org.kie.api.conf.KieBaseOption;
import org.kie.api.conf.MultiValueKieBaseOption;
import org.kie.api.conf.SingleValueKieBaseOption;
import org.kie.internal.builder.KnowledgeBuilderConfiguration;
import org.kie.internal.builder.conf.KnowledgeBuilderOption;
import org.kie.internal.builder.conf.MultiValueKieBuilderOption;
import org.kie.internal.builder.conf.SingleValueKieBuilderOption;

public class CompositeBuilderConfiguration extends CompositeConfiguration<KnowledgeBuilderOption, SingleValueKieBuilderOption, MultiValueKieBuilderOption> implements KnowledgeBuilderConfiguration {

    public CompositeBuilderConfiguration(Properties properties, ClassLoader classloader,
                                         ConfigurationFactory<KnowledgeBuilderOption, SingleValueKieBuilderOption, MultiValueKieBuilderOption>... factories) {
        super(properties, classloader, factories);
    }

}
