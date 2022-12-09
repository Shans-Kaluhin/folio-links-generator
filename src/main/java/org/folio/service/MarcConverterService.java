package org.folio.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.folio.model.JsonBibToPopulate;
import org.folio.reference.ReferenceTranslationHolder;
import org.folio.model.Configuration;
import org.folio.model.integration.ExternalIdsHolder;
import org.folio.processor.RuleProcessor;
import org.folio.processor.referencedata.ReferenceDataWrapperImpl;
import org.folio.processor.rule.Rule;
import org.folio.reader.EntityReader;
import org.folio.reader.JPathSyntaxEntityReader;
import org.folio.writer.RecordWriter;
import org.folio.writer.impl.MarcRecordWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static org.folio.FolioLinksGeneratorApp.exitWithError;
import static org.folio.reference.SampleInstanceWorker.populateInstances;
import static org.folio.util.FileWorker.getMappedResourceFile;
import static org.folio.util.FileWorker.writeFile;

public class MarcConverterService {
    private static final Logger LOG = LoggerFactory.getLogger(MarcConverterService.class);
    private static final String LINKED_INSTANCES_FILE = "linkedInstances.txt";
    private static final String GENERATED_BIBS_FILE = "SCRIPT_auto_generated_bibs_for_linking_tests.mrc";
    private final Configuration configuration;

    public MarcConverterService(Configuration configuration) {
        this.configuration = configuration;
    }

    public void writeLinkedIds(List<ExternalIdsHolder> instances) {
        var mrcInstances = instances.stream().map(ExternalIdsHolder::getId).toList();
        LOG.info("Write file with linked instances for each authority");
        writeFile(LINKED_INSTANCES_FILE, mrcInstances);
    }

    public Path generateBibs() {
        LOG.info("Generating bib mrc file...");

        var rules = retrieveRules();
        var reference = new ReferenceDataWrapperImpl(new HashMap<>());
        var sampleInstances = populateInstances(configuration.getMarcBibs());

        var mrcFile = sampleInstances.stream()
                .flatMap(sampleBib -> mapInstances(rules, reference, sampleBib).stream())
                .toList();

        return writeFile(GENERATED_BIBS_FILE, mrcFile);
    }

    private List<String> mapInstances(List<Rule> rules, ReferenceDataWrapperImpl reference, JsonBibToPopulate sampleBib) {
       var mrcInstance = mapInstance(rules, reference, sampleBib.getSample());
       return duplicateStrings(mrcInstance, sampleBib.getTotalCount());
    }

    private String mapInstance(List<Rule> rules, ReferenceDataWrapperImpl reference, JsonNode instance) {
        EntityReader entityReader = new JPathSyntaxEntityReader(instance.toString());
        RecordWriter recordWriter = new MarcRecordWriter();
        RuleProcessor ruleProcessor = new RuleProcessor(ReferenceTranslationHolder.SET_VALUE);
        return ruleProcessor.process(entityReader, recordWriter, reference, rules,
                (a) -> exitWithError("Failed to map mrc record: " + a)
        );
    }

    private List<String> duplicateStrings(String str, int times) {
        var strings = new ArrayList<String>();
        for (int i = 0; i < times; i++) {
            strings.add(str);
        }
        return strings;
    }

    private List<Rule> retrieveRules() {
        return Arrays.asList(getMappedResourceFile("rulesDefault.json", Rule[].class));
    }
}
