package com.mes.catalogue;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

/**
 * CRUD access to registered catalogue datasets. Extends
 * {@link JpaSpecificationExecutor} so the listing can be searched/filtered and
 * paginated via dynamically-built specifications.
 */
public interface DatasetRepository
        extends JpaRepository<DatasetRecord, Long>, JpaSpecificationExecutor<DatasetRecord> {

    /** Newest registrations first — used as the default listing order. */
    List<DatasetRecord> findAllByOrderByCreatedAtDesc();
}
