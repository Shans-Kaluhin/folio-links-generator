package org.folio.model;

import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MarcField {
  private String tag;
  private String ind1;
  private String ind2;
  private Map<Character, String> subfields;

  public MarcField(String tag) {
    this.tag = tag;
  }

  public MarcField copyWithTag(String newTag) {
    return new MarcField(newTag, ind1, ind2, new HashMap<>(subfields));
  }
}
