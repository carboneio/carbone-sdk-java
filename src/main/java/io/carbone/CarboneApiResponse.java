package io.carbone;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * JSON envelope of the Carbone API responses whose {@code data} is not a render or legacy template ID.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CarboneApiResponse<T> {

    private boolean success;
    private String error;
    private String code;
    private T data;
    private Boolean hasMore;
    private String nextCursor;
}
