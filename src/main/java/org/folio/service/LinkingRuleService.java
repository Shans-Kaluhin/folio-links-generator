package org.folio.service;

import lombok.extern.slf4j.Slf4j;
import org.folio.client.EntitiesLinksClient;
import org.folio.model.MarcField;
import org.folio.model.integration.ExternalIdsHolder;
import org.folio.model.integration.LinkingRule;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
public class LinkingRuleService {
    protected static final String ID_LOC_GOV = "https://id.loc.gov/authorities/names/";
    private final EntitiesLinksClient linksClient;

    public LinkingRuleService(EntitiesLinksClient linksClient) {
        this.linksClient = linksClient;
    }

    public void setLinkingSubfields(ExternalIdsHolder authority) {
        var authorityId = authority.getId();
        var naturalId = ID_LOC_GOV + authority.getNaturalId();

        authority.getFields().forEach(f -> f.getSubfields()
                .putAll(Map.of('0', naturalId, '9', authorityId)));
    }

    public List<MarcField> constructBibFields(String requiredField, Map<String, List<MarcField>> authorityMergedFields) {
        var bibMarcFields = new ArrayList<MarcField>();

        var linkingRules = linksClient.getLinkedRules().get(requiredField);
        if (linkingRules == null) {
            log.info("Field {} was skipped as it does not comply with linked rules", requiredField);
            return null;
        }

        for (var linkingRule : linkingRules) {
            var authorityFields = authorityMergedFields.get(linkingRule.getAuthorityField());

            if (authorityFields != null) {
                for (var authorityField : authorityFields) {
                    if (isViolateExistence(linkingRule, authorityField, requiredField)) {
                        continue;
                    }

                    var bibMarcField = authorityField.copyWithTag(linkingRule.getBibField());
                    modifySubfields(linkingRule, bibMarcField);
                    bibMarcFields.add(bibMarcField);
                }
            }
        }

        return bibMarcFields;
    }

    private void modifySubfields(LinkingRule linkingRule, MarcField bibMarcField) {
        var modifications = linkingRule.getSubfieldModifications();

        if (!modifications.isEmpty()) {
            for (var modification : modifications) {
                var subfields = bibMarcField.getSubfields();
                var source = subfields.remove(modification.source());

                if (source != null) {
                    subfields.put(modification.target(), source);
                }
            }
        }
    }

    private boolean isViolateExistence(LinkingRule linkingRule, MarcField authorityField, String requiredField) {
        var existenceValidations = linkingRule.getValidation();

        if (!existenceValidations.isEmpty()) {
            for (var validation : existenceValidations) {
                var subfields = authorityField.getSubfields();
                var isExist = subfields.get(validation.subfield()) != null;

                if (isExist != validation.existence()) {
                    log.info("Authority {}. Fields {} -> {} was not linked. Subfield '{}' is {}",
                            authorityField.getSubfields().get('9'),
                            requiredField,
                            authorityField.getTag(),
                            validation.subfield(),
                            isExist ? "NOT required" : "required");
                    return true;
                }
            }
        }
        return false;
    }
}
