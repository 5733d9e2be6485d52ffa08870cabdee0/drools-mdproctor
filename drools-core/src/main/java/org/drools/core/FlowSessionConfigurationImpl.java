/*
 * Copyright 2010 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.drools.core;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.drools.base.util.MVELExecutor;
import org.drools.core.process.WorkItemManagerFactory;
import org.drools.core.util.ConfFileUtils;
import org.drools.wiring.api.classloader.ProjectClassLoader;
import org.kie.api.conf.CompositeConfiguration;
import org.kie.api.conf.ConfigurationKey;
import org.kie.api.conf.OptionsConfiguration;
import org.kie.api.runtime.conf.KieSessionOption;
import org.kie.api.runtime.conf.MultiValueKieSessionOption;
import org.kie.api.runtime.conf.SingleValueKieSessionOption;
import org.kie.api.runtime.conf.TimerJobFactoryOption;
import org.kie.api.runtime.process.WorkItemHandler;
import org.kie.internal.utils.ChainedProperties;

/**
 * SessionConfiguration
 *
 * A class to store Session related configuration. It must be used at session instantiation time
 * or not used at all.
 * This class will automatically load default values from system properties, so if you want to set
 * a default configuration value for all your new sessions, you can simply set the property as
 * a System property.
 *
 * After the Session is created, it makes the configuration immutable and there is no way to make it
 * mutable again. This is to avoid inconsistent behavior inside session.
 *
 * NOTE: This API is under review and may change in the future.
 * 
 * 
 * drools.keepReference = <true|false>
 * drools.clockType = <pseudo|realtime|heartbeat|implicit>
 */
public class FlowSessionConfigurationImpl extends FlowSessionConfiguration {

    private static final long              serialVersionUID = 510l;

    private CompositeConfiguration<KieSessionOption, SingleValueKieSessionOption, MultiValueKieSessionOption> compConfig;

    private ChainedProperties              chainedProperties;

    private volatile boolean               immutable;

    private Map<String, WorkItemHandler>   workItemHandlers;
    private WorkItemManagerFactory         workItemManagerFactory;

    private transient ClassLoader          classLoader;

    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeObject( chainedProperties );
        out.writeBoolean(immutable);
    }

    @SuppressWarnings("unchecked")
    public void readExternal(ObjectInput in) throws IOException,
                                            ClassNotFoundException {
        chainedProperties = (ChainedProperties) in.readObject();
        immutable = in.readBoolean();
    }

    public FlowSessionConfigurationImpl(CompositeConfiguration<KieSessionOption, SingleValueKieSessionOption, MultiValueKieSessionOption> compConfig,
                                        ClassLoader classLoader, ChainedProperties chainedProperties) {
        this.compConfig = compConfig;
        setClassLoader( classLoader);
        this.immutable = false;
        this.chainedProperties = chainedProperties;
    }

    @Override public ClassLoader getClassLoader() {
        return this.classLoader;
    }

    public void setClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    /**
     * Makes the configuration object immutable. Once it becomes immutable,
     * there is no way to make it mutable again.
     * This is done to keep consistency.
     */
    public void makeImmutable() {
        this.immutable = true;
    }

    /**
     * Returns true if this configuration object is immutable or false otherwise.
     */
    public boolean isImmutable() {
        return this.immutable;
    }

    private void checkCanChange() {
        if ( this.immutable ) {
            throw new UnsupportedOperationException( "Can't set a property after configuration becomes immutable" );
        }
    }

    public Map<String, WorkItemHandler> getWorkItemHandlers() {

        if ( this.workItemHandlers == null ) {
            initWorkItemHandlers(new HashMap<String, Object>());
        }
        return this.workItemHandlers;

    }
    
    public Map<String, WorkItemHandler> getWorkItemHandlers(Map<String, Object> params) {
        
        if ( this.workItemHandlers == null ) {
            initWorkItemHandlers(params);
        }
        return this.workItemHandlers;
    }
    

    private void initWorkItemHandlers(Map<String, Object> params) {
        this.workItemHandlers = new HashMap<String, WorkItemHandler>();

        // split on each space
        String locations[] = getPropertyValue( "drools.workItemHandlers", "" ).split( "\\s" );

        // load each SemanticModule
        for ( String factoryLocation : locations ) {
            // trim leading/trailing spaces and quotes
            factoryLocation = factoryLocation.trim();
            if ( factoryLocation.startsWith( "\"" ) ) {
                factoryLocation = factoryLocation.substring( 1 );
            }
            if ( factoryLocation.endsWith( "\"" ) ) {
                factoryLocation = factoryLocation.substring( 0,
                                                             factoryLocation.length() - 1 );
            }
            if ( !factoryLocation.equals( "" ) ) {
                loadWorkItemHandlers( factoryLocation, params );
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void loadWorkItemHandlers(String location, Map<String, Object> params) {
        String content = ConfFileUtils.URLContentsToString( ConfFileUtils.getURL( location,
                                                                                  null,
                                                                                  RuleBaseConfiguration.class ) );
        Map<String, WorkItemHandler> workItemHandlers = (Map<String, WorkItemHandler>) MVELExecutor.get().eval( content, params );
        this.workItemHandlers.putAll( workItemHandlers );
    }

    public WorkItemManagerFactory getWorkItemManagerFactory() {
        if ( this.workItemManagerFactory == null ) {
            initWorkItemManagerFactory();
        }
        return this.workItemManagerFactory;
    }

    @Override
    public void setWorkItemManagerFactory(WorkItemManagerFactory workItemManagerFactory) {
        this.workItemManagerFactory = workItemManagerFactory;
    }

    @SuppressWarnings("unchecked")
    private void initWorkItemManagerFactory() {
        String className = getPropertyValue( "drools.workItemManagerFactory", "org.drools.core.process.impl.DefaultWorkItemManagerFactory" );
        Class<WorkItemManagerFactory> clazz = null;
        try {
            clazz = (Class<WorkItemManagerFactory>) this.classLoader.loadClass( className );
        } catch ( ClassNotFoundException e ) {
        }

        if ( clazz != null ) {
            try {
                this.workItemManagerFactory = clazz.newInstance();
            } catch ( Exception e ) {
                throw new IllegalArgumentException( "Unable to instantiate work item manager factory '" + className + "'",
                                                    e );
            }
        } else {
            throw new IllegalArgumentException( "Work item manager factory '" + className + "' not found" );
        }
    }

    public String getProcessInstanceManagerFactory() {
        return getPropertyValue( "drools.processInstanceManagerFactory", "org.jbpm.process.instance.impl.DefaultProcessInstanceManagerFactory" );
    }

    public String getSignalManagerFactory() {
        return getPropertyValue( "drools.processSignalManagerFactory", "org.jbpm.process.instance.event.DefaultSignalManagerFactory" );
    }

    @Override public <X extends OptionsConfiguration<KieSessionOption, SingleValueKieSessionOption, MultiValueKieSessionOption>> X as(ConfigurationKey<X> key) {
        return compConfig.as(key);
    }


    @SuppressWarnings("unchecked")

    public String getPropertyValue( String name, String defaultValue ) {
        return this.chainedProperties.getProperty( name, defaultValue );
    }

}
