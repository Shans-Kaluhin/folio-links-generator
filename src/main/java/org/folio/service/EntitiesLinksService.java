package org.folio.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.folio.client.EntitiesLinksClient;
import org.folio.model.Configuration;
import org.folio.model.ExternalIdsHolder;
import org.folio.model.InstanceLinks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class EntitiesLinksService {
    private static final Logger LOG = LoggerFactory.getLogger(EntitiesLinksService.class);
    private final EntitiesLinksClient linksClient;
    private final Configuration configuration;
    private JsonNode rules;

    public EntitiesLinksService(Configuration configuration, EntitiesLinksClient linksClient) {
        this.configuration = configuration;
        this.linksClient = linksClient;
    }

    public void linkRecords(List<ExternalIdsHolder> instances, List<ExternalIdsHolder> authorities) {
        rules = linksClient.getLinkedRules();

        //authorities loop
        for (ExternalIdsHolder authorityHolder : authorities) {
            LOG.info("Linking instances for authority: " + authorityHolder.getId());
            //configuration loop
            for (Configuration.BibsConfig bibConfig : configuration.getMarcBibs()) {
                //instances loop
                for (int i = 0; i < bibConfig.totalBibs(); i++) {
                    var instanceHolder = instances.get(i);
                    var instanceLinks = constructLinks(instanceHolder, authorityHolder, bibConfig.linkingFields());
                    linksClient.link(instanceHolder.getId(), instanceLinks);
                }
            }
        }
    }

    public InstanceLinks constructLinks(ExternalIdsHolder instance, ExternalIdsHolder authority, List<Integer> fields) {
        var links = fields.stream()
                .map(String::valueOf)
                .map(field -> constructLinkByRules(instance, authority, field))
                .toList();

        return new InstanceLinks(links);
    }

    public InstanceLinks.Link constructLinkByRules(ExternalIdsHolder instance, ExternalIdsHolder authority, String field) {
        var authorityNaturalId = authority.getHrid().replaceAll("\\s", "");
        var subfields = new ArrayList<Character>();

        for (JsonNode rule : rules) {
            var bibField = rule.get("bibField").asText();
            if (field.equals(bibField)) {
                var authoritySubfields = rule.get("authoritySubfields");
                authoritySubfields.elements().forEachRemaining(subfield -> subfields.add(subfield.asText().charAt(0)));
                break;
            }
        }

        return new InstanceLinks.Link(instance.getId(), authority.getId(), field, authorityNaturalId, subfields);
    }
}
