package org.folio.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class InstanceLinks {
    private List<Link> links;

    public record Link(String instanceId,
                       String authorityId,
                       String bibRecordTag,
                       String authorityNaturalId,
                       List<Character> bibRecordSubfields) {
    }
}
