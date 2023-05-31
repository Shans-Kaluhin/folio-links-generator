package org.folio.model.integration;

import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class InstanceLinks {
  private List<Link> links;

  public record Link(int linkingRuleId,
                     String instanceId,
                     String authorityId,
                     String bibRecordTag,
                     String authorityNaturalId,
                     Set<Character> bibRecordSubfields) {
  }
}
