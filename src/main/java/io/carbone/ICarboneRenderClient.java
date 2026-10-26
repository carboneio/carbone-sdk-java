package io.carbone;

import java.util.Map;

import feign.Headers;
import feign.HeaderMap;
import feign.Param;
import feign.RequestLine;
import feign.Body;
public interface ICarboneRenderClient {
    @RequestLine("POST /{templateId}")

    @Headers("Content-Type: application/json")
    @Body("{renderData}")
    CarboneResponse renderReport(@Param("renderData") String renderData, @Param("templateId") String templateId) throws CarboneException;

    @RequestLine("POST /{templateId}")
    @Headers("Content-Type: application/json")
    @Body("{renderData}")
    CarboneResponse renderReport(@Param("renderData") String renderData, @Param("templateId") String templateId, @HeaderMap Map<String, Object> headers) throws CarboneException;

    /** templateId "template" renders the base64 template sent in the "template" body field. */
    @RequestLine("POST /{templateId}?download=true")
    @Headers("Content-Type: application/json")
    @Body("{renderData}")
    CarboneDocument renderAndDownload(@Param("renderData") String renderData, @Param("templateId") String templateId, @HeaderMap Map<String, Object> headers) throws CarboneException;

    @RequestLine("GET /{renderId}")
    @Headers("Content-Type: application/String")
    CarboneDocument getReport(@Param("renderId") String renderId) throws CarboneException;
}
