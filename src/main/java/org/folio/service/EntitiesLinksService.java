package org.folio.service;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import me.tongfei.progressbar.ProgressBar;
import org.folio.client.EntitiesLinksClient;
import org.folio.model.MarcField;
import org.folio.model.integration.ExternalIdsHolder;
import org.folio.model.integration.InstanceLinks;

import java.util.List;
import java.util.Objects;

import static org.folio.service.LinkingRuleService.ID_LOC_GOV;
import static org.folio.service.LinksGenerationService.progressBarBuilder;

@Slf4j
@Setter
public class EntitiesLinksService {
    private static final String LINKING_TITLE = "Linking Instances";
    private final EntitiesLinksClient linksClient;
    private final LinkingRuleService linkingRuleService;

    public EntitiesLinksService(EntitiesLinksClient linksClient, LinkingRuleService linkingRuleService) {
        this.linksClient = linksClient;
        this.linkingRuleService = linkingRuleService;
    }

    public void linkRecords(List<ExternalIdsHolder> instances) {
        for (var instance : ProgressBar.wrap(instances, progressBarBuilder(LINKING_TITLE))) {
            var instanceId = instance.getId();
            var instanceLinks = constructLinksByMarcFields(instance);

            linksClient.link(instanceId, instanceLinks);
        }
    }

    private InstanceLinks constructLinksByMarcFields(ExternalIdsHolder instance) {
        return new InstanceLinks(instance.getFields().stream()
                .map(marc -> constructLink(instance.getId(), marc))
                .filter(Objects::nonNull)
                .toList());
    }

    private InstanceLinks.Link constructLink(String instanceId, MarcField marcField) {
        if (marcField == null || !marcField.getSubfields().containsKey('0')) {
            return null;
        }
        var bibTag = marcField.getTag();
        var ruleId = linkingRuleService.getRuleId(bibTag);
        var subfields = marcField.getSubfields();
        var authorityId = subfields.get('9');
        var authorityNaturalId = subfields.get('0').substring(ID_LOC_GOV.length());
        subfields.remove('9');
        subfields.remove('0');

        return new InstanceLinks.Link(ruleId, instanceId, authorityId, bibTag, authorityNaturalId, subfields.keySet());
    }
}
