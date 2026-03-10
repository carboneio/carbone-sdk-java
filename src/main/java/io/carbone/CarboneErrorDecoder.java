package io.carbone;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import feign.Response;
import feign.codec.ErrorDecoder;

class CarboneErrorDecoder implements ErrorDecoder {

    @Override
    public Exception decode(String methodKey, Response response) {
        CarboneException carboneException;
        try {
            String result = new String(response.body().asInputStream().readAllBytes(), StandardCharsets.UTF_8);

            ObjectMapper mapper = new ObjectMapper();
            mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
            CarboneResponse carboneResponse = mapper.readValue(result, CarboneResponse.class);
            carboneException = new CarboneException(carboneResponse, response.status());

        } catch (IOException e) {
            carboneException = new CarboneException(e.getMessage());
        }
        return carboneException;
    }
}
