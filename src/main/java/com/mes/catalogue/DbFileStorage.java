package com.mes.catalogue;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Default {@link DatasetFileStorage}: persists original-file bytes in the
 * relational DB via {@link DatasetFileBlobRepository}. Swap for an S3-backed
 * implementation on AWS without changing callers.
 *
 * <p>File bytes are encrypted at rest with {@link FileEncryption} (AES-256-GCM):
 * encrypted on {@link #store} and decrypted on {@link #load}, so the
 * {@code dataset_file_blob.content} column never holds plaintext banking data.</p>
 */
@Component
public class DbFileStorage implements DatasetFileStorage {

    private final DatasetFileBlobRepository blobRepository;
    private final FileEncryption fileEncryption;

    public DbFileStorage(DatasetFileBlobRepository blobRepository, FileEncryption fileEncryption) {
        this.blobRepository = blobRepository;
        this.fileEncryption = fileEncryption;
    }

    @Override
    public void store(Long datasetId, String originalFileName, String contentType, byte[] content) {
        DatasetFileBlob blob = blobRepository.findByDatasetId(datasetId)
                .orElseGet(DatasetFileBlob::new);
        blob.setDatasetId(datasetId);
        blob.setOriginalFileName(originalFileName);
        blob.setContentType(contentType);
        blob.setContent(fileEncryption.encrypt(content));
        blobRepository.save(blob);
    }

    @Override
    public Optional<StoredFile> load(Long datasetId) {
        return blobRepository.findByDatasetId(datasetId)
                .map(b -> new StoredFile(b.getOriginalFileName(), b.getContentType(),
                        fileEncryption.decrypt(b.getContent())));
    }

    @Override
    @Transactional
    public void delete(Long datasetId) {
        blobRepository.deleteByDatasetId(datasetId);
    }
}
