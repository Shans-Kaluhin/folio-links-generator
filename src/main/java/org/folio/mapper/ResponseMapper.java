package org.folio.mapper;

import static org.folio.mapper.MarcMapper.mapToMarcBibField;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import lombok.SneakyThrows;
import org.folio.model.MarcField;
import org.folio.model.RecordType;
import org.folio.model.integration.ExternalIdsHolder;
import org.folio.model.integration.JobExecution;
import org.folio.model.integration.JobStatus;
import org.folio.model.integration.LinkingRule;
import org.folio.model.integration.UploadDefinition;

public class ResponseMapper {
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  @SneakyThrows
  public static JsonNode mapResponseToJson(HttpResponse<String> response) {
    return OBJECT_MAPPER.readTree(response.body());
  }

  @SneakyThrows
  public static JobExecution mapToJobExecution(String json, String jobId) {
    var jobs = OBJECT_MAPPER.readTree(json).get("jobExecutions");

    for (var job : jobs) {
      var id = job.get("id").asText();

      if (jobId.equals(id)) {
        var progress = job.get("progress");
        var status = job.get("status").asText();
        var uiStatus = job.get("uiStatus").asText();
        var current = progress.get("current").asInt();
        var total = progress.get("total").asInt();

        return new JobExecution(status, uiStatus, current, total);
      }
    }
    return new JobExecution(JobStatus.ERROR.name(), "JOB_NOT_FOUND", 0, 0);
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

    return new ExternalIdsHolder(id, hrid, mapRecordFields(jsonNode));
  }

  private static List<MarcField> mapRecordFields(JsonNode jsonNode) {
    var mappedFields = new ArrayList<MarcField>();
    var fields = (ArrayNode) jsonNode.findValue("fields");

    for (var field : fields) {
      field.fields().forEachRemaining(e -> {
        var marcField = mapToMarcBibField(e.getKey(), e.getValue());
        if (marcField != null) {
          mappedFields.add(marcField);
        }
      });
    }

    return mappedFields;
  }

  @SneakyThrows
  public static HashMap<String, List<LinkingRule>> mapRules(String json, RecordType keyType) {
    var rules = new HashMap<String, List<LinkingRule>>();
    var jsonNode = OBJECT_MAPPER.readTree(json);

    for (JsonNode rule : jsonNode) {
      var id = rule.get("id").asInt();
      var bibField = rule.get("bibField").asText();
      var authorityField = rule.get("authorityField").asText();
      var keyTag = keyType.equals(RecordType.MARC_BIB) ? bibField : authorityField;

      var subfields = mapSubfields(rule);
      var validation = mapValidation(rule);
      var modifications = mapModifications(rule);
      var linkingRule = new LinkingRule(id, bibField, authorityField, subfields, validation, modifications);

      var existRules = rules.get(keyTag);
      if (existRules != null) {
        existRules.add(linkingRule);
      } else {
        var list = new ArrayList<LinkingRule>();
        list.add(linkingRule);
        rules.put(keyTag, list);
      }
    }

    return rules;
  }

  private static List<Character> mapSubfields(JsonNode json) {
    var subfields = new ArrayList<Character>();
    var jsonSubfields = json.get("authoritySubfields");

    if (jsonSubfields != null) {
      jsonSubfields.elements()
        .forEachRemaining(subfield -> subfields.add(subfield.asText().charAt(0)));
    }
    return subfields;
  }

  private static List<LinkingRule.SubfieldModification> mapModifications(JsonNode json) {
    var modifications = new ArrayList<LinkingRule.SubfieldModification>();
    var jsonModifications = json.get("subfieldModifications");

    if (jsonModifications != null) {
      jsonModifications.elements()
        .forEachRemaining(subfield -> modifications.add(new LinkingRule.SubfieldModification(
          subfield.get("source").asText().charAt(0), subfield.get("target").asText().charAt(0))));
    }
    return modifications;
  }

  private static List<LinkingRule.Validation> mapValidation(JsonNode json) {
    var validation = new ArrayList<LinkingRule.Validation>();
    var jsonValidation = json.get("validation");

    if (jsonValidation != null) {
      jsonValidation.get("existence").elements()
        .forEachRemaining(existence -> existence.fields().forEachRemaining(sub ->
          validation.add(new LinkingRule.Validation(sub.getKey().charAt(0), sub.getValue().asBoolean()))
        ));
    }
    return validation;
  }
}
