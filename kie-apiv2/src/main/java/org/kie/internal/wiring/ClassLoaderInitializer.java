package org.kie.internal.wiring;

public interface ClassLoaderInitializer<T> {

    public void initFrom(T other);

}
