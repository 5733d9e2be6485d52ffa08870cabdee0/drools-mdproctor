package org.kie.api.conf;

import java.util.Properties;

import org.kie.internal.utils.ChainedProperties;

public interface ConfigurationFactory<T extends Option, S extends SingleValueOption, M extends MultiValueOption> {

    String type();

    OptionsConfiguration<T, S, M> create(ClassLoader classLoader, ChainedProperties chainedProperties);
}
