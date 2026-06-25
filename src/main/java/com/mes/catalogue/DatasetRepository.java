package com.mes.catalogue;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** CRUD access to registered catalogue datasets. */
public interface DatasetRepository extends JpaRepository<DatasetRecord, Long> {

    /** Newest registrations first — drives the catalogue listing order. */
    List<DatasetRecord> findAllByOrderByCreatedAtDesc();
}
