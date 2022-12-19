package org.folio.model.integration;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.folio.model.MarcField;

import java.util.List;

@Getter
@AllArgsConstructor
public class ExternalIdsHolder {
    private String id;
    private String hrid;
    private List<MarcField> fields;

    public MarcField getField(String tag) {
        return fields.stream()
                .filter(b -> b.getTag().equals(tag))
                .findFirst()
                .orElse(null);
    }

    public String getTitle() {
        return fields.stream()
                .filter(b -> b.getTag().equals("245"))
                .map(b -> b.getSubfields().get('a'))
                .findFirst()
                .orElse(null);
    }

    public String getNaturalId() {
        return fields.stream()
                .filter(b -> b.getTag().equals("010"))
                .map(b -> b.getSubfields().get('a'))
                .findFirst()
                .orElse(hrid)
                .replaceAll("\\s", "");
    }
}
