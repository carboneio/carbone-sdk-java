package io.carbone;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Optional fields sent with a template upload. Only non-null fields are sent.
 * See https://carbone.io/documentation/developer/http-api/manage-templates.html
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CarboneTemplateOptions {

    /** {@code deployedAt} value meaning "now": deploys the version when it is uploaded. */
    public static final long DEPLOY_NOW = 42000000000L;

    /** {@code true} creates a Template ID, or adds a new version to {@code id} if set. */
    private Boolean versioning;
    /** Existing Template ID (64-bit) the new version belongs to. A Version ID is rejected by the API. */
    private String id;
    private String name;
    private String comment;
    private String category;
    private List<String> tags;
    /** Sample data as a JSON array string: {@code [{"data":{},"complement":{},"translations":{},"enum":{}}]}. */
    private String sample;
    /**
     * UTC Unix timestamp in seconds, {@code DEPLOY_NOW} deploys the version now. Required to add a version
     * to an existing Template ID: it must differ from the deployedAt of the currently deployed version.
     */
    private Long deployedAt;
    /** UTC Unix timestamp in seconds, 0 means no expiration. */
    private Long expireAt;
}
