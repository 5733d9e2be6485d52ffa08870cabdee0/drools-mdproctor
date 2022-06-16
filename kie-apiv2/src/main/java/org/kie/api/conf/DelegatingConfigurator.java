package org.kie.api.conf;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class DelegatingConfigurator<T extends Option, S extends SingleValueOption, M extends MultiValueOption>
      implements OptionsConfiguration<T, S, M> {
    private Map<String, OptionsConfiguration> configurators = new HashMap<>();

    public DelegatingConfigurator() {

    }

    @Override public <C extends T> void setOption(C option) {

    }

    @Override public <C extends M> C getOption(Class<C> option, String key) {
        return null;
    }

    @Override public <C extends S> C getOption(Class<C> option) {
        return null;
    }

    @Override public <C extends M> Set<String> getOptionKeys(Class<C> option) {
        return OptionsConfiguration.super.getOptionKeys(option);
    }
}
