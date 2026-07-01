package com.mes.skills;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Produces format-preserving synthetic stand-ins for real cell values so that the
 * structural SHAPE of banking data (digit/letter classes, separators, length) can
 * be sent to the Claude API for type / PII / PCI inference WITHOUT ever
 * transmitting the real values.
 *
 * <p>Digits map to random digits, ASCII letters to random letters (case
 * preserved); every other character (separators, punctuation, symbols such as
 * {@code '@'}, {@code '.'}, {@code '-'}, {@code '/'}, whitespace) is kept verbatim,
 * so emails, dates, decimals and account-number formats retain their recognizable
 * shape while leaking no actual data.</p>
 */
@Component
public class SampleSanitizer {

    /** Returns a synthetic value with the same character-class shape as {@code value}. */
    public String synthesize(String value) {
        if (value == null) {
            return null;
        }
        StringBuilder out = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c >= '0' && c <= '9') {
                out.append((char) ('0' + ThreadLocalRandom.current().nextInt(10)));
            } else if (c >= 'a' && c <= 'z') {
                out.append((char) ('a' + ThreadLocalRandom.current().nextInt(26)));
            } else if (c >= 'A' && c <= 'Z') {
                out.append((char) ('A' + ThreadLocalRandom.current().nextInt(26)));
            } else {
                out.append(c); // keep separators / punctuation / whitespace verbatim
            }
        }
        return out.toString();
    }

    /** Synthesizes every value in the list (nulls preserved, never the real values). */
    public List<String> synthesize(List<String> values) {
        if (values == null) {
            return new ArrayList<>();
        }
        List<String> out = new ArrayList<>(values.size());
        for (String v : values) {
            out.add(synthesize(v));
        }
        return out;
    }
}
