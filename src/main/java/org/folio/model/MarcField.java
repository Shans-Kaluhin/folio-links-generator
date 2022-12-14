package org.folio.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class MarcField {
    String tag;
    String ind1;
    String ind2;
    Map<Character, String> subfields;
}
