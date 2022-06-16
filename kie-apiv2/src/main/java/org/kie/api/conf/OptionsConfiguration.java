package org.kie.api.conf;

public interface OptionsConfiguration<T extends Option, S extends SingleValueOption, M extends MultiValueOption> {

    /**
     * Gets an option value
     *
     * @param option the option class for the option being requested
     * @param <C extends T>
     *
     * @return the Option value for the given option. Returns null if option is
     *         not configured.
     */
    public <C extends T> void setOption( C option );


    /**
     * Gets an option value for the given option + key. This method should
     * be used for multi-value options, like accumulate functions configuration
     * where one option has multiple values, distinguished by a sub-key.
     *
     * @param option the option class for the option being requested
     * @param key the key for the option being requested
     * @param <C extends M> C
     *
     * @return the Option value for the given option + key. Returns null if option is
     *         not configured.
     */
    public <C extends M> C getOption( Class<C> option, String key );



    /**
     * Gets an option value
     *
     * @param option the option class for the option being requested
     * @param <C extends S> C
     *
     * @return the Option value for the given option. Returns null if option is
     *         not configured.
     */
    public <C extends S> C getOption( Class<C> option );
}
