package org.folio.reference;

import static org.folio.util.FileWorker.getJsonObject;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.folio.model.Configuration;

public class SampleInstanceWorker {

  public static List<JsonNode> populateInstances(List<Configuration.BibsConfig> bibsConfig) {
    return bibsConfig.stream()
      .flatMap(SampleInstanceWorker::createSamples)
      .toList();
  }

  private static Stream<JsonNode> createSamples(Configuration.BibsConfig bibsConfig) {
    var sampleInstance = getJsonObject("sample/sampleInstance.json");
    var sampleData = getJsonObject("sample/sampleFieldsData.json");

    bibsConfig.linkingFields()
      .forEach(field -> populateFields(sampleInstance, sampleData, field));

    return populateTitles(sampleInstance, bibsConfig);
  }

  private static void populateFields(ObjectNode json, ObjectNode sampleData, String field) {
    var data = sampleData.get(field);
    if (data != null) {
      var instance = (ObjectNode) json.get("instance");
      var name = data.fieldNames().next();
      if (instance.has(name)) {
        ((ArrayNode) instance.get(name)).addAll((ArrayNode) data.get(name));
      } else {
        instance.setAll((ObjectNode) data);
      }
    }
  }

  private static Stream<JsonNode> populateTitles(JsonNode sample, Configuration.BibsConfig bibsConfig) {
    var samples = new ArrayList<JsonNode>();
    for (int i = 0; i < bibsConfig.totalBibs(); i++) {
      var sampleCopy = sample.deepCopy();
      var title = String.format("Generated bib #%s. Linked: %s", i, bibsConfig.linkingFields());

      ((ObjectNode) sampleCopy.get("instance")).put("title", title);

      samples.add(sampleCopy);
    }
    return samples.stream();
  }
}
