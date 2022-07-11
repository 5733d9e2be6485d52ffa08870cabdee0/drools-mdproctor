package org.kie.api.conf;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.kie.internal.utils.ChainedProperties;

public class CompositeConfiguration<T extends Option, S extends SingleValueOption, M extends MultiValueOption>
      implements OptionsConfiguration<T, S, M> {

    private ClassLoader classLoader;

    private ChainedProperties chainedProperties;

    private Map<String, OptionsConfiguration<T, S, M>> configurations = new HashMap<>();

    public CompositeConfiguration(Properties properties, ClassLoader classloader, ConfigurationFactory<T, S, M>... factories) {
        setClassLoader(classloader);
        this.chainedProperties = ChainedProperties.getChainedProperties( this.classLoader );

        if ( properties != null ) {
            this.chainedProperties.addProperties( properties );
        }

        for(ConfigurationFactory<T, S, M> f  : factories) {
            configurations.put(f.type(), f.create(this, classloader, chainedProperties));
        }
    }

    private OptionsConfiguration<T, S, M> getOptionsConfiguration(String type) {
        return this.configurations.get(type);
    }

    @Override public void makeImmutable() {
        configurations.values().stream().forEach(conf -> conf.makeImmutable());
    }

    @Override public <C extends T> void setOption(C option) {
        OptionsConfiguration delegate = configurations.get(option.type());
        if (delegate==null) {
            throw new RuntimeException("Configuration for type " + option.type() + "does not exist");
        }
        delegate.setOption(option);
    }

    @Override public <C extends S> C getOption(OptionKey<C> optionKey) {
        OptionsConfiguration<T, S, M> delegate = configurations.get(optionKey.type());
        if (delegate==null) {
            throw new RuntimeException("Configuration for type " + optionKey.type() + "does not exist");
        }
        return delegate.getOption(optionKey);
    }

    @Override public <C extends M> C getOption(OptionKey<C> optionKey, String subKey) {
        OptionsConfiguration<T, S, M> delegate = configurations.get(optionKey.type());
        if (delegate==null) {
            throw new RuntimeException("Configuration for type " + optionKey.type() + "does not exist");
        }
        return delegate.getOption(optionKey, subKey);
    }

    @Override public <C extends M> Set<String> getOptionSubKeys(OptionKey<C> optionKey) {
        OptionsConfiguration<T, S, M> delegate = configurations.get(optionKey.type());
        if (delegate==null) {
            throw new RuntimeException("Configuration for type " + optionKey.type() + "does not exist");
        }
        return delegate.getOptionSubKeys(optionKey);
    }

    public ClassLoader getClassLoader() {
        return this.classLoader;
    }

    public void setClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    public ChainedProperties getChainedProperties() {
        return chainedProperties;
    }

    public <X extends OptionsConfiguration<T, S, M>> X as(ConfigurationKey<X> configuration) {
        return (X) configurations.get(configuration.type());
    }
}
