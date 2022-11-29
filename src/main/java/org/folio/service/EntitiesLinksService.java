package org.folio.service;

import org.folio.client.EntitiesLinksClient;
import org.folio.model.Configuration;
import org.folio.model.ExternalIdsHolder;
import org.folio.model.InstanceLinks;

import java.util.ArrayList;
import java.util.List;

public class EntitiesLinksService {
    private final EntitiesLinksClient linksClient;
    private final Configuration configuration;
    private String rules;

    public EntitiesLinksService(Configuration configuration, EntitiesLinksClient linksClient) {
        this.configuration = configuration;
        this.linksClient = linksClient;
    }

    public void linkRecords(List<ExternalIdsHolder> instances, List<ExternalIdsHolder> authorities) {
        rules = linksClient.getLinkedRules();

        //authorities loop
        for (ExternalIdsHolder authorityHolder : authorities) {
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
        var subfields = new ArrayList<Character>();

        return new InstanceLinks.Link(instance.getId(), authority.getId(), field, subfields);
    }
}
