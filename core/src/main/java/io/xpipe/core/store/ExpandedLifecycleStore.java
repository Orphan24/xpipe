package io.xpipe.core.store;

public interface ExpandedLifecycleStore extends DataStore {

    default void initializeStore() {}

    default void finalizeStore() throws Exception {}
}
