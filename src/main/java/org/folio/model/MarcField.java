package org.folio.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;

@Getter
@AllArgsConstructor
public class MarcField {
    String tag;
    String ind1;
    String ind2;
    Map<Character, String> subfields;

    public MarcField copyWithTag(String newTag){
        return new MarcField(newTag, ind1, ind2, subfields);
    }
}
