/*
 * Copyright 2015 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.drools.core;

import java.io.Externalizable;
import java.util.Map;
import java.util.Set;

import org.drools.core.process.WorkItemManagerFactory;
import org.kie.api.conf.ConfigurationKey;
import org.kie.api.conf.OptionKey;
import org.kie.api.runtime.conf.KieSessionConfiguration;
import org.kie.api.runtime.conf.KieSessionOption;
import org.kie.api.runtime.conf.MultiValueKieSessionOption;
import org.kie.api.runtime.conf.SingleValueKieSessionOption;
import org.kie.api.runtime.conf.WorkItemHandlerOption;
import org.kie.api.runtime.process.WorkItemHandler;
public abstract class FlowSessionConfiguration implements KieSessionConfiguration, Externalizable {

//    public static FlowSessionConfiguration newInstance() {
//        return new SessionConfigurationImpl();
//    }
//
//    public static FlowSessionConfiguration newInstance(Properties properties) {
//        return new SessionConfigurationImpl(properties);
//    }

    public static final ConfigurationKey<FlowSessionConfiguration> KEY = new ConfigurationKey<>("Flow");

    public abstract Map<String, WorkItemHandler> getWorkItemHandlers();
    public abstract Map<String, WorkItemHandler> getWorkItemHandlers(Map<String, Object> params);
    public abstract WorkItemManagerFactory getWorkItemManagerFactory();
    public abstract void setWorkItemManagerFactory(WorkItemManagerFactory workItemManagerFactory);

    public abstract String getProcessInstanceManagerFactory();

    public abstract String getSignalManagerFactory();


    public final <T extends KieSessionOption> void setOption(T option) {
        switch (option.propertyName()) {
            case WorkItemHandlerOption.PROPERTY_NAME: {
                getWorkItemHandlers().put(((WorkItemHandlerOption) option).getName(),
                                          ((WorkItemHandlerOption) option).getHandler());
            }
        }
    }

    @SuppressWarnings("unchecked")
    public final <T extends SingleValueKieSessionOption> T getOption(OptionKey<T> option) {
        return null;
    }

    @SuppressWarnings("unchecked")
    public final <T extends MultiValueKieSessionOption> T getOption(OptionKey<T> option,
                                                                    String subKey) {
        switch(option.name()) {
            case WorkItemHandlerOption.PROPERTY_NAME: {
                return (T) WorkItemHandlerOption.get(subKey,
                                                     getWorkItemHandlers().get(subKey));
            }
        }
        return null;
    }

    @Override public <C extends MultiValueKieSessionOption> Set<String> getOptionSubKeys(OptionKey<C> optionKey) {
        return getWorkItemHandlers().keySet();
    }

    public final void setProperty(String name,
                                  String value) {
    }

    public final String getProperty(String name) {
        return null;
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FlowSessionConfiguration that = (FlowSessionConfiguration) o;

        return getWorkItemHandlers().equals(that.getWorkItemHandlers());
    }

    @Override
    public final int hashCode() {
        int result = getWorkItemHandlers().hashCode();
        return result;
    }
}
