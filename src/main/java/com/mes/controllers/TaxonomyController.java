package com.mes.controllers;

import com.mes.skills.SensitiveDataTaxonomy;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Serves the sensitive-data classification taxonomy for the UI Help Guide, so the
 * human-readable category definitions are defined once (in {@link SensitiveDataTaxonomy})
 * and shared by the classifier and the frontend. Internal regex patterns are not exposed.
 */
@RestController
@RequestMapping("/api/taxonomy")
public class TaxonomyController {

    /** Public, regex-free view of a category for the Help Guide. */
    public record CategoryView(String key, String label, String definition,
                               List<String> examples, String standard) {
    }

    @GetMapping
    public ResponseEntity<List<CategoryView>> categories() {
        List<CategoryView> views = SensitiveDataTaxonomy.CATEGORIES.stream()
                .map(c -> new CategoryView(c.key(), c.label(), c.definition(), c.examples(), c.standard()))
                .toList();
        return ResponseEntity.ok(views);
    }
}
