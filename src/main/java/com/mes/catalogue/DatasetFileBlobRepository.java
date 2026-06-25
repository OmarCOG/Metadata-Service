package com.mes.catalogue;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** CRUD access to stored original-file bytes, keyed by dataset id. */
public interface DatasetFileBlobRepository extends JpaRepository<DatasetFileBlob, Long> {

    Optional<DatasetFileBlob> findByDatasetId(Long datasetId);
}
