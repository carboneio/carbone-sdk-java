package io.carbone;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class CarboneV5Test {
    ICarboneTemplateClient carboneTemplate = mock(CarboneTemplateClient.class);
    ICarboneRenderClient carboneRender = mock(CarboneRenderClient.class);
    ICarboneStatusClient carboneStatus = mock(CarboneStatusClient.class);
    ICarboneServices carboneServices = CarboneServicesFactory.CARBONE_SERVICES_FACTORY_INSTANCE.create(carboneTemplate, carboneRender, carboneStatus);

    static final String TEMPLATE_ID = "1234567890123456789";
    static final String VERSION_ID = "fb9241ea2218ffd8f974110e539386384620244618c2efbf182b7bd47242987a";
    static final byte[] TEMPLATE = "template content".getBytes();

    private static <T> CarboneApiResponse<T> success(T data) {
        return CarboneApiResponse.<T>builder().success(true).data(data).build();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> captureAddTemplateForm() throws CarboneException {
        ArgumentCaptor<Map<String, ?>> form = ArgumentCaptor.forClass(Map.class);
        verify(carboneTemplate).addTemplate(form.capture());
        return (Map<String, Object>) form.getValue();
    }

    @Test
    public void Test_Add_Template_With_Options_Sends_Fields_In_Api_Order() throws CarboneException {
        CarboneTemplate uploaded = CarboneTemplate.builder().id(TEMPLATE_ID).versionId(VERSION_ID).build();
        when(carboneTemplate.addTemplate(anyMap())).thenReturn(success(uploaded));

        CarboneTemplate result = carboneServices.addTemplate(TEMPLATE, CarboneTemplateOptions.builder()
            .expireAt(0L)
            .tags(Arrays.asList("invoice", "v2"))
            .name("Invoice")
            .id(TEMPLATE_ID)
            .versioning(true)
            .build());

        assertSame(uploaded, result);
        Map<String, Object> form = captureAddTemplateForm();
        assertEquals(Arrays.asList("versioning", "id", "name", "tags", "expireAt", "template"), new ArrayList<>(form.keySet()));
        assertEquals("true", form.get("versioning"));
        assertEquals("[\"invoice\",\"v2\"]", form.get("tags"));
        assertEquals("0", form.get("expireAt"));
        assertArrayEquals(TEMPLATE, (byte[]) form.get("template"));
    }

    @Test
    public void Test_Add_Template_Without_Options_Sends_Only_The_File() throws CarboneException {
        CarboneTemplate legacy = CarboneTemplate.builder().templateId(VERSION_ID).templateExtension("odt").build();
        when(carboneTemplate.addTemplate(anyMap())).thenReturn(success(legacy));

        CarboneTemplate result = carboneServices.addTemplate(TEMPLATE, null);

        assertEquals(VERSION_ID, result.getTemplateId());
        assertEquals(Collections.singletonList("template"), new ArrayList<>(captureAddTemplateForm().keySet()));
    }

    @Test
    public void Test_Add_Template_With_Options_Api_Error() throws CarboneException {
        when(carboneTemplate.addTemplate(anyMap())).thenReturn(CarboneApiResponse.<CarboneTemplate>builder()
            .success(false).error("\"name\" length must be less than or equal to 200 characters long").code("w103").build());

        CarboneException e = assertThrows(CarboneException.class, () -> carboneServices.addTemplate(TEMPLATE, new CarboneTemplateOptions()));

        assertEquals("\"name\" length must be less than or equal to 200 characters long", e.getMessage());
        assertEquals("w103", e.getCarboneResponse().getCode());
    }

    @Test
    public void Test_Update_Template_Sends_Only_Set_Fields() throws CarboneException {
        CarboneTemplate updated = CarboneTemplate.builder().versionId(VERSION_ID).name("Renamed").build();
        when(carboneTemplate.updateTemplate(eq(TEMPLATE_ID), anyString())).thenReturn(success(updated));

        CarboneTemplate result = carboneServices.updateTemplate(TEMPLATE_ID, CarboneTemplateUpdate.builder()
            .name("Renamed")
            .tags(Collections.singletonList("contract"))
            .build());

        assertSame(updated, result);
        verify(carboneTemplate).updateTemplate(TEMPLATE_ID, "{\"name\":\"Renamed\",\"tags\":[\"contract\"]}");
    }

    @Test
    public void Test_Update_Template_Missing_Arguments() {
        assertThrows(CarboneException.class, () -> carboneServices.updateTemplate("", new CarboneTemplateUpdate()));
        assertThrows(CarboneException.class, () -> carboneServices.updateTemplate(TEMPLATE_ID, null));
        verifyNoInteractions(carboneTemplate);
    }

    @Test
    public void Test_List_Templates_With_Query() throws CarboneException {
        List<CarboneTemplate> templates = Collections.singletonList(CarboneTemplate.builder().id(TEMPLATE_ID).build());
        CarboneApiResponse<List<CarboneTemplate>> response = success(templates);
        response.setHasMore(true);
        response.setNextCursor("cursor-2");
        when(carboneTemplate.listTemplates(anyMap())).thenReturn(response);

        CarboneTemplateList result = carboneServices.listTemplates(CarboneTemplateQuery.builder()
            .category("invoices").limit(10).includeVersions(true).build());

        assertEquals(templates, result.getTemplates());
        assertTrue(result.isHasMore());
        assertEquals("cursor-2", result.getNextCursor());
        verify(carboneTemplate).listTemplates(Map.of("category", "invoices", "includeVersions", true, "limit", 10));
    }

    @Test
    public void Test_List_Templates_Without_Query() throws CarboneException {
        when(carboneTemplate.listTemplates(anyMap())).thenReturn(success(null));

        CarboneTemplateList result = carboneServices.listTemplates();

        assertTrue(result.getTemplates().isEmpty());
        assertFalse(result.isHasMore());
        assertNull(result.getNextCursor());
        verify(carboneTemplate).listTemplates(Collections.emptyMap());
    }

    @Test
    public void Test_List_Template_Categories_And_Tags() throws CarboneException {
        when(carboneTemplate.listTemplateCategories()).thenReturn(success(Arrays.asList(Map.of("name", "customer2"), Map.of("name", "folder1"))));
        when(carboneTemplate.listTemplateTags()).thenReturn(success(Arrays.asList(Map.of("name", "contract"), Map.of("name", "invoice"))));

        assertEquals(Arrays.asList("customer2", "folder1"), carboneServices.listTemplateCategories());
        assertEquals(Arrays.asList("contract", "invoice"), carboneServices.listTemplateTags());
    }

    @Test
    public void Test_Render_Report_With_Webhook_Headers() throws CarboneException {
        String json = "{\"data\":{}}";
        CarboneResponse response = CarboneResponse.builder().success(true).build();
        when(carboneRender.renderReport(eq(json), eq(TEMPLATE_ID), anyMap())).thenReturn(response);

        String renderId = carboneServices.renderReport(json, TEMPLATE_ID, CarboneRenderOptions.builder()
            .webhookUrl("https://example.com/hook")
            .webhookHeader("authorization", "Bearer secret")
            .header("carbone-egress-header-authorization", "Bearer egress")
            .build());

        assertNull(renderId);
        verify(carboneRender).renderReport(json, TEMPLATE_ID, Map.of(
            "carbone-webhook-url", "https://example.com/hook",
            "carbone-webhook-header-authorization", "Bearer secret",
            "carbone-egress-header-authorization", "Bearer egress"));
    }

    @Test
    public void Test_Render_Report_With_Options_Returns_Render_Id() throws CarboneException {
        CarboneResponse response = CarboneResponse.builder().success(true)
            .data(CarboneResponse.CarboneResponseData.builder().renderId("render-id").build()).build();
        when(carboneRender.renderReport(anyString(), anyString(), anyMap())).thenReturn(response);

        assertEquals("render-id", carboneServices.renderReport("{}", VERSION_ID, null));
        verify(carboneRender).renderReport("{}", VERSION_ID, Collections.emptyMap());
    }

    @Test
    public void Test_Render_Report_With_Options_Api_Error() throws CarboneException {
        CarboneResponse response = CarboneResponse.builder().success(false).error("Invalid webhook URL").build();
        when(carboneRender.renderReport(anyString(), anyString(), anyMap())).thenReturn(response);

        CarboneException e = assertThrows(CarboneException.class, () -> carboneServices.renderReport("{}", TEMPLATE_ID, null));

        assertEquals("Invalid webhook URL", e.getMessage());
    }

    @Test
    public void Test_Render_And_Download() throws CarboneException {
        CarboneDocument document = new CarboneDocument("pdf".getBytes(), "report.pdf");
        when(carboneRender.renderAndDownload("{}", TEMPLATE_ID, Collections.emptyMap())).thenReturn(document);

        assertSame(document, carboneServices.renderAndDownload("{}", TEMPLATE_ID));
    }

    @Test
    public void Test_Render_And_Download_Missing_Arguments() {
        assertThrows(CarboneException.class, () -> carboneServices.renderAndDownload("{}", ""));
        assertThrows(CarboneException.class, () -> carboneServices.renderAndDownload("", TEMPLATE_ID));
        verifyNoInteractions(carboneRender);
    }

    @Test
    public void Test_Render_Inline_Adds_Base64_Template_To_Body() throws CarboneException {
        CarboneDocument document = new CarboneDocument("pdf".getBytes(), "report.pdf");
        when(carboneRender.renderAndDownload(anyString(), eq("template"), anyMap())).thenReturn(document);

        CarboneDocument result = carboneServices.renderInline("{\"data\":{\"id\":1},\"convertTo\":\"pdf\"}", TEMPLATE);

        assertSame(document, result);
        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(carboneRender).renderAndDownload(body.capture(), eq("template"), any());
        JsonObject json = JsonParser.parseString(body.getValue()).getAsJsonObject();
        assertEquals(1, json.getAsJsonObject("data").get("id").getAsInt());
        assertEquals("pdf", json.get("convertTo").getAsString());
        assertEquals(Base64.getEncoder().encodeToString(TEMPLATE), json.get("template").getAsString());
    }

    @Test
    public void Test_Render_Inline_Rejects_Invalid_Arguments() {
        assertThrows(CarboneException.class, () -> carboneServices.renderInline("[1, 2]", TEMPLATE));
        assertThrows(CarboneException.class, () -> carboneServices.renderInline("{not json", TEMPLATE));
        assertThrows(CarboneException.class, () -> carboneServices.renderInline("{}", new byte[0]));
        verifyNoInteractions(carboneRender);
    }
}
