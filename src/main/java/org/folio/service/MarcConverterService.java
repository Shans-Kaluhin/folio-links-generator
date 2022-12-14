package org.folio.service;

import org.folio.client.EntitiesLinksClient;
import org.folio.model.Configuration;
import org.folio.model.MarcField;
import org.folio.model.integration.ExternalIdsHolder;
import org.folio.processor.translations.Translation;
import org.folio.writer.RecordWriter;
import org.folio.writer.impl.MarcRecordWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.folio.util.FileWorker.writeFile;
import static org.folio.util.Mapper.mapToCompositeValue;

public class MarcConverterService {
    private static final Logger LOG = LoggerFactory.getLogger(MarcConverterService.class);
    private static final String GENERATED_BIBS_FILE = "SCRIPT_auto_generated_bibs_for_linking_tests.mrc";
    private static final String ID_LOC_GOV = "https://id.loc.gov/authorities/names/";
    private final EntitiesLinksClient linksClient;
    private final Configuration configuration;

    public MarcConverterService(Configuration configuration, EntitiesLinksClient linksClient) {
        this.configuration = configuration;
        this.linksClient = linksClient;
    }

    public Path generateBibs(List<ExternalIdsHolder> authorities) {
        var mrcFile = new ArrayList<String>();
        authorities.forEach(this::setLinkingSubfields);
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
        List<MarcField> marcFields = new ArrayList<>();

        for (var requiredField : linkingFields) {
            var authorityField = authorityFields.get(requiredField);
            var ruleSubfields = linksClient.getLinkedRules().get(requiredField);

            if (authorityField != null) {
                if (ruleSubfields != null) {
                    marcFields.addAll(authorityField);
                } else {
                    LOG.info("Field {} was skipped as it does not comply with linked rules", requiredField);
                }
            }
        }

        return marcFields;
    }

    private Map<String, List<MarcField>> mergeAuthoritiesFields(List<ExternalIdsHolder> authorities) {
        var merged = new HashMap<String, List<MarcField>>();
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

    private void setLinkingSubfields(ExternalIdsHolder authority) {
        var authorityId = authority.getId();
        var naturalId = ID_LOC_GOV + authority.getNaturalId();

        authority.getFields().forEach(f -> f.getSubfields()
                .putAll(Map.of('0', naturalId,
                               '9', authorityId)));
    }

    private MarcField getNameMarcField(List<String> fields, int i) {
        var name = String.format("Generated bib #%s. Linked: %s", i, fields);
        return new MarcField("245", "0", "0", Map.of('a', name));
    }

    private MarcField getInstanceTypeField() {
        return new MarcField("336", " ", " ", Map.of('a', "still image"));
    }
}
