package org.folio.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.folio.client.EntitiesLinksClient;
import org.folio.model.Configuration;
import org.folio.model.ExternalIdsHolder;
import org.folio.model.InstanceLinks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class EntitiesLinksService {
    private static final Logger LOG = LoggerFactory.getLogger(EntitiesLinksService.class);
    private final EntitiesLinksClient linksClient;
    private final Configuration configuration;
    private HashMap<String, List<Character>> rules;

    public EntitiesLinksService(Configuration configuration, EntitiesLinksClient linksClient) {
        this.configuration = configuration;
        this.linksClient = linksClient;
    }

    public void linkRecords(List<ExternalIdsHolder> instances, List<ExternalIdsHolder> authorities) {
        rules = mapRules(linksClient.getLinkedRules());

        for (ExternalIdsHolder authorityHolder : authorities) {
            linkInstances(authorityHolder, instances);
        }
    }

    private void linkInstances(ExternalIdsHolder authorityHolder, List<ExternalIdsHolder> instances) {
        LOG.info("Linking instances for authority: " + authorityHolder.getId());
        int instancesAmount = instances.size();

        for (Configuration.BibsConfig bibConfig : configuration.getMarcBibs()) {
            for (int i = 0; i < bibConfig.totalBibs(); i++) {
                var instanceHolder = instances.get(--instancesAmount);
                var instanceLinks = constructLinks(bibConfig, authorityHolder, instanceHolder);

                linksClient.appendLinks(instanceHolder.getId(), new InstanceLinks(instanceLinks));
            }
        }
    }

    private List<InstanceLinks.Link> constructLinks(Configuration.BibsConfig config, ExternalIdsHolder authorityHolder, ExternalIdsHolder instanceHolder) {
        return config.linkingFields().stream()
                .map(String::valueOf)
                .map(field -> constructLink(field, authorityHolder, instanceHolder))
                .collect(Collectors.toList());
    }

    private InstanceLinks.Link constructLink(String field, ExternalIdsHolder authorityHolder, ExternalIdsHolder instanceHolder) {
        var instanceId = instanceHolder.getId();
        var authorityId = authorityHolder.getId();
        var authorityNaturalId = authorityHolder.getHrid().replaceAll("\\s", "");
        var subfields = rules.get(field);

        return new InstanceLinks.Link(instanceId, authorityId, field, authorityNaturalId, subfields);
    }

    private HashMap<String, List<Character>> mapRules(JsonNode jsonNode) {
        var rules = new HashMap<String, List<Character>>();

        for (JsonNode rule : jsonNode) {
            var bibField = rule.get("bibField").asText();
            var subfields = new ArrayList<Character>();
            rule.get("authoritySubfields").elements().forEachRemaining(subfield -> subfields.add(subfield.asText().charAt(0)));
            rules.put(bibField, subfields);
        }

        return rules;
    }
}
