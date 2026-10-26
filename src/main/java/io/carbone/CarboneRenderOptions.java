package io.carbone;

import java.util.LinkedHashMap;
import java.util.Map;

import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

/**
 * Per-request headers for a render.
 * <pre>
 * CarboneRenderOptions.builder()
 *     .webhookUrl("https://example.com/webhook")
 *     .webhookHeader("authorization", "Bearer secret")
 *     .build();
 * </pre>
 */
@Getter
@Builder
public class CarboneRenderOptions {

    /** Enables asynchronous rendering: Carbone calls this URL with the render ID once the document is generated. */
    private final String webhookUrl;
    /** Headers Carbone adds to the webhook call, sent as {@code carbone-webhook-header-<name>}. */
    @Singular
    private final Map<String, String> webhookHeaders;
    /** Any other request header, for instance {@code carbone-egress-header-authorization}. */
    @Singular
    private final Map<String, String> headers;

    static Map<String, Object> toHeaders(CarboneRenderOptions options) {
        Map<String, Object> headers = new LinkedHashMap<>();
        if (options == null) {
            return headers;
        }
        headers.putAll(options.headers);
        if (options.webhookUrl != null) {
            headers.put("carbone-webhook-url", options.webhookUrl);
        }
        options.webhookHeaders.forEach((name, value) -> headers.put("carbone-webhook-header-" + name, value));
        return headers;
    }
}
