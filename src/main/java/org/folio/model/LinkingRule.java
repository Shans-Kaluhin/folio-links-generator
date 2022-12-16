package org.folio.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class LinkingRule {
    String bibField;
    String authorityField;
    List<Character> subfields;
}
