package org.folio.service;

import org.folio.model.Configuration;
import org.folio.model.MarcField;
import org.folio.model.integration.ExternalIdsHolder;
import org.folio.processor.translations.Translation;
import org.folio.writer.RecordWriter;
import org.folio.writer.impl.MarcRecordWriter;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.folio.util.FileWorker.writeFile;
import static org.folio.util.Mapper.mapToCompositeValue;

public class MarcConverterService {
    private static final String GENERATED_BIBS_FILE = "SCRIPT_auto_generated_bibs_for_linking_tests.mrc";
    private final LinkingRuleService linkingRuleService;
    private final Configuration configuration;

    public MarcConverterService(Configuration configuration, LinkingRuleService linkingRuleService) {
        this.configuration = configuration;
        this.linkingRuleService = linkingRuleService;
    }

    public Path generateBibs(List<ExternalIdsHolder> authorities) {
        var mrcFile = new ArrayList<String>();
        var authoritiesFields = mergeAuthoritiesFields(authorities);

        for (var config : configuration.getMarcBibs()) {
            for (int i = 0; i < config.totalBibs(); i++) {
                var marcBibFields = mapFieldsAndFilterByRules(authoritiesFields, config.linkingFields());
                marcBibFields.add(getNameMarcField(config.linkingFields(), i));
                marcBibFields.add(getInstanceTypeField());

                var instance = createInstance(marcBibFields);
                mrcFile.add(instance);
            }
        }

        return writeFile(GENERATED_BIBS_FILE, mrcFile);
    }

    private String createInstance(List<MarcField> bibFields) {
        RecordWriter recordWriter = new MarcRecordWriter();
        recordWriter.writeLeader(getLeaderTranslation());
        bibFields.forEach(bibField ->
                recordWriter.writeField(bibField.getTag(), mapToCompositeValue(bibField)));

        return recordWriter.getResult();
    }

    private List<MarcField> mapFieldsAndFilterByRules(Map<String, List<MarcField>> authorityFields, List<String> linkingFields) {
        var marcFields = new ArrayList<MarcField>();
        var fieldsToRemove = new ArrayList<String>();

        for (var requiredField : linkingFields) {
            var bibFields = linkingRuleService.constructBibFields(requiredField, authorityFields);
            if (bibFields.isEmpty()) {
                fieldsToRemove.add(requiredField);
            }
            marcFields.addAll(bibFields);
        }
        linkingFields.removeAll(fieldsToRemove);

        return marcFields;
    }

    private Map<String, List<MarcField>> mergeAuthoritiesFields(List<ExternalIdsHolder> authorities) {
        var merged = new HashMap<String, List<MarcField>>();
        authorities.forEach(linkingRuleService::setLinkingSubfields);
        authorities.stream()
                .flatMap(authority -> authority.getFields().stream())
                .forEach(field -> {
                    if (merged.containsKey(field.getTag())) {
                        merged.get(field.getTag()).add(field);
                    } else {
                        var list = new ArrayList<MarcField>();
                        list.add(field);
                        merged.put(field.getTag(), list);
                    }
                });
        return merged;
    }

    private Translation getLeaderTranslation() {
        Translation leaderTranslation = new Translation();
        leaderTranslation.setFunction("set_17-19_positions");
        leaderTranslation.setParameters(Map.of(
                "position17", "3",
                "position18", "c",
                "position19", " "));
        return leaderTranslation;
    }

    private MarcField getNameMarcField(List<String> fields, int i) {
        var name = String.format("Generated bib #%s. Linked: %s", i, fields);
        return new MarcField("245", "0", "0", Map.of('a', name));
    }

    private MarcField getInstanceTypeField() {
        return new MarcField("336", " ", " ", Map.of('a', "still image"));
    }
}
