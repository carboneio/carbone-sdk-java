package io.carbone;

import java.util.LinkedHashMap;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Filters for listing templates. Only non-null fields are sent.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CarboneTemplateQuery {

    private String id;
    private String versionId;
    private String category;
    private Integer origin;
    /** {@code true} lists all versions of a Template ID. */
    private Boolean includeVersions;
    /** Fuzzy match on the name, or exact match on a Template ID or Version ID. */
    private String search;
    /** Between 1 and 100, defaults to 100. */
    private Integer limit;
    /** {@code CarboneTemplateList.getNextCursor()} of the previous page. */
    private String cursor;

    Map<String, Object> toQueryMap() {
        Map<String, Object> query = new LinkedHashMap<>();
        putIfNotNull(query, "id", id);
        putIfNotNull(query, "versionId", versionId);
        putIfNotNull(query, "category", category);
        putIfNotNull(query, "origin", origin);
        putIfNotNull(query, "includeVersions", includeVersions);
        putIfNotNull(query, "search", search);
        putIfNotNull(query, "limit", limit);
        putIfNotNull(query, "cursor", cursor);
        return query;
    }

    private static void putIfNotNull(Map<String, Object> map, String key, Object value) {
        if (value != null) {
            map.put(key, value);
        }
    }
}
