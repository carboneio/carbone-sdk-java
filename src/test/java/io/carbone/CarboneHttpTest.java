package io.carbone;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpServer;

/**
 * Checks the HTTP requests built by the Feign clients against a local server.
 */
public class CarboneHttpTest {

    HttpServer server;
    String previousUrl;
    ICarboneServices carboneServices;

    // last request received
    String method;
    String uri;
    Headers requestHeaders;
    byte[] requestBody;

    // next response sent
    int responseStatus;
    String responseContentType;
    byte[] responseBody;
    Map<String, String> responseHeaders;

    @BeforeEach
    public void startServer() throws IOException, CarboneException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            method = exchange.getRequestMethod();
            uri = exchange.getRequestURI().toString();
            requestHeaders = exchange.getRequestHeaders();
            requestBody = exchange.getRequestBody().readAllBytes();
            exchange.getResponseHeaders().set("Content-Type", responseContentType);
            responseHeaders.forEach((name, value) -> exchange.getResponseHeaders().set(name, value));
            exchange.sendResponseHeaders(responseStatus, responseBody.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(responseBody);
            }
        });
        server.start();
        respondJson(200, "{\"success\":true}");

        previousUrl = CarboneServicesFactory.CARBONE_SERVICES_FACTORY_INSTANCE.GetCarboneUrl();
        CarboneServicesFactory.CARBONE_SERVICES_FACTORY_INSTANCE.SetCarboneUrl("http://127.0.0.1:" + server.getAddress().getPort());
        carboneServices = CarboneServicesFactory.CARBONE_SERVICES_FACTORY_INSTANCE.create("test-token");
    }

    @AfterEach
    public void stopServer() throws CarboneException {
        CarboneServicesFactory.CARBONE_SERVICES_FACTORY_INSTANCE.SetCarboneUrl(previousUrl);
        server.stop(0);
    }

    private void respondJson(int status, String json) {
        responseStatus = status;
        responseContentType = "application/json";
        responseBody = json.getBytes(StandardCharsets.UTF_8);
        responseHeaders = Map.of();
    }

    private String requestBodyAsString() {
        return new String(requestBody, StandardCharsets.UTF_8);
    }

    @Test
    public void Test_Default_Api_Version_Is_5() throws CarboneException {
        carboneServices.listTemplates();

        assertEquals("5", requestHeaders.getFirst("carbone-version"));
        assertEquals("Bearer test-token", requestHeaders.getFirst("Authorization"));
    }

    @Test
    public void Test_Api_Version_Can_Be_Set() throws CarboneException {
        CarboneServicesFactory.CARBONE_SERVICES_FACTORY_INSTANCE.create("test-token", "4").listTemplates();

        assertEquals("4", requestHeaders.getFirst("carbone-version"));
    }

    @Test
    public void Test_Add_Template_Legacy() throws CarboneException {
        respondJson(200, "{\"success\":true,\"data\":{\"templateId\":\"abc\",\"templateExtension\":\"odt\"}}");

        String templateId = carboneServices.addTemplate("content".getBytes());

        assertEquals("abc", templateId);
        assertEquals("POST", method);
        assertEquals("/template", uri);
    }

    @Test
    public void Test_Add_Template_With_Options_Multipart() throws CarboneException {
        respondJson(200, "{\"success\":true,\"data\":{\"id\":\"123\",\"versionId\":\"abc\",\"type\":\"odt\",\"size\":7,"
            + "\"createdAt\":1700000000,\"deployedAt\":1700000000}}");

        CarboneTemplate template = carboneServices.addTemplate("content".getBytes(), CarboneTemplateOptions.builder()
            .versioning(true).id("123").name("Invoice").tags(Arrays.asList("a", "b")).build());

        assertEquals("123", template.getId());
        assertEquals("abc", template.getVersionId());
        assertEquals(7L, template.getSize());
        assertEquals(1700000000L, template.getDeployedAt());
        assertEquals("POST", method);
        assertEquals("/template", uri);
        assertTrue(requestHeaders.getFirst("Content-Type").startsWith("multipart/form-data"));
        String body = requestBodyAsString();
        int versioning = body.indexOf("name=\"versioning\"");
        int id = body.indexOf("name=\"id\"");
        int name = body.indexOf("name=\"name\"");
        int tags = body.indexOf("name=\"tags\"");
        int file = body.indexOf("name=\"template\"");
        assertTrue(versioning >= 0 && versioning < id && id < name && name < tags && tags < file, body);
        assertTrue(body.contains("[\"a\",\"b\"]"), body);
        // the API ignores text fields that have a Content-Type header
        assertTrue(body.contains("name=\"versioning\"\r\n\r\ntrue\r\n"), body);
        assertEquals(1, body.split("Content-Type:", -1).length - 1, body);
        assertTrue(body.contains("content"), body);
    }

    @Test
    public void Test_Update_Template_Patch() throws CarboneException {
        respondJson(200, "{\"success\":true,\"data\":{\"versionId\":\"abc\",\"name\":\"Renamed\"}}");

        CarboneTemplate template = carboneServices.updateTemplate("123", CarboneTemplateUpdate.builder().name("Renamed").build());

        assertEquals("Renamed", template.getName());
        assertEquals("PATCH", method);
        assertEquals("/template/123", uri);
        assertEquals("application/json", requestHeaders.getFirst("Content-Type"));
        assertEquals("{\"name\":\"Renamed\"}", requestBodyAsString());
    }

    @Test
    public void Test_Delete_And_Get_Template_Paths() throws CarboneException {
        carboneServices.deleteTemplate("abc");
        assertEquals("DELETE", method);
        assertEquals("/template/abc", uri);

        responseContentType = "application/octet-stream";
        responseBody = "file".getBytes();
        assertArrayEquals("file".getBytes(), carboneServices.getTemplate("abc"));
        assertEquals("GET", method);
        assertEquals("/template/abc", uri);
    }

    @Test
    public void Test_List_Templates_Query_And_Pagination() throws CarboneException {
        respondJson(200, "{\"success\":true,\"data\":[{\"id\":\"123\",\"versionId\":\"abc\",\"tags\":[\"x\"],\"origin\":1}],"
            + "\"hasMore\":true,\"nextCursor\":\"next\"}");

        CarboneTemplateList list = carboneServices.listTemplates(CarboneTemplateQuery.builder()
            .search("invoice 2026").limit(1).cursor("c1").build());

        assertEquals("GET", method);
        assertEquals("/templates?search=invoice%202026&limit=1&cursor=c1", uri);
        assertEquals(1, list.getTemplates().size());
        assertEquals("123", list.getTemplates().get(0).getId());
        assertEquals(Arrays.asList("x"), list.getTemplates().get(0).getTags());
        assertEquals(1, list.getTemplates().get(0).getOrigin());
        assertTrue(list.isHasMore());
        assertEquals("next", list.getNextCursor());
    }

    @Test
    public void Test_List_Categories_And_Tags() throws CarboneException {
        respondJson(200, "{\"success\":true,\"data\":[{\"name\":\"folder1\"},{\"name\":\"customer2\"}]}");
        assertEquals(Arrays.asList("folder1", "customer2"), carboneServices.listTemplateCategories());
        assertEquals("/templates/categories", uri);

        respondJson(200, "{\"success\":true,\"data\":[{\"name\":\"contract\"}]}");
        assertEquals(Arrays.asList("contract"), carboneServices.listTemplateTags());
        assertEquals("/templates/tags", uri);
    }

    @Test
    public void Test_Render_Report_With_Webhook() throws CarboneException {
        respondJson(200, "{\"success\":true,\"data\":{\"renderId\":\"\"}}");

        String renderId = carboneServices.renderReport("{\"data\":{}}", "123", CarboneRenderOptions.builder()
            .webhookUrl("https://example.com/hook").webhookHeader("authorization", "Bearer secret").build());

        assertNull(renderId);
        assertEquals("POST", method);
        assertEquals("/render/123", uri);
        assertEquals("https://example.com/hook", requestHeaders.getFirst("carbone-webhook-url"));
        assertEquals("Bearer secret", requestHeaders.getFirst("carbone-webhook-header-authorization"));
        assertEquals("{\"data\":{}}", requestBodyAsString());
    }

    @Test
    public void Test_Render_And_Download() throws CarboneException {
        responseStatus = 200;
        responseContentType = "application/pdf";
        responseBody = "%PDF".getBytes();
        responseHeaders = Map.of("Content-Disposition", "attachment; filename=\"invoice.pdf\"");

        CarboneDocument document = carboneServices.renderAndDownload("{\"data\":{\"id\":\"{d.id}\"}}", "abc");

        assertEquals("POST", method);
        assertEquals("/render/abc?download=true", uri);
        assertEquals("{\"data\":{\"id\":\"{d.id}\"}}", requestBodyAsString());
        assertArrayEquals("%PDF".getBytes(), document.getFileContent());
        assertEquals("invoice.pdf", document.getName());
    }

    @Test
    public void Test_Render_Inline() throws CarboneException {
        responseStatus = 200;
        responseContentType = "application/pdf";
        responseBody = "%PDF".getBytes();
        responseHeaders = Map.of("Content-Disposition", "attachment; filename=\"report.pdf\"");

        carboneServices.renderInline("{\"data\":{}}", "content".getBytes());

        assertEquals("/render/template?download=true", uri);
        assertEquals("{\"data\":{},\"template\":\"Y29udGVudA==\"}", requestBodyAsString());
    }

    @Test
    public void Test_Api_Error_Is_Mapped_To_Carbone_Exception() {
        respondJson(404, "{\"success\":false,\"error\":\"Template not found\",\"code\":\"w100\"}");

        CarboneException e = assertThrows(CarboneException.class, () -> carboneServices.renderAndDownload("{}", "missing"));

        assertEquals(404, e.getHttpStatus());
        assertEquals("Template not found", e.getMessage());
        assertEquals("w100", e.getCarboneResponse().getCode());
    }
}
