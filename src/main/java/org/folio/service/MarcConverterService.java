package org.folio.service;

import static org.apache.commons.lang3.StringUtils.repeat;
import static org.folio.mapper.MarcMapper.mapToCompositeValue;
import static org.folio.mapper.MarcMapper.mapToStringValue;
import static org.folio.util.FileWorker.writeFile;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.folio.model.Configuration;
import org.folio.model.MarcField;
import org.folio.model.SimpleMarcField;
import org.folio.model.integration.ExternalIdsHolder;
import org.folio.processor.translations.Translation;
import org.folio.writer.RecordWriter;
import org.folio.writer.impl.MarcRecordWriter;

@Slf4j
public class MarcConverterService {
  private static final String GENERATED_BIBS_FILE = "SCRIPT_auto_generated_bibs_for_linking_tests.mrc";
  private final LinkingRuleService linkingRuleService;
  private final Configuration configuration;

  public MarcConverterService(Configuration configuration, LinkingRuleService linkingRuleService) {
    this.configuration = configuration;
    this.linkingRuleService = linkingRuleService;
  }

  public File generateBibs(List<ExternalIdsHolder> authorities) {
    var authoritiesFields = mergeAuthoritiesFields(authorities);
    linkingRuleService.setAuthorityMergedFields(authoritiesFields);

    var mrcFile = new ArrayList<String>();
    if (configuration.isUniqueMarcBibs() || configuration.getMarcBibs() == null) {
      for (var authority : authorities) {
        var marcBibFields = new ArrayList<MarcField>();
        for (var field : authority.getFields()) {
          var marcBibField = linkingRuleService.constructBibFieldByAuthorityTag(field);
          if (marcBibField != null) {
            marcBibFields.add(marcBibField);
          }
        }
        if (marcBibFields.isEmpty()) {
          log.info("Authority {} have no fields to link", authority.getId());
          continue;
        }
        marcBibFields.add(getNameMarcField(List.of(authority.getId())));
        marcBibFields.add(get008EmptyField());
        marcBibFields.add(getInstanceTypeField());
        mrcFile.add(createInstance(marcBibFields));
      }
    } else {
      for (var config : configuration.getMarcBibs()) {
        var marcBibFields = mapFieldsAndFilterByRules(config.linkingFields());
        marcBibFields.add(getNameMarcField(config.linkingFields()));
        marcBibFields.add(get008EmptyField());
        marcBibFields.add(getInstanceTypeField());

        for (int i = 0; i < config.totalBibs(); i++) {
          var instance = createInstance(marcBibFields);
          mrcFile.add(instance);
        }
      }
    }

    return writeFile(GENERATED_BIBS_FILE, mrcFile);
  }

  private String createInstance(List<MarcField> bibFields) {
    RecordWriter recordWriter = new MarcRecordWriter();
    recordWriter.writeLeader(getLeaderTranslation());
    bibFields.forEach(bibField -> {
      if (bibField instanceof SimpleMarcField simpleMarcField) {
        recordWriter.writeField(bibField.getTag(), mapToStringValue(simpleMarcField));
      } else {
        recordWriter.writeField(bibField.getTag(), mapToCompositeValue(bibField));
      }
    });

    return recordWriter.getResult();
  }

  private List<MarcField> mapFieldsAndFilterByRules(List<String> linkingFields) {
    var marcFields = new ArrayList<MarcField>();
    var fieldsToRemove = new ArrayList<String>();

    for (var requiredField : linkingFields) {
      var bibFields = linkingRuleService.constructBibFieldsByBibTag(requiredField);
      if (bibFields == null) {
        fieldsToRemove.add(requiredField);
      } else {
        marcFields.addAll(bibFields);
      }
    }

    linkingFields.removeAll(fieldsToRemove);
    return marcFields;
  }

  private Map<String, List<MarcField>> mergeAuthoritiesFields(List<ExternalIdsHolder> authorities) {
    var merged = new HashMap<String, List<MarcField>>();
    authorities.forEach(linkingRuleService::putLinkingSubfields);
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

  private SimpleMarcField get008EmptyField() {
    return new SimpleMarcField("008", repeat(' ', 40));
  }

  private MarcField getNameMarcField(List<String> fields) {
    var name = String.format("Generated bib. Linked: %s", fields);
    return new MarcField("245", "0", "0", Map.of('a', name));
  }

  private MarcField getInstanceTypeField() {
    return new MarcField("336", " ", " ", Map.of('a', "still image"));
  }
}
