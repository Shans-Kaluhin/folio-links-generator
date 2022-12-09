package org.folio.model;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class JsonBibToPopulate {
    private int totalCount;
    private JsonNode sample;
}
