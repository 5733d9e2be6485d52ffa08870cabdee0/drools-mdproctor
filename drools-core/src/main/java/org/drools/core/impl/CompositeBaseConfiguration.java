package org.drools.core.impl;

import java.util.Properties;

import org.kie.api.conf.KieBaseConfiguration;
import org.kie.api.conf.ConfigurationFactory;
import org.kie.api.conf.CompositeConfiguration;
import org.kie.api.conf.KieBaseOption;
import org.kie.api.conf.KieBaseOptionsConfiguration;
import org.kie.api.conf.MultiValueKieBaseOption;
import org.kie.api.conf.SingleValueKieBaseOption;

public class CompositeBaseConfiguration extends CompositeConfiguration<KieBaseOption, SingleValueKieBaseOption, MultiValueKieBaseOption> implements KieBaseConfiguration {

    public CompositeBaseConfiguration(Properties properties, ClassLoader classloader,
                                      ConfigurationFactory<KieBaseOption, SingleValueKieBaseOption, MultiValueKieBaseOption>... factories) {
        super(properties, classloader, factories);
    }

}
