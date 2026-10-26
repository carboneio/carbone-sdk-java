package io.carbone;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import feign.FeignException;
import feign.Response;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

class CarboneServices implements ICarboneServices {

    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    int err;
    private final ICarboneTemplateClient carboneTemplateClient;
    private final ICarboneRenderClient carboneRenderClient;
    private final ICarboneStatusClient carboneStatusClient ;
    String reportName;

    public CarboneServices(ICarboneTemplateClient carboneTemplateClient, ICarboneRenderClient carboneRenderClient, ICarboneStatusClient carboneStatusClient) {
        this.carboneTemplateClient = carboneTemplateClient;
        this.carboneRenderClient = carboneRenderClient;
        this.carboneStatusClient = carboneStatusClient;
    }

    @Override
    public String addTemplate(String templatePath) throws CarboneException, IOException {
        Path filePath = Paths.get(templatePath);
        byte[] fileBytes = Files.readAllBytes(filePath);
        return addTemplate(fileBytes);
    }

    @Override
    public String addTemplate(byte[] templateFile) throws CarboneException {
        CarboneResponse carboneResponse = carboneTemplateClient.addTemplate(templateFile);
        return carboneResponse.getData().getTemplateId();
    }

    @Override
    public boolean deleteTemplate(String templateId) throws CarboneException { 
        CarboneResponse carboneResponse = carboneTemplateClient.deleteTemplate(templateId);
        return carboneResponse.isSuccess();
    }

    public boolean checkPathIsAbsolute(String path) {
        Path p = Paths.get(path);
        return p.isAbsolute();
    }

    public String generateTemplateId(String path) {
        try {
            File file = new File(path);
            byte[] fileBytes;
            try (FileInputStream fis = new FileInputStream(file)) {
                fileBytes = fis.readAllBytes();
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
            
            MessageDigest digest;
            try {
                digest = MessageDigest.getInstance("SHA-256");
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
                return null;
            }
            
            digest.update(fileBytes);
            byte[] hashByte = digest.digest();
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashByte) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append(0);
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    public CarboneDocument render(String Json, String fileOrTemplateID) throws CarboneException
    {
        if (fileOrTemplateID.isEmpty()) {
            throw new CarboneException("Carbone SDK render error: argument is missing: file_or_template_id");
        }
        if (Json.isEmpty()) {
            throw new CarboneException("Carbone SDK render error: argument is missing: json_data");
        }
        CarboneResponse resp = null;
        File file = new File(fileOrTemplateID);
        if (!file.exists()) {
            resp = carboneRenderClient.renderReport(Json, fileOrTemplateID);
        } 
        else {
            try {
                String templateId = generateTemplateId(fileOrTemplateID);
                resp = carboneRenderClient.renderReport(Json, templateId);
            } catch (CarboneException e) {
                if(e.getHttpStatus() == 404) 
                {
                    try {
                        Path filePath = Paths.get(fileOrTemplateID);
                        CarboneResponse respAddTemplate = carboneTemplateClient.addTemplate(Files.readAllBytes(filePath));
                        if (respAddTemplate.isSuccess()) {
                            resp = carboneRenderClient.renderReport(Json, respAddTemplate.getData().templateId);
                        } else {
                            throw new CarboneException("Carbone SDK render error: failed to add template");
                        }
                    } catch (IOException err) {
                        throw new CarboneException("Carbone SDK render error: failed to read template file");
                    }
                }
                else{
                    throw e;
                }
            }
        }
        if (resp == null) {
            throw new CarboneException("Carbone SDK render error: something went wrong");
        }
        if (!resp.isSuccess()) {
            if (resp.getError() == null) {
                throw new CarboneException("Carbone SDK render error: render failed without error message");
            }
            throw new CarboneException(resp, 200);
        }
        return getReport(resp.getData().getRenderId());
    }


    @Override
    public String renderReport(String renderData, String templateId) throws CarboneException {
        if(checkPathIsAbsolute(templateId))
        {
            String newTemplateId = generateTemplateId(templateId);
            CarboneResponse carboneResponse = carboneRenderClient.renderReport(renderData, newTemplateId);
            return carboneResponse.getData().getRenderId();
        }

        CarboneResponse carboneResponse = carboneRenderClient.renderReport(renderData, templateId);
        return carboneResponse.getData().getRenderId();
    }

    @Override
    public CarboneDocument getReport(String renderId) throws CarboneException {
        CarboneDocument response = carboneRenderClient.getReport(renderId);
        return response;
    } 

    @Override
    public byte[] getTemplate(String templateId) throws CarboneException 
    {
        CarboneFileResponse response = carboneTemplateClient.getTemplate(templateId);
        return response.getFileContent();
    }

    @Override
    public CarboneTemplate addTemplate(byte[] templateFile, CarboneTemplateOptions options) throws CarboneException {
        Map<String, Object> form = new LinkedHashMap<>();
        if (options != null) {
            putFormField(form, "versioning", options.getVersioning());
            // the API requires "id" before the "template" field
            putFormField(form, "id", options.getId());
            putFormField(form, "name", options.getName());
            putFormField(form, "comment", options.getComment());
            putFormField(form, "category", options.getCategory());
            putFormField(form, "tags", options.getTags() == null ? null : GSON.toJson(options.getTags()));
            putFormField(form, "sample", options.getSample());
            putFormField(form, "deployedAt", options.getDeployedAt());
            putFormField(form, "expireAt", options.getExpireAt());
        }
        // the API requires "template" to be the last field
        form.put("template", templateFile);
        return unwrap(carboneTemplateClient.addTemplate(form), "add template");
    }

    @Override
    public CarboneTemplate updateTemplate(String templateIdOrVersionId, CarboneTemplateUpdate update) throws CarboneException {
        if (templateIdOrVersionId == null || templateIdOrVersionId.isEmpty()) {
            throw new CarboneException("Carbone SDK update template error: argument is missing: template_id");
        }
        if (update == null) {
            throw new CarboneException("Carbone SDK update template error: argument is missing: update");
        }
        return unwrap(carboneTemplateClient.updateTemplate(templateIdOrVersionId, GSON.toJson(update)), "update template");
    }

    @Override
    public CarboneTemplateList listTemplates(CarboneTemplateQuery query) throws CarboneException {
        Map<String, Object> queryMap = query == null ? new LinkedHashMap<>() : query.toQueryMap();
        CarboneApiResponse<List<CarboneTemplate>> response = carboneTemplateClient.listTemplates(queryMap);
        List<CarboneTemplate> templates = unwrap(response, "list templates");
        return new CarboneTemplateList(
            templates == null ? Collections.emptyList() : templates,
            Boolean.TRUE.equals(response.getHasMore()),
            response.getNextCursor());
    }

    @Override
    public List<String> listTemplateCategories() throws CarboneException {
        return names(unwrap(carboneTemplateClient.listTemplateCategories(), "list template categories"));
    }

    @Override
    public List<String> listTemplateTags() throws CarboneException {
        return names(unwrap(carboneTemplateClient.listTemplateTags(), "list template tags"));
    }

    @Override
    public String renderReport(String json, String templateIdOrVersionId, CarboneRenderOptions options) throws CarboneException {
        checkRenderArguments(json, templateIdOrVersionId);
        CarboneResponse response = carboneRenderClient.renderReport(json, templateIdOrVersionId, CarboneRenderOptions.toHeaders(options));
        if (response == null) {
            throw new CarboneException("Carbone SDK render error: something went wrong");
        }
        if (!response.isSuccess()) {
            throw apiError(response.getError(), response.getCode(), "render");
        }
        // with a webhook, the render ID is sent to the webhook URL and the API returns an empty one
        String renderId = response.getData() == null ? null : response.getData().getRenderId();
        return renderId == null || renderId.isEmpty() ? null : renderId;
    }

    @Override
    public CarboneDocument renderAndDownload(String json, String templateIdOrVersionId, CarboneRenderOptions options) throws CarboneException {
        checkRenderArguments(json, templateIdOrVersionId);
        return carboneRenderClient.renderAndDownload(json, templateIdOrVersionId, CarboneRenderOptions.toHeaders(options));
    }

    @Override
    public CarboneDocument renderInline(String json, byte[] templateFile, CarboneRenderOptions options) throws CarboneException {
        if (templateFile == null || templateFile.length == 0) {
            throw new CarboneException("Carbone SDK render error: argument is missing: template");
        }
        checkRenderArguments(json, "template");
        JsonObject body;
        try {
            body = JsonParser.parseString(json).getAsJsonObject();
        } catch (JsonParseException | IllegalStateException e) {
            throw new CarboneException("Carbone SDK render error: json_data must be a JSON object");
        }
        body.addProperty("template", Base64.getEncoder().encodeToString(templateFile));
        return carboneRenderClient.renderAndDownload(GSON.toJson(body), "template", CarboneRenderOptions.toHeaders(options));
    }

    private static void checkRenderArguments(String json, String templateIdOrVersionId) throws CarboneException {
        if (templateIdOrVersionId == null || templateIdOrVersionId.isEmpty()) {
            throw new CarboneException("Carbone SDK render error: argument is missing: file_or_template_id");
        }
        if (json == null || json.isEmpty()) {
            throw new CarboneException("Carbone SDK render error: argument is missing: json_data");
        }
    }

    private static void putFormField(Map<String, Object> form, String name, Object value) {
        if (value != null) {
            form.put(name, value.toString());
        }
    }

    private static <T> T unwrap(CarboneApiResponse<T> response, String action) throws CarboneException {
        if (response == null) {
            throw new CarboneException("Carbone SDK " + action + " error: something went wrong");
        }
        if (!response.isSuccess()) {
            throw apiError(response.getError(), response.getCode(), action);
        }
        return response.getData();
    }

    private static CarboneException apiError(String error, String code, String action) {
        if (error == null) {
            return new CarboneException("Carbone SDK " + action + " error: request failed without error message");
        }
        return new CarboneException(CarboneResponse.builder().success(false).error(error).code(code).build(), 200);
    }

    private static List<String> names(List<Map<String, String>> items) {
        if (items == null) {
            return Collections.emptyList();
        }
        return items.stream().map(item -> item.get("name")).collect(Collectors.toList());
    }

    @Override
    public String getStatus() throws CarboneException
    {
        Response response = null;
        try {
            response = carboneStatusClient.getStatus();
            InputStream bodyIs = response.body().asInputStream();
            String body = new String(bodyIs.readAllBytes(), StandardCharsets.UTF_8);
            return body;
        } catch (IOException e) {
            throw new CarboneException("Error reading response body");
        } catch (FeignException e) {
            throw new CarboneException("Feign exception occurred");
        } finally {
            if (response != null && response.body() != null) {
                try {
                    response.body().close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
