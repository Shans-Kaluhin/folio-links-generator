package org.folio.model.integration;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LinkingRule {
  private int id;
  private String bibField;
  private String authorityField;
  private List<Character> subfields;
  private List<Validation> validation;
  private List<SubfieldModification> subfieldModifications;

  public record SubfieldModification(Character source, Character target) {
  }

  public record Validation(Character subfield, boolean existence) {
  }
}
