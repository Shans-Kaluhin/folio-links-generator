package org.folio.reference;

import org.apache.commons.collections4.CollectionUtils;
import org.folio.processor.referencedata.ReferenceDataWrapper;
import org.folio.processor.rule.Metadata;
import org.folio.processor.translations.Translation;
import org.folio.processor.translations.TranslationFunction;
import org.folio.processor.translations.TranslationHolder;

import java.util.List;

public enum ReferenceTranslationHolder implements TranslationHolder, TranslationFunction {
    SET_VALUE {
        public String apply(String value, int currentIndex, Translation translation, ReferenceDataWrapper referenceData, Metadata metadata) {
            return translation.getParameter("value");
        }
    },
    SET_NATURE_OF_CONTENT_TERM {
        public String apply(String id, int currentIndex, Translation translation, ReferenceDataWrapper referenceData, Metadata metadata) {
            return "audiobook";
        }
    },
    SET_IDENTIFIER {
        public String apply(String identifierValue, int currentIndex, Translation translation, ReferenceDataWrapper referenceData, Metadata metadata) {
            Object metadataIdentifierTypeIds = metadata.getData().get("identifierType").getData();
            if (metadataIdentifierTypeIds != null) {
                if (translation.getParameter("type").equalsIgnoreCase(identifierValue)) {
                    return identifierValue;
                }
            }

            return "";
        }
    },
    SET_RELATED_IDENTIFIER {
        public String apply(String identifierValue, int currentIndex, Translation translation, ReferenceDataWrapper referenceData, Metadata metadata) {
            Object metadataIdentifierTypeIds = metadata.getData().get("identifierType").getData();
            if (metadataIdentifierTypeIds != null) {
                if (translation.getParameter("relatedIdentifierTypes").equalsIgnoreCase(identifierValue)) {
                    return translation.getParameter("type");
                }
            }

            return "";
        }
    },
    SET_CONTRIBUTOR {
        public String apply(String identifierValue, int currentIndex, Translation translation, ReferenceDataWrapper referenceData, Metadata metadata) {
            Object metadataContributorNameTypeIds = metadata.getData().get("contributorNameTypeId").getData();
            if (metadataContributorNameTypeIds != null) {
                return translation.getParameter("type");
            }

            return "";
        }
    },
    SET_ALTERNATIVE_TITLE {
        public String apply(String identifierValue, int currentIndex, Translation translation, ReferenceDataWrapper referenceData, Metadata metadata) {
            Object metadataAlternativeTitleTypesIds = metadata.getData().get("alternativeTitleTypeId").getData();
            if (metadataAlternativeTitleTypesIds != null) {
                return identifierValue;
            }

            return "";
        }
    },
    SET_INSTANCE_TYPE_ID {
        public String apply(String instanceTypeId, int currentIndex, Translation translation, ReferenceDataWrapper referenceData, Metadata metadata) {
            return "still image";
        }
    },
    SET_INSTANCE_FORMAT_ID {
        public String apply(String instanceFormatId, int currentIndex, Translation translation, ReferenceDataWrapper referenceData, Metadata metadata) {
            if (translation.getParameter("value").equals("0")) {
                return "audio";
            } else {
                return "audio disc";
            }
        }
    },
    SET_ELECTRONIC_ACCESS_INDICATOR {
        public String apply(String value, int currentIndex, Translation translation, ReferenceDataWrapper referenceData, Metadata metadata) {
            List<String> relationshipIds = (List) metadata.getData().get("relationshipId").getData();
            if (CollectionUtils.isNotEmpty(relationshipIds) && relationshipIds.size() > currentIndex) {
                return relationshipIds.get(currentIndex);
            }

            return " ";
        }
    };

    public TranslationFunction lookup(String function) {
        return valueOf(function.toUpperCase());
    }
}
