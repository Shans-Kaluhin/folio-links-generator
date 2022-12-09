package org.folio.model.integration;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.folio.model.RecordType;

@Getter
@AllArgsConstructor
public class ExternalIdsHolder {
    private String id;
    private String hrid;

    public static ExternalIdsHolder map(JsonNode jsonNode, RecordType recordType) {
        var holder = jsonNode.get("externalIdsHolder");
        var id = holder.get(recordType.getExternalIdName() + "Id").asText();
        var hrid = holder.get(recordType.getExternalIdName() + "Hrid").asText();

        return new ExternalIdsHolder(id, hrid);
    }
}
