package org.folio.model.integration;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;
import java.util.Set;

@Getter
@AllArgsConstructor
public class InstanceLinks {
    private List<Link> links;

    public record Link(String instanceId,
                       String authorityId,
                       String bibRecordTag,
                       String authorityNaturalId,
                       Set<Character> bibRecordSubfields) {
    }
}
