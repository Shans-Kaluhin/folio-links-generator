package org.folio.model;

import lombok.Getter;

@Getter
public class SimpleMarcField extends MarcField {
  private final String value;

  public SimpleMarcField(String tag, String value) {
    super(tag);
    this.value = value;
  }
}
