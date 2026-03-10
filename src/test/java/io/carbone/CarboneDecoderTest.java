package io.carbone;

import static java.util.Collections.emptyMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import feign.Request;
import feign.Response;
public class CarboneDecoderTest {

    private Request request;
    private CarboneDecoder carboneDecoder;

    @BeforeEach
    public void setUp() {
        request = Request.create(Request.HttpMethod.GET, "url", emptyMap(), Request.Body.empty(), null);

        carboneDecoder = new CarboneDecoder();
    }

    @Test
    public void Test_Decoder_Carbone_Response() throws IOException {
        //GIVEN
        CarboneResponse carboneResponse = CarboneResponse.builder()
            .success(true)
            .build();

        Response feignResponse = Response.builder()
            .request(request)
            .body(new ObjectMapper().writeValueAsBytes(carboneResponse))
            .build();


        // WHEN
        Object decodedObj = carboneDecoder.decode(feignResponse, CarboneResponse.class);

        // THEN
        Assertions.assertThat(decodedObj)
            .isInstanceOf(CarboneResponse.class);
    }

    @Test
    public void Test_Decoder_Carbone_File_Response() throws IOException {
        //GIVEN
        CarboneFileResponse carboneResponse = new CarboneFileResponse(new byte[]{});

        Response feignResponse = Response.builder()
            .request(request)
            .body(new ObjectMapper().writeValueAsBytes(carboneResponse))
            .build();


        // WHEN
        Object decodedObj = carboneDecoder.decode(feignResponse, CarboneFileResponse.class);

        // THEN
        Assertions.assertThat(decodedObj)
            .isInstanceOf(CarboneFileResponse.class);
    }

    @Test
    public void Test_Decoder_Carbone_Document() throws IOException {
        //GIVEN
        CarboneDocument carboneResponse = new CarboneDocument(new byte[]{}, new String());

        Response feignResponse = Response.builder()
            .request(request)
            .body(new ObjectMapper().writeValueAsBytes(carboneResponse))
            .build();


        // WHEN
        Object decodedObj = carboneDecoder.decode(feignResponse, CarboneDocument.class);

        // THEN
        Assertions.assertThat(decodedObj)
            .isInstanceOf(CarboneDocument.class);
    }

    @Test
    public void Test_Decode_Content_Disposition(){
        String resposne = "";
        Map<String, Collection<String>> headers = new HashMap<>();

        headers.put("access-control-allow-origin", Arrays.asList("*"));
        headers.put("access-control-expose-headers", Arrays.asList("X-Request-URL", "Content-Disposition"));
        headers.put("content-disposition", Arrays.asList("filename=\"report.pdf\""));
        headers.put("content-type", Arrays.asList("application/pdf"));
        headers.put("date", Arrays.asList("Tue, 18 Jun 2024 08:28:34 GMT"));
        headers.put("keep-alive", Arrays.asList("timeout=5"));
        headers.put("strict-transport-security", Arrays.asList("max-age=63072000"));
        headers.put("transfer-encoding", Arrays.asList("chunked"));
        headers.put("x-request-url", Arrays.asList("/render/MTAuMjAuMjEuNDEgICAg9FYGg17kxU3kclCwTqv6iQcmVwb3J0.pdf"));
        resposne = carboneDecoder.DecodeContentDisposition(headers);

        assertEquals(resposne, "report.pdf");
    }

    @Test
    public void Test_Decode_Content_Disposition_Not_Egal_At_2(){
        String resposne = "";
        Map<String, Collection<String>> headers = new HashMap<>();

        headers.put("access-control-allow-origin", Arrays.asList("*"));
        headers.put("access-control-expose-headers", Arrays.asList("X-Request-URL", "Content-Disposition"));
        headers.put("content-disposition", Arrays.asList("filename\"report\""));
        headers.put("content-type", Arrays.asList("application/pdf"));
        headers.put("date", Arrays.asList("Tue, 18 Jun 2024 08:28:34 GMT"));
        headers.put("keep-alive", Arrays.asList("timeout=5"));
        headers.put("strict-transport-security", Arrays.asList("max-age=63072000"));
        headers.put("transfer-encoding", Arrays.asList("chunked"));
        headers.put("x-request-url", Arrays.asList("/render/MTAuMjAuMjEuNDEgICAg9FYGg17kxU3kclCwTqv6iQcmVwb3J0.pdf"));
        resposne = carboneDecoder.DecodeContentDisposition(headers);

        assertNull(resposne);
    }
}
