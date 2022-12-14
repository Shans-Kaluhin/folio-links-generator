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

    public String getTitle() {
        var title = fields.stream()
                .filter(b -> b.getTag().equals("245"))
                .map(b -> b.getSubfields().get('a')).findFirst();
        return title.orElse(null);
    }
}
