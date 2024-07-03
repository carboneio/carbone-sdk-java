package io.carbone;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CarboneException extends Exception {
    private final CarboneResponse carboneResponse;
    private final int httpStatus;

    public CarboneException(CarboneResponse carboneResponse, int httpStatus) {
        super(carboneResponse.getError());
        this.carboneResponse = carboneResponse;
        this.httpStatus = httpStatus;
    }

    public CarboneException(String errorMessage) {
        super(errorMessage);
        this.carboneResponse = null;
        this.httpStatus = 500;
    }
}
