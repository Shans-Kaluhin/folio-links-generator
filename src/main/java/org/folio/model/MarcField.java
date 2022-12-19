package org.folio.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
@AllArgsConstructor
public class MarcField {
    private String tag;
    private String ind1;
    private String ind2;
    private Map<Character, String> subfields;

    public MarcField copyWithTag(String newTag){
        return new MarcField(newTag, ind1, ind2, new HashMap<>(subfields));
    }
}
