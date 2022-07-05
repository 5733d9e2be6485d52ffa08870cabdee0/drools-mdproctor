package org.drools.core.impl;

import org.drools.core.FlowBaseConfiguration;
import org.drools.core.KieBaseConfigurationImpl;
import org.drools.core.RuleBaseConfiguration;
import org.kie.api.conf.ConfigurationFactory;
import org.kie.api.conf.KieBaseOption;
import org.kie.api.conf.MultiValueKieBaseOption;
import org.kie.api.conf.OptionsConfiguration;
import org.kie.api.conf.SingleValueKieBaseOption;
import org.kie.internal.utils.ChainedProperties;

public class ConfigurationFactories {
    public static ConfigurationFactory<KieBaseOption, SingleValueKieBaseOption, MultiValueKieBaseOption> baseConf = new ConfigurationFactory() {

        @Override public String type() {
            return "Base";
        }

        @Override public OptionsConfiguration create(ClassLoader classLoader,
                                                     ChainedProperties chainedProperties) {
            return new KieBaseConfigurationImpl(classLoader, chainedProperties);
        }
    };

    public static ConfigurationFactory<KieBaseOption, SingleValueKieBaseOption, MultiValueKieBaseOption> ruleConf = new ConfigurationFactory() {

        @Override public String type() {
            return "Rule";
        }

        @Override public OptionsConfiguration create(ClassLoader classLoader, ChainedProperties chainedProperties) {
            return new RuleBaseConfiguration(classLoader, chainedProperties);
        }
    };

    public static ConfigurationFactory<KieBaseOption, SingleValueKieBaseOption, MultiValueKieBaseOption> flowConf = new ConfigurationFactory() {

        @Override public String type() {
            return "Flow";
        }

        @Override public OptionsConfiguration create(ClassLoader classLoader, ChainedProperties chainedProperties) {
            return new FlowBaseConfiguration(classLoader, chainedProperties);
        }
    };

}
