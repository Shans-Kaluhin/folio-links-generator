package org.folio.service;

import org.folio.client.EntitiesLinksClient;
import org.folio.model.MarcField;
import org.folio.model.integration.ExternalIdsHolder;
import org.folio.model.integration.LinkingRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LinkingRuleService {
    private static final Logger LOG = LoggerFactory.getLogger(LinkingRuleService.class);
    private static final String ID_LOC_GOV = "https://id.loc.gov/authorities/names/";
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
            LOG.info("Field {} was skipped as it does not comply with linked rules", requiredField);
            return null;
        }

        for (var linkingRule : linkingRules) {
            var authorityFields = authorityMergedFields.get(linkingRule.getAuthorityField());

            for (var authorityField : authorityFields) {
                if (isViolateExistence(linkingRule, authorityField, requiredField)) {
                    continue;
                }

                var bibMarcField = authorityField.copyWithTag(linkingRule.getBibField());
                modifySubfields(linkingRule, bibMarcField);
                bibMarcFields.add(bibMarcField);
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
                    LOG.info("Authority {}. Fields {} -> {} was not linked. Subfield '{}' is {}",
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
