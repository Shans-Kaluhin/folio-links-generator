package org.folio.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.SneakyThrows;
import org.folio.model.RecordType;
import org.folio.model.integration.ExternalIdsHolder;
import org.folio.model.integration.UploadDefinition;

import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Mapper {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @SneakyThrows
    public static JsonNode mapResponseToJson(HttpResponse<String> response) {
        return OBJECT_MAPPER.readTree(response.body());
    }

    @SneakyThrows
    public static HashMap<String, List<Character>> mapRules(String json) {
        var rules = new HashMap<String, List<Character>>();
        var jsonNode = OBJECT_MAPPER.readTree(json);

        for (JsonNode rule : jsonNode) {
            var bibField = rule.get("bibField").asText();
            var subfields = new ArrayList<Character>();
            rule.get("authoritySubfields").elements().forEachRemaining(subfield -> subfields.add(subfield.asText().charAt(0)));
            rules.put(bibField, subfields);
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

    private static ExternalIdsHolder mapRecordToExternalIds(JsonNode jsonNode, RecordType recordType) {
        var holder = jsonNode.get("externalIdsHolder");
        var id = holder.get(recordType.getExternalIdName() + "Id").asText();
        var hrid = holder.get(recordType.getExternalIdName() + "Hrid").asText();

        var fields = (ArrayNode) jsonNode.findValue("fields");

        return new ExternalIdsHolder(id, hrid, fields);
    }
}
