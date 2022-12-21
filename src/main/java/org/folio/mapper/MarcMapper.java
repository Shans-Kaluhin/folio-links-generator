package org.folio.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import org.folio.model.MarcField;
import org.folio.processor.rule.DataSource;
import org.folio.reader.values.CompositeValue;
import org.folio.reader.values.StringValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MarcMapper {

    public static CompositeValue mapToCompositeValue(MarcField marcField) {
        CompositeValue compositeValue = new CompositeValue();
        List<StringValue> values = new ArrayList<>();

        values.addAll(mapInd(marcField));
        values.addAll(mapSubfields(marcField));

        compositeValue.addEntry(values);
        return compositeValue;
    }

    public static List<StringValue> mapInd(MarcField marcField) {
        var d1 = new DataSource();
        d1.setIndicator("1");
        var ind1 = new StringValue(marcField.getInd1(), d1, null);

        var d2 = new DataSource();
        d2.setIndicator("2");
        var ind2 = new StringValue(marcField.getInd2(), d2, null);

        return List.of(ind1, ind2);
    }

    public static List<StringValue> mapSubfields(MarcField marcField) {
        var subfields = new ArrayList<StringValue>();

        marcField.getSubfields().forEach((subfield, value) -> {
            var dataSource = new DataSource();
            dataSource.setSubfield(subfield.toString());
            var stringValue = new StringValue(value, dataSource, null);
            subfields.add(stringValue);
        });

        return subfields;
    }

    public static MarcField mapToMarcBibField(String field, JsonNode value) {
        if (value.getNodeType().equals(JsonNodeType.STRING)) {
            return null;
        }
        Map<Character, String> bibSubfields = new HashMap<>();
        var authoritySubfields = value.get("subfields");
        var ind1 = value.get("ind1").asText();
        var ind2 = value.get("ind2").asText();

        for (var authoritySubfield : authoritySubfields) {
            authoritySubfield.fields().forEachRemaining(e -> {
                Character subfield = e.getKey().charAt(0);
                if (!(subfield.equals('0') || subfield.equals('9'))) {
                    bibSubfields.put(subfield, e.getValue().asText());
                }
            });
        }

        return new MarcField(field, ind1, ind2, bibSubfields);
    }
}
