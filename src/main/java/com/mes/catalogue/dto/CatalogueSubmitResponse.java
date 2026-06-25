package com.mes.catalogue.dto;

import java.time.Instant;

/** Returned after a successful registration — drives the UI confirmation screen. */
public record CatalogueSubmitResponse(Long id, String exchangeId, Instant createdAt) {
}
