package org.folio.model.integration;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.folio.model.MarcField;

@Getter
@AllArgsConstructor
public class ExternalIdsHolder {
  private String id;
  private String hrid;
  private List<MarcField> fields;

  public String getNaturalId() {
    return fields.stream()
      .filter(b -> b.getTag().equals("010"))
      .map(b -> b.getSubfields().get('a'))
      .findFirst()
      .orElse(hrid)
      .replaceAll("\\s", "");
  }
}
