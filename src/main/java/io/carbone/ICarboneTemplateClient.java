package io.carbone;

import java.util.List;
import java.util.Map;

import feign.Headers;
import feign.Param;
import feign.QueryMap;
import feign.RequestLine;

public interface ICarboneTemplateClient {
    @RequestLine("POST /template")
    @Headers("Content-Type: multipart/form-data")
    CarboneResponse addTemplate(@Param("template") byte[] templateFile) throws CarboneException;

    /** Form fields are sent in iteration order, the "template" file must be the last one. */
    @RequestLine("POST /template")
    @Headers("Content-Type: multipart/form-data")
    CarboneApiResponse<CarboneTemplate> addTemplate(Map<String, ?> formData) throws CarboneException;

    @RequestLine("PATCH /template/{templateId}")
    @Headers("Content-Type: application/json")
    CarboneApiResponse<CarboneTemplate> updateTemplate(@Param("templateId") String templateId, String jsonBody) throws CarboneException;

    @RequestLine("DELETE /template/{templateId}")
    CarboneResponse deleteTemplate(@Param("templateId") String templateId) throws CarboneException;

    @RequestLine("GET /template/{templateId}")
    CarboneFileResponse getTemplate(@Param("templateId") String templateId) throws CarboneException;

    @RequestLine("GET /templates")
    CarboneApiResponse<List<CarboneTemplate>> listTemplates(@QueryMap Map<String, Object> query) throws CarboneException;

    @RequestLine("GET /templates/categories")
    CarboneApiResponse<List<Map<String, String>>> listTemplateCategories() throws CarboneException;

    @RequestLine("GET /templates/tags")
    CarboneApiResponse<List<Map<String, String>>> listTemplateTags() throws CarboneException;
}
