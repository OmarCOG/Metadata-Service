package com.mes.catalogue;

import java.util.Optional;

/**
 * Stores and retrieves the original uploaded file for a registered dataset.
 *
 * <p>The default {@link DbFileStorage} persists bytes in the database. On AWS,
 * an {@code S3FileStorage} implementation can replace it without touching the
 * service or controller — metadata stays in the relational DB, files move to S3.
 */
public interface DatasetFileStorage {

    /** Persists the raw bytes of a dataset's original file. */
    void store(Long datasetId, String originalFileName, String contentType, byte[] content);

    /** Loads a previously stored file, or empty if none exists for the id. */
    Optional<StoredFile> load(Long datasetId);

    /** Removes the stored file for a dataset (no-op if none exists). */
    void delete(Long datasetId);

    /** An original file retrieved from storage. */
    record StoredFile(String originalFileName, String contentType, byte[] content) {}
}
