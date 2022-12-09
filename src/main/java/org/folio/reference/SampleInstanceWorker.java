package org.folio.reference;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.folio.model.Configuration;
import org.folio.model.JsonBibToPopulate;

import java.util.List;

import static org.folio.util.FileWorker.getJsonObject;

public class SampleInstanceWorker {

    public static List<JsonBibToPopulate> populateInstances(List<Configuration.BibsConfig> bibsConfig) {
        return bibsConfig.stream().map(SampleInstanceWorker::createSample).toList();
    }

    private static JsonBibToPopulate createSample(Configuration.BibsConfig bibsConfig) {
        var sampleInstance = getJsonObject("sample/sampleInstance.json");
        var sampleData = getJsonObject("sample/sampleFieldsData.json");

        bibsConfig.linkingFields()
                .forEach(field -> populateFields(sampleInstance, sampleData, field));

        return new JsonBibToPopulate(bibsConfig.totalBibs(), sampleInstance);
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
}
