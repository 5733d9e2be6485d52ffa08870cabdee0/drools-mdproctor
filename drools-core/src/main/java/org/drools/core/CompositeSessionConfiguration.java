package org.drools.core;

import java.util.Properties;

import org.kie.api.conf.ConfigurationFactory;
import org.kie.api.conf.CompositeConfiguration;
import org.kie.api.runtime.conf.KieSessionConfiguration;
import org.kie.api.runtime.conf.KieSessionOption;
import org.kie.api.runtime.conf.MultiValueKieSessionOption;
import org.kie.api.runtime.conf.SingleValueKieSessionOption;

public class CompositeSessionConfiguration extends CompositeConfiguration<KieSessionOption, SingleValueKieSessionOption, MultiValueKieSessionOption> implements KieSessionConfiguration {

    public CompositeSessionConfiguration(Properties properties, ClassLoader classloader,
                                         ConfigurationFactory<KieSessionOption, SingleValueKieSessionOption, MultiValueKieSessionOption>... factories) {
        super(properties, classloader, factories);
    }

}
