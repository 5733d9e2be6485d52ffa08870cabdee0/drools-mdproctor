package org.drools.core.impl;

import java.util.Properties;

import org.kie.api.KieBaseConfiguration;
import org.kie.api.conf.ConfigurationFactory;
import org.kie.api.conf.DelegatingConfiguration;
import org.kie.api.conf.KieBaseOption;
import org.kie.api.conf.MultiValueKieBaseOption;
import org.kie.api.conf.SingleValueKieBaseOption;

public class DelegatingBaseConfiguration extends DelegatingConfiguration<KieBaseOption, SingleValueKieBaseOption, MultiValueKieBaseOption> implements KieBaseConfiguration {

    public DelegatingBaseConfiguration(Properties properties, ClassLoader classloader,
                                       ConfigurationFactory<KieBaseOption, SingleValueKieBaseOption, MultiValueKieBaseOption>... factories) {
        super(properties, classloader, factories);
    }

}
