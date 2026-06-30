package com.mes.catalogue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mes.catalogue.dto.CatalogueDetail;
import com.mes.catalogue.dto.CatalogueSubmitRequest;
import com.mes.catalogue.dto.CatalogueUpdateRequest;
import com.mes.models.EnhancedMetadataResponse;
import com.mes.models.FieldMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link CatalogueService} using mocked repositories/storage —
 * no database or Spring context required.
 */
@ExtendWith(MockitoExtension.class)
class CatalogueServiceTest {

    @Mock DatasetRepository datasetRepository;
    @Mock DatasetFileStorage fileStorage;
    final ObjectMapper objectMapper = new ObjectMapper();
    CatalogueService service;

    @BeforeEach
    void setUp() {
        service = new CatalogueService(datasetRepository, fileStorage, objectMapper);
    }

    private FieldMetadata field(String name, boolean pci, boolean npi, List<String> tags) {
        FieldMetadata f = new FieldMetadata();
        f.setFieldName(name);
        f.setDataType("string");
        f.setTags(tags);
        f.setPciData(pci);
        f.setNpiData(npi);
        return f;
    }

    @Test
    void register_persistsRecordWithComputedCountsAndTags_andStoresFile() {
        EnhancedMetadataResponse meta = new EnhancedMetadataResponse();
        meta.setFileName("cust.csv");
        meta.setFileFormat("csv");
        meta.setTotalRecords(100);
        meta.setFields(List.of(
                field("card", true, false, List.of("financial", "pci")),
                field("name", false, true, List.of("personal"))));

        CatalogueSubmitRequest req = new CatalogueSubmitRequest();
        req.setTitle("Customers");
        req.setDescription("desc");
        req.setDataSteward("steward@capitalone.com");
        req.setTags(List.of("billing", "billing", "  ", "monthly"));
        req.setPiiData(true);
        req.setDataRetentionYears(5);
        req.setMetadata(meta);

        MockMultipartFile file = new MockMultipartFile("file", "cust.csv", "text/csv", "a,b\n1,2".getBytes());

        when(datasetRepository.existsByTitleIgnoreCase("Customers")).thenReturn(false);
        when(datasetRepository.save(any(DatasetRecord.class))).thenAnswer(inv -> {
            DatasetRecord r = inv.getArgument(0);
            r.setId(7L);
            return r;
        });

        CatalogueDetail detail = service.register(req, file);

        assertThat(detail.id()).isEqualTo(7L);
        assertThat(detail.title()).isEqualTo("Customers");
        assertThat(detail.dataSteward()).isEqualTo("steward@capitalone.com");
        assertThat(detail.piiData()).isTrue();
        assertThat(detail.dataRetentionYears()).isEqualTo(5);
        assertThat(detail.tags()).containsExactly("billing", "monthly"); // trimmed + de-duped
        assertThat(detail.totalFields()).isEqualTo(2);
        assertThat(detail.pciFieldsCount()).isEqualTo(1);
        assertThat(detail.npiFieldsCount()).isEqualTo(1);

        ArgumentCaptor<DatasetRecord> rec = ArgumentCaptor.forClass(DatasetRecord.class);
        verify(datasetRepository).save(rec.capture());
        assertThat(rec.getValue().getAllTags()).contains("financial", "pci", "personal");

        verify(fileStorage).store(eq(7L), eq("cust.csv"), eq("text/csv"), any(byte[].class));
    }

    @Test
    void register_rejectsBlankTitle() {
        CatalogueSubmitRequest req = new CatalogueSubmitRequest();
        req.setMetadata(new EnhancedMetadataResponse());
        MockMultipartFile file = new MockMultipartFile("file", "f.csv", "text/csv", "x".getBytes());
        assertThatThrownBy(() -> service.register(req, file))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void register_rejectsDuplicateName() {
        EnhancedMetadataResponse meta = new EnhancedMetadataResponse();
        meta.setFields(List.of());
        CatalogueSubmitRequest req = new CatalogueSubmitRequest();
        req.setTitle("Dupe");
        req.setDataSteward("a@b.com");
        req.setMetadata(meta);
        MockMultipartFile file = new MockMultipartFile("file", "f.csv", "text/csv", "x".getBytes());
        when(datasetRepository.existsByTitleIgnoreCase("Dupe")).thenReturn(true);
        assertThatThrownBy(() -> service.register(req, file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be unique");
    }

    @Test
    void getDetail_rehydratesFieldsFromJson() throws Exception {
        DatasetRecord r = new DatasetRecord();
        r.setId(3L);
        r.setTitle("T");
        r.setFieldsJson(objectMapper.writeValueAsString(List.of(field("a", false, false, List.of("x")))));
        when(datasetRepository.findById(3L)).thenReturn(Optional.of(r));

        CatalogueDetail d = service.getDetail(3L);

        assertThat(d.fields()).hasSize(1);
        assertThat(d.fields().get(0).getFieldName()).isEqualTo("a");
    }

    @Test
    void getDetail_missing_throwsNotFound() {
        when(datasetRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getDetail(99L))
                .isInstanceOf(DatasetNotFoundException.class);
    }

    @Test
    void delete_removesFileThenRecord() {
        when(datasetRepository.existsById(4L)).thenReturn(true);
        service.delete(4L);
        verify(fileStorage).delete(4L);
        verify(datasetRepository).deleteById(4L);
    }

    @Test
    void delete_missing_throwsNotFound_andDoesNotTouchStorage() {
        when(datasetRepository.existsById(5L)).thenReturn(false);
        assertThatThrownBy(() -> service.delete(5L)).isInstanceOf(DatasetNotFoundException.class);
        verify(fileStorage, never()).delete(anyLong());
    }

    @Test
    void update_changesTitleAndReplacesFields_recomputingCounts() throws Exception {
        DatasetRecord r = new DatasetRecord();
        r.setId(8L);
        r.setTitle("old");
        r.setTotalRecords(10);
        r.setFieldsJson(objectMapper.writeValueAsString(List.of(field("a", false, false, List.of("x")))));
        when(datasetRepository.findById(8L)).thenReturn(Optional.of(r));
        when(datasetRepository.save(any(DatasetRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        CatalogueUpdateRequest req = new CatalogueUpdateRequest();
        req.setTitle("new");
        req.setFields(List.of(
                field("a", true, false, List.of("pci")),
                field("b", false, false, List.of("y"))));

        CatalogueDetail d = service.update(8L, req);

        assertThat(d.title()).isEqualTo("new");
        assertThat(d.totalFields()).isEqualTo(2);
        assertThat(d.pciFieldsCount()).isEqualTo(1);
        assertThat(d.totalRecords()).isEqualTo(10); // unchanged
    }
}
