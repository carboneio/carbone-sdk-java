package io.carbone;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public interface ICarboneServices {

    /**
     * Upload template to Carbone
     * @param templateFile file's content as byte[]
     * @return the template ID
     * @throws CarboneException contain CarboneResponse format with API error code and error messages
     */
    String addTemplate(byte[] templateFile) throws CarboneException;

     /**
     * Upload template to Carbone
     * @param templatePath path of the template as String
     * @return the template ID
     * @throws CarboneException contain CarboneResponse format with API error code and error messages
     * @throws IOException 
     */
    String addTemplate(String templatePath) throws CarboneException, IOException;

    /**
     * Delete uploaded template
     * @param templateId id returned by addTemplate() method. A Template ID deletes all its versions, a Version ID deletes only that version
     * @throws CarboneException contain CarboneResponse format with API error code and error messages
     */
    boolean deleteTemplate(String templateId) throws CarboneException;


    /**
     * Download rendered report
     * @param templateId id returned by renderReport()
     * @return report content in byte[]
     * @throws CarboneException contain CarboneResponse format with API error code and error messages
     */
    byte[] getTemplate(String templateId) throws CarboneException;

    /**
     * Render report
     * @param json Json object with data set to replace in template
     * @param templateId id returned by addTemplate() method
     * @return id of rendered report
     * @throws CarboneException contain CarboneResponse format with API error code and error messages
     */
    String renderReport(String json, String templateId) throws CarboneException;

    /**
     * Download rendered report
     * @param renderId id returned by renderReport()
     * @return report content in CarboneDocument
     * @throws CarboneException contain CarboneResponse format with API error code and error messages
     */
    CarboneDocument getReport(String renderId) throws CarboneException;


    /**
     * Give the status
     * @return report the status of the API as a JSON: {"success":true,"code":200,"message":"OK","version":"4.22.8"}
     * @throws CarboneException contain CarboneResponse format with API error code and error messages
     */
    String getStatus() throws CarboneException;

    /**
     * render report
     * @param Json JSON dataset injected into the template, to generate the document
     * @param fileOrTemplateID id returned by addTemplate() method or the id of document of studio or a local path
     * @return report content in CarboneDocument
     * @throws CarboneException contain CarboneResponse format with API error code and error messages
     */
    CarboneDocument render(String Json, String fileOrTemplateID) throws CarboneException;

     /**
     * precalculates the template id 
     * @param path local path to the template file
     * @return give a precalculates templateId
     */
    String generateTemplateId(String path);

    /**
     * Upload a template with metadata and versioning options
     * @param templateFile file's content as byte[]
     * @param options versioning, Template ID, name, tags, category... (nullable)
     * @return the uploaded template: id and versionId when versioning is enabled, templateId otherwise
     * @throws CarboneException contain CarboneResponse format with API error code and error messages
     */
    CarboneTemplate addTemplate(byte[] templateFile, CarboneTemplateOptions options) throws CarboneException;

    /**
     * Upload a template with metadata and versioning options
     * @param templatePath path of the template as String
     * @param options versioning, Template ID, name, tags, category... (nullable)
     * @return the uploaded template: id and versionId when versioning is enabled, templateId otherwise
     * @throws CarboneException contain CarboneResponse format with API error code and error messages
     * @throws IOException the template file can't be read
     */
    default CarboneTemplate addTemplate(String templatePath, CarboneTemplateOptions options) throws CarboneException, IOException {
        return addTemplate(Files.readAllBytes(Paths.get(templatePath)), options);
    }

    /**
     * Update template metadata
     * @param templateIdOrVersionId a Template ID updates the deployed version, a Version ID updates that version
     * @param update fields to update, at least one
     * @return the updated version: versionId and the updated fields
     * @throws CarboneException contain CarboneResponse format with API error code and error messages
     */
    CarboneTemplate updateTemplate(String templateIdOrVersionId, CarboneTemplateUpdate update) throws CarboneException;

    /**
     * List deployed templates, most recent first
     * @param query filters and pagination (nullable)
     * @return one page of templates
     * @throws CarboneException contain CarboneResponse format with API error code and error messages
     */
    CarboneTemplateList listTemplates(CarboneTemplateQuery query) throws CarboneException;

    /**
     * List deployed templates, most recent first (first page, up to 100 templates)
     * @return one page of templates
     * @throws CarboneException contain CarboneResponse format with API error code and error messages
     */
    default CarboneTemplateList listTemplates() throws CarboneException {
        return listTemplates(null);
    }

    /**
     * List the categories of deployed templates
     * @return category names, sorted
     * @throws CarboneException contain CarboneResponse format with API error code and error messages
     */
    List<String> listTemplateCategories() throws CarboneException;

    /**
     * List the tags of deployed templates
     * @return tag names, sorted
     * @throws CarboneException contain CarboneResponse format with API error code and error messages
     */
    List<String> listTemplateTags() throws CarboneException;

    /**
     * Render report with request headers, for instance an asynchronous render with a webhook
     * @param json Json object with data set to replace in template
     * @param templateIdOrVersionId Template ID or Version ID
     * @param options webhook and request headers (nullable)
     * @return id of rendered report, or null with a webhook: the render ID is sent to the webhook URL
     * @throws CarboneException contain CarboneResponse format with API error code and error messages
     */
    String renderReport(String json, String templateIdOrVersionId, CarboneRenderOptions options) throws CarboneException;

    /**
     * Render report and download it in a single request, the report is not stored by Carbone
     * @param json Json object with data set to replace in template
     * @param templateIdOrVersionId Template ID or Version ID
     * @return report content in CarboneDocument
     * @throws CarboneException contain CarboneResponse format with API error code and error messages
     */
    default CarboneDocument renderAndDownload(String json, String templateIdOrVersionId) throws CarboneException {
        return renderAndDownload(json, templateIdOrVersionId, null);
    }

    /**
     * Render report and download it in a single request, the report is not stored by Carbone
     * @param json Json object with data set to replace in template
     * @param templateIdOrVersionId Template ID or Version ID
     * @param options request headers (nullable)
     * @return report content in CarboneDocument
     * @throws CarboneException contain CarboneResponse format with API error code and error messages
     */
    CarboneDocument renderAndDownload(String json, String templateIdOrVersionId, CarboneRenderOptions options) throws CarboneException;

    /**
     * Render a template sent with the request and download the report, neither is stored by Carbone
     * @param json Json object with data set to replace in template
     * @param templateFile template content as byte[]
     * @return report content in CarboneDocument
     * @throws CarboneException contain CarboneResponse format with API error code and error messages
     */
    default CarboneDocument renderInline(String json, byte[] templateFile) throws CarboneException {
        return renderInline(json, templateFile, null);
    }

    /**
     * Render a template sent with the request and download the report, neither is stored by Carbone
     * @param json Json object with data set to replace in template
     * @param templateFile template content as byte[]
     * @param options request headers (nullable)
     * @return report content in CarboneDocument
     * @throws CarboneException contain CarboneResponse format with API error code and error messages
     */
    CarboneDocument renderInline(String json, byte[] templateFile, CarboneRenderOptions options) throws CarboneException;
}
