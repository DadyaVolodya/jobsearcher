package ru.fsp.jobsearcher.infrastructure.storage;

import java.io.InputStream;

public interface StoragePort {
    StoredObject store(String key, InputStream data, long size, String contentType);

    record StoredObject(String key, long sizeBytes) {
    }
}
