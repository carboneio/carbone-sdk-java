package io.carbone;

import feign.form.FormEncoder;
import feign.gson.GsonEncoder;
import feign.slf4j.Slf4jLogger;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;

public enum CarboneServicesFactory {

    CARBONE_SERVICES_FACTORY_INSTANCE;

    private static String CARBONE_URI = "https://api.carbone.io";

    public ICarboneServices create(String token, String version) {
        String apiToken = System.getenv("CARBONE_TOKEN");
        if( token != "")
        {
            apiToken = token;
        }

         version = "4";
        var carboneTemplateClient = CarboneFeignClientBuilder.createCarboneFeignClient(apiToken, version)
            .encoder(new FormEncoder())
            .decoder(new CarboneDecoder())
            .logger(new Slf4jLogger(CarboneTemplateClient.class))
            .target(CarboneTemplateClient.class, CARBONE_URI + "/template");

        var carboneRenderClient = CarboneFeignClientBuilder.createCarboneFeignClient(apiToken, version)
            .encoder(new GsonEncoder())
            .decoder(new CarboneDecoder())
            .logger(new Slf4jLogger(CarboneRenderClient.class))
            .target(CarboneRenderClient.class, CARBONE_URI + "/render");

        var carboneStatusClient = CarboneFeignClientBuilder.createCarboneFeignClient(apiToken, version)
            .encoder(new JacksonEncoder())
            .decoder(new JacksonDecoder())
            .logger(new Slf4jLogger(CarboneStatusClient.class))
            .target(CarboneStatusClient.class, CARBONE_URI);

        return create(carboneTemplateClient, carboneRenderClient, carboneStatusClient);
    }


    public ICarboneServices create(ICarboneTemplateClient carboneTemplateClient, ICarboneRenderClient carboneRenderClient, ICarboneStatusClient carboneStatusClient ) {
        return new CarboneServices(carboneTemplateClient, carboneRenderClient, carboneStatusClient);
    }

    public static void SetCarbonneUri(String newUrl) {CARBONE_URI = newUrl;}
}