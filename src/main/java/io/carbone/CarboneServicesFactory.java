package io.carbone;

import feign.gson.GsonEncoder;
import feign.slf4j.Slf4jLogger;

public enum CarboneServicesFactory {

    CARBONE_SERVICES_FACTORY_INSTANCE;

    private static String CARBONE_URL = "https://api.carbone.io";

    private CarboneTemplateClient carboneTemplateClient;

    private CarboneRenderClient carboneRenderClient;

    private CarboneStatusClient carboneStatusClient;

    public ICarboneServices create(String ... config) {
        String apiToken = System.getenv("CARBONE_TOKEN");
        String apiVersion = "5";
        if (config.length > 0 && !config[0].isEmpty()) {
            apiToken = config[0];
        }
        
        if (config.length > 1 && !config[1].isEmpty()) {
            apiVersion = config[1];
        }
        carboneTemplateClient = CarboneFeignClientBuilder.createCarboneFeignClient(apiToken, apiVersion)
            .encoder(CarboneFormFieldWriter.formEncoder())
            .decoder(new CarboneDecoder())
            .logger(new Slf4jLogger(CarboneTemplateClient.class))
            .target(CarboneTemplateClient.class, CARBONE_URL);

        carboneRenderClient = CarboneFeignClientBuilder.createCarboneFeignClient(apiToken, apiVersion)
            .encoder(new GsonEncoder())
            .decoder(new CarboneDecoder())
            .logger(new Slf4jLogger(CarboneRenderClient.class))
            .target(CarboneRenderClient.class, CARBONE_URL + "/render");

        carboneStatusClient = CarboneFeignClientBuilder.createCarboneFeignClient(apiToken, apiVersion)
            .encoder(new GsonEncoder())
            .decoder(new CarboneDecoder())
            .logger(new Slf4jLogger(CarboneStatusClient.class))
            .target(CarboneStatusClient.class, CARBONE_URL);

        return create(carboneTemplateClient, carboneRenderClient, carboneStatusClient);
    }


    public ICarboneServices create(ICarboneTemplateClient carboneTemplateClient, ICarboneRenderClient carboneRenderClient, ICarboneStatusClient carboneStatusClient ) {
        return new CarboneServices(carboneTemplateClient, carboneRenderClient, carboneStatusClient);
    }

    public void SetCarboneUrl(String newUrl) throws CarboneException {CARBONE_URL = newUrl;}

    public String GetCarboneUrl() {return CARBONE_URL;}
}
