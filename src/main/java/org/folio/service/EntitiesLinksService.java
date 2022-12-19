package org.folio.service;

import org.folio.client.EntitiesLinksClient;
import org.folio.model.Configuration;
import org.folio.model.integration.ExternalIdsHolder;
import org.folio.model.integration.InstanceLinks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class EntitiesLinksService {
    private static final Logger LOG = LoggerFactory.getLogger(EntitiesLinksService.class);
    private final EntitiesLinksClient linksClient;
    private final Configuration configuration;

    public EntitiesLinksService(Configuration configuration, EntitiesLinksClient linksClient) {
        this.configuration = configuration;
        this.linksClient = linksClient;
    }

    public void linkRecords(List<ExternalIdsHolder> instances, List<ExternalIdsHolder> authorities) {
        for (Configuration.BibsConfig bibConfig : configuration.getMarcBibs()) {
            var instancesByTitle = retrieveInstancesBySubfields(instances, bibConfig.linkingFields());

            for (var instanceHolder : instancesByTitle) {
                var instanceId = instanceHolder.getId();
                var instanceLinks = constructLinks(bibConfig, authorities, instanceHolder);

                LOG.info("Linking all authorities for instance: {}", instanceId);
                linksClient.link(instanceId, instanceLinks);
            }
        }
    }

    private InstanceLinks constructLinks(Configuration.BibsConfig config, List<ExternalIdsHolder> authorities, ExternalIdsHolder instanceHolder) {
        var links = new ArrayList<InstanceLinks.Link>();

        for (var authorityHolder : authorities) {
            config.linkingFields().stream()
                    .map(field -> constructLink(field, authorityHolder, instanceHolder))
                    .filter(Objects::nonNull)
                    .forEach(links::add);
        }
        return new InstanceLinks(links);
    }

    private InstanceLinks.Link constructLink(String field, ExternalIdsHolder authorityHolder, ExternalIdsHolder instanceHolder) {
        var instanceId = instanceHolder.getId();
        var authorityId = authorityHolder.getId();
        var authorityNaturalId = authorityHolder.getNaturalId();
        var marcField = instanceHolder.getField(field);

        if (marcField == null) {
            return null;
        }
        return new InstanceLinks.Link(instanceId, authorityId, field, authorityNaturalId, marcField.getSubfields().keySet());
    }

    private List<ExternalIdsHolder> retrieveInstancesBySubfields(List<ExternalIdsHolder> instances, List<String> subfields) {
        return instances.stream()
                .filter(i -> i.getTitle().contains(subfields.toString()))
                .toList();
    }
}
