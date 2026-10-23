package io.carbone;

import static feign.Logger.Level.NONE;

import feign.Feign;
import feign.http2client.Http2Client;

class CarboneFeignClientBuilder {

    private CarboneFeignClientBuilder(){}

    static Feign.Builder createCarboneFeignClient(String token, String version) {
        return Feign.builder()
            .client(new Http2Client())
            .errorDecoder(new CarboneErrorDecoder())
            .logLevel(NONE)
            .requestInterceptor(template -> {
                template.header("Authorization", "Bearer " + token);
                template.header("carbone-version", version);
            });
    }
}
