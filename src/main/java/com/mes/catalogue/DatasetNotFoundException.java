package com.mes.catalogue;

/** Thrown when a catalogue dataset id does not exist — mapped to HTTP 404. */
public class DatasetNotFoundException extends RuntimeException {
    public DatasetNotFoundException(Long id) {
        super("No catalogue dataset found with id " + id);
    }
}
