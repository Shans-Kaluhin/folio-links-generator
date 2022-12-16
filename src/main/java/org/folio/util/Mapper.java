package org.folio.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import lombok.SneakyThrows;
import org.folio.model.LinkingRule;
import org.folio.model.MarcField;
import org.folio.model.RecordType;
import org.folio.model.integration.ExternalIdsHolder;
import org.folio.model.integration.UploadDefinition;
import org.folio.processor.rule.DataSource;
import org.folio.reader.values.CompositeValue;
import org.folio.reader.values.StringValue;

import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Mapper {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @SneakyThrows
    public static JsonNode mapResponseToJson(HttpResponse<String> response) {
        return OBJECT_MAPPER.readTree(response.body());
    }

    @SneakyThrows
    public static HashMap<String, LinkingRule> mapRules(String json) {
        var rules = new HashMap<String, LinkingRule>();
        var jsonNode = OBJECT_MAPPER.readTree(json);

        for (JsonNode rule : jsonNode) {
            var bibField = rule.get("bibField").asText();
            var authorityField = rule.get("authorityField").asText();
            var subfields = new ArrayList<Character>();
            rule.get("authoritySubfields").elements().forEachRemaining(subfield -> subfields.add(subfield.asText().charAt(0)));
            rules.put(bibField, new LinkingRule(bibField, authorityField, subfields));
        }

        return rules;
    }

    @SneakyThrows
    public static UploadDefinition mapUploadDefinition(String json, Path filePath) {
        var jsonBody = OBJECT_MAPPER.readTree(json);

        var fileDefinitions = jsonBody.findValue("fileDefinitions");
        var uploadDefinitionId = fileDefinitions.findValue("uploadDefinitionId").asText();
        var jobExecutionId = fileDefinitions.findValue("jobExecutionId").asText();
        var fileId = fileDefinitions.findValue("id").asText();

        return new UploadDefinition(uploadDefinitionId, jobExecutionId, fileId, filePath);
    }

    @SneakyThrows
    public static List<ExternalIdsHolder> mapRecordsToExternalIds(String json, RecordType recordType) {
        var externalIds = new ArrayList<ExternalIdsHolder>();
        var jsonBody = OBJECT_MAPPER.readTree(json);

        var records = jsonBody.findValue("records");
        for (JsonNode record : records) {
            externalIds.add(mapRecordToExternalIds(record, recordType));
        }

        return externalIds;
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
                bibSubfields.put(subfield, e.getValue().asText());
            });
        }

        return new MarcField(field, ind1, ind2, bibSubfields);
    }

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

    private static ExternalIdsHolder mapRecordToExternalIds(JsonNode jsonNode, RecordType recordType) {
        var holder = jsonNode.get("externalIdsHolder");
        var id = holder.get(recordType.getExternalIdName() + "Id").asText();
        var hrid = holder.get(recordType.getExternalIdName() + "Hrid").asText();

        return new ExternalIdsHolder(id, hrid, mapRecordFields(jsonNode));
    }

    private static List<MarcField> mapRecordFields(JsonNode jsonNode) {
        var mappedFields = new ArrayList<MarcField>();
        var fields = (ArrayNode) jsonNode.findValue("fields");

        for (var field : fields) {
            field.fields().forEachRemaining(e -> {
                var marcField = mapToMarcBibField(e.getKey(), e.getValue());
                if (marcField != null) mappedFields.add(marcField);
            });
        }

        return mappedFields;
    }
}
