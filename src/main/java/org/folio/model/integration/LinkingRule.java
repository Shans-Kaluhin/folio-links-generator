package org.folio.model.integration;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class LinkingRule {
    private String bibField;
    private String authorityField;
    private List<Character> subfields;
    private List<Validation> validation;
    private List<SubfieldModification> subfieldModifications;

    public record SubfieldModification(Character source, Character target) {}

    public record Validation(Character subfield, boolean existence) {}
}
