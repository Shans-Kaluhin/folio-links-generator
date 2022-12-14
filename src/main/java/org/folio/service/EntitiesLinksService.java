package org.folio.service;

import org.folio.client.EntitiesLinksClient;
import org.folio.model.Configuration;
import org.folio.model.integration.ExternalIdsHolder;
import org.folio.model.integration.InstanceLinks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

public class EntitiesLinksService {
    private static final Logger LOG = LoggerFactory.getLogger(EntitiesLinksService.class);
    private final EntitiesLinksClient linksClient;
    private final Configuration configuration;

    public EntitiesLinksService(Configuration configuration, EntitiesLinksClient linksClient) {
        this.configuration = configuration;
        this.linksClient = linksClient;
    }

    public void linkRecords(List<ExternalIdsHolder> instances, List<ExternalIdsHolder> authorities) {
        for (ExternalIdsHolder authorityHolder : authorities) {
            linkInstances(authorityHolder, instances);
        }
    }

    private void linkInstances(ExternalIdsHolder authorityHolder, List<ExternalIdsHolder> instances) {
        LOG.info("Linking instances for authority: " + authorityHolder.getId());

        for (Configuration.BibsConfig bibConfig : configuration.getMarcBibs()) {
            var instancesByTitle = retrieveInstancesBySubfields(instances, bibConfig.linkingFields());

            for (var instanceHolder : instancesByTitle) {
                var instanceLinks = constructLinks(bibConfig, authorityHolder, instanceHolder);

                linksClient.appendLinks(instanceHolder.getId(), new InstanceLinks(instanceLinks));
            }
        }
    }

    private List<InstanceLinks.Link> constructLinks(Configuration.BibsConfig config, ExternalIdsHolder authorityHolder, ExternalIdsHolder instanceHolder) {
        return config.linkingFields().stream()
                .map(String::valueOf)
                .map(field -> constructLink(field, authorityHolder, instanceHolder))
                .filter(l -> l.bibRecordSubfields() != null)
                .collect(Collectors.toList());
    }

    private InstanceLinks.Link constructLink(String field, ExternalIdsHolder authorityHolder, ExternalIdsHolder instanceHolder) {
        var instanceId = instanceHolder.getId();
        var authorityId = authorityHolder.getId();
        var authorityNaturalId = authorityHolder.getNaturalId();
        var subfields = linksClient.getLinkedRules().get(field);

        return new InstanceLinks.Link(instanceId, authorityId, field, authorityNaturalId, subfields);
    }

    private List<ExternalIdsHolder> retrieveInstancesBySubfields(List<ExternalIdsHolder> instances, List<String> subfields) {
        return instances.stream()
                .filter(i -> i.getTitle().contains(subfields.toString()))
                .toList();
    }
}
