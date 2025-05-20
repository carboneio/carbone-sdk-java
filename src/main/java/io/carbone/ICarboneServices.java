package io.carbone;

import java.io.IOException;

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
     * @param templateId id returned by addTemplate() method
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
     * precalculates the template id
     * @param fileBytes byte content of the template file
     * @return give a precalculates templateId
     */
    String generateTemplateId(byte[] fileBytes);
    
}
