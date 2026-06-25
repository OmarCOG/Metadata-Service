package com.mes.catalogue;

import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Default {@link DatasetFileStorage}: persists original-file bytes in the
 * relational DB via {@link DatasetFileBlobRepository}. Swap for an S3-backed
 * implementation on AWS without changing callers.
 */
@Component
public class DbFileStorage implements DatasetFileStorage {

    private final DatasetFileBlobRepository blobRepository;

    public DbFileStorage(DatasetFileBlobRepository blobRepository) {
        this.blobRepository = blobRepository;
    }

    @Override
    public void store(Long datasetId, String originalFileName, String contentType, byte[] content) {
        DatasetFileBlob blob = blobRepository.findByDatasetId(datasetId)
                .orElseGet(DatasetFileBlob::new);
        blob.setDatasetId(datasetId);
        blob.setOriginalFileName(originalFileName);
        blob.setContentType(contentType);
        blob.setContent(content);
        blobRepository.save(blob);
    }

    @Override
    public Optional<StoredFile> load(Long datasetId) {
        return blobRepository.findByDatasetId(datasetId)
                .map(b -> new StoredFile(b.getOriginalFileName(), b.getContentType(), b.getContent()));
    }
}
