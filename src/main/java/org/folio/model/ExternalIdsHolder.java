package org.folio.model;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Getter;

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
