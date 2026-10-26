package io.carbone;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * One page of templates. While {@code isHasMore()} is {@code true}, set {@code getNextCursor()}
 * as the {@code cursor} of the next {@code CarboneTemplateQuery} to get the next page.
 */
@Data
@AllArgsConstructor
public class CarboneTemplateList {

    private final List<CarboneTemplate> templates;
    private final boolean hasMore;
    private final String nextCursor;
}
