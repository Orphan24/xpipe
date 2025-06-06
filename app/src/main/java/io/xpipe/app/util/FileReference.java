package io.xpipe.app.util;

import io.xpipe.app.ext.LocalStore;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.core.store.FileSystemStore;

import lombok.Value;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.nio.file.Path;

/**
 * Represents a file located on a file system.
 */
@SuperBuilder
@Jacksonized
@Value
public class FileReference {

    DataStoreEntryRef<? extends FileSystemStore> fileSystem;
    String path;

    public FileReference(DataStoreEntryRef<? extends FileSystemStore> fileSystem, String path) {
        this.fileSystem = fileSystem;
        this.path = path;
    }

    public static FileReference local(Path p) {
        return new FileReference(DataStorage.get().local().ref(), p.toString());
    }

    public static FileReference local(String p) {
        return new FileReference(DataStorage.get().local().ref(), p);
    }

    public boolean isLocal() {
        return fileSystem.getStore() instanceof LocalStore;
    }
}
