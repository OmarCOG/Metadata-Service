package com.mes.skills;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * The single source of truth for the sensitive-data classification taxonomy.
 *
 * <p>Drives three things from one definition: the Claude prompt rubric
 * ({@link #promptRubric()}), the local field-name fallback ({@link #detect(String)}),
 * and the user-facing Help Guide (served by the taxonomy endpoint). Categories are
 * <b>multi-label</b> — a single column may belong to several (e.g. a card number is
 * PCI <i>and</i> PII <i>and</i> NPI).
 *
 * <p>Curated from official standards: NIST SP 800-122 (PII), GLBA (NPI),
 * PCI-DSS (PCI). Standards are stable, so this lives in code rather than a DB.
 */
public final class SensitiveDataTaxonomy {

    /** One classification category. {@code namePatterns} are case-insensitive regexes. */
    public record Category(
            String key,
            String label,
            String definition,
            List<String> examples,
            String standard,
            List<Pattern> namePatterns) {
    }

    public static final List<Category> CATEGORIES = List.of(
            new Category(
                    "pii", "PII",
                    "Personally Identifiable Information — data that directly identifies a person.",
                    List.of("full name", "email", "phone", "home address", "SSN",
                            "date of birth", "passport / driver's-license number", "biometric data"),
                    "NIST SP 800-122",
                    compile("first.?name", "last.?name", "full.?name", "cust.*name", "emp.*name",
                            "e-?mail", "phone", "mobile", "(^|_)address", "ssn", "social.?security",
                            "\\bdob\\b", "birth.?date", "date.?of.?birth", "passport", "driver.?licen[cs]e",
                            "nationality", "gender")),
            new Category(
                    "npi", "NPI",
                    "Nonpublic Personal Information (GLBA) — nonpublic financial info about a person.",
                    List.of("account number", "balance", "transaction / payment history",
                            "loan or credit info", "income", "routing / IBAN"),
                    "GLBA",
                    compile("account", "\\bacct\\b", "balance", "payment", "transaction", "\\btxn\\b",
                            "loan", "credit", "debit", "income", "salary", "iban", "routing", "swift",
                            "deposit", "withdrawal")),
            new Category(
                    "pci", "PCI",
                    "Payment card / cardholder data governed by PCI-DSS.",
                    List.of("card number (PAN)", "CVV / CVC", "card expiry", "cardholder name", "PIN", "track data"),
                    "PCI-DSS",
                    compile("card.?number", "\\bpan\\b", "cardholder", "\\bcvv\\b", "\\bcvc\\b", "cvv2",
                            "expir", "exp.?date", "\\bpin\\b", "track.?data", "credit.?card", "debit.?card"))
    );

    private SensitiveDataTaxonomy() {
    }

    private static List<Pattern> compile(String... regexes) {
        return List.of(regexes).stream()
                .map(r -> Pattern.compile(r, Pattern.CASE_INSENSITIVE))
                .toList();
    }

    /**
     * Best-effort name-based detection used by the local fallback (when Claude is
     * unavailable). Returns {key -> matched?} for all categories.
     */
    public static Map<String, Boolean> detect(String fieldName) {
        String name = fieldName == null ? "" : fieldName.toLowerCase().trim();
        Map<String, Boolean> result = new LinkedHashMap<>();
        for (Category c : CATEGORIES) {
            boolean hit = c.namePatterns().stream().anyMatch(p -> p.matcher(name).find());
            result.put(c.key(), hit);
        }
        return result;
    }

    /** Renders the category rubric injected into the Claude system prompt. */
    public static String promptRubric() {
        StringBuilder sb = new StringBuilder();
        for (Category c : CATEGORIES) {
            sb.append("- ").append(c.key()).append("Data: true if the field holds ")
              .append(c.label()).append(" — ").append(c.definition())
              .append(" Examples: ").append(String.join(", ", c.examples())).append(".\n");
        }
        return sb.toString();
    }
}
