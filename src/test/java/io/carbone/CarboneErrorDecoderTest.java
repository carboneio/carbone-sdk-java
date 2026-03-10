package io.carbone;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import feign.Request;
import feign.Response;

public class CarboneErrorDecoderTest {
    private Request request;
    private CarboneErrorDecoder carboneErrorDecoder;
    private CarboneResponse carboneResponse;

    @BeforeEach
    public void setUp() {
        request = Request.create(Request.HttpMethod.GET, "url", emptyMap(), Request.Body.empty(), null);

        carboneResponse = CarboneResponse.builder()
            .error("carbone error")
            .success(false)
            .build();

        carboneErrorDecoder = new CarboneErrorDecoder();
    }

    @Test
    public void Return_Carbone_Exception_Error_Request() throws JsonProcessingException {
        // GIVEN
        Response feignResponse = Response.builder()
            .request(request)
            .status(400)
            .body(new ObjectMapper().writeValueAsBytes(carboneResponse))
            .build();

        // WHEN
        Exception exception = carboneErrorDecoder.decode("GET", feignResponse);

        // THEN
        assertThat(exception)
            .isInstanceOf(CarboneException.class)
            .hasMessage("carbone error");
    }
}
