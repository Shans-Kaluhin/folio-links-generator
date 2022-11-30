package org.folio.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.folio.model.Configuration;
import org.folio.processor.RuleProcessor;
import org.folio.processor.referencedata.JsonObjectWrapper;
import org.folio.processor.referencedata.ReferenceDataWrapperImpl;
import org.folio.processor.rule.Rule;
import org.folio.reader.EntityReader;
import org.folio.reader.JPathSyntaxEntityReader;
import org.folio.writer.RecordWriter;
import org.folio.writer.impl.MarcRecordWriter;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.folio.FolioLinksGeneratorApp.exitWithError;
import static org.folio.util.FileWorker.getJsonObject;
import static org.folio.util.FileWorker.getMappedResourceFile;
import static org.folio.util.FileWorker.writeFile;

public class MarcConverterService {

    private static final String GENERATED_BIBS_FILE = "generatedBibs.mrc";
    private final Configuration configuration;

    public MarcConverterService(Configuration configuration) {
        this.configuration = configuration;
    }

    public Path generateBibs() {
        var rules = retrieveRules();
        var reference = retrieveReference();
        var totalBibs = configuration.getMarcBibs().stream()
                .mapToInt(Configuration.BibsConfig::totalBibs)
                .sum();

        var mrcInstances = generateBibs(rules, reference, totalBibs);

        return writeFile(GENERATED_BIBS_FILE, mrcInstances);
    }

    private List<String> generateBibs(List<Rule> rules, ReferenceDataWrapperImpl reference, int amount) {
        ObjectNode jsonSample = retrieveSampleInstance();

        List<String> instances = new ArrayList<>();
        for (int i = 0; i < amount; i++) {
            var json = jsonSample.deepCopy();
            var jsonInstance = (ObjectNode) json.get("instance");
            jsonInstance.put("title", "Generated bib " + i);

            var instance = mapInstance(rules, reference, json);
            instances.add(instance);
        }
        return instances;
    }

    private String mapInstance(List<Rule> rules, ReferenceDataWrapperImpl reference, JsonNode instance) {
        EntityReader entityReader = new JPathSyntaxEntityReader(instance.toString());
        RecordWriter recordWriter = new MarcRecordWriter();
        RuleProcessor ruleProcessor = new RuleProcessor();
        return ruleProcessor.process(entityReader, recordWriter, reference, rules,
                (a) -> exitWithError("Failed to map mrc record: " + a)
        );
    }

    private ReferenceDataWrapperImpl retrieveReference() {
        var reference = new HashMap<String, Map<String, JsonObjectWrapper>>();

        var instanceTypeMap = new HashMap<String, JsonObjectWrapper>();
        var instanceTypeJson = new JsonObjectWrapper(retrieveInstanceType());
        instanceTypeMap.put(instanceTypeJson.getMap().get("id").toString(), instanceTypeJson);

        reference.put("instanceTypes", instanceTypeMap);
        return new ReferenceDataWrapperImpl(reference);
    }

    private HashMap retrieveInstanceType() {
        return getMappedResourceFile("instanceType.json", HashMap.class);
    }

    private List<Rule> retrieveRules() {
        var rules = getMappedResourceFile("rulesDefault.json", Rule[].class);
        return Arrays.asList(rules);
    }

    private ObjectNode retrieveSampleInstance() {
        return getJsonObject("sampleInstance.json");
    }
}
