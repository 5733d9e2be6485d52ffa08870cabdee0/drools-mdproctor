package org.kie.api.conf;

public class Configuration<T extends OptionsConfiguration> {
    private String type;

    public Configuration(String type) {
        this.type = type;
    }

    public String type() {
        return type;
    }
}
