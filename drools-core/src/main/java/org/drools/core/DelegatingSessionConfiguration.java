package org.drools.core;

import java.util.Properties;

import org.kie.api.KieBaseConfiguration;
import org.kie.api.conf.ConfigurationFactory;
import org.kie.api.conf.DelegatingConfiguration;
import org.kie.api.conf.KieBaseOption;
import org.kie.api.conf.MultiValueKieBaseOption;
import org.kie.api.conf.SingleValueKieBaseOption;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.KieSessionConfiguration;
import org.kie.api.runtime.conf.KieSessionOption;
import org.kie.api.runtime.conf.MultiValueKieSessionOption;
import org.kie.api.runtime.conf.SingleValueKieSessionOption;

public class DelegatingSessionConfiguration extends DelegatingConfiguration<KieSessionOption, SingleValueKieSessionOption, MultiValueKieSessionOption> implements KieSessionConfiguration {

    public DelegatingSessionConfiguration(Properties properties, ClassLoader classloader,
                                          ConfigurationFactory<KieSessionOption, SingleValueKieSessionOption, MultiValueKieSessionOption>... factories) {
        super(properties, classloader, factories);
    }

}
