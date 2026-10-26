package io.carbone;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Template metadata returned by the Carbone API (upload, update and list endpoints).
 * Fields not sent by the API are {@code null}.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CarboneTemplate {

    /** Template ID (64-bit), shared by all versions of a template. Set when versioning is enabled. */
    private String id;
    /** Version ID (SHA-256), identifies one specific version of a template. */
    private String versionId;
    /** Legacy template ID (SHA-256 of the file), returned when versioning is disabled. */
    private String templateId;
    /** Legacy template extension, returned when versioning is disabled. */
    private String templateExtension;
    private String type;
    private Long size;
    private String name;
    private String comment;
    private String category;
    private List<String> tags;
    private Integer origin;
    /** UTC Unix timestamp in seconds. */
    private Long createdAt;
    /** UTC Unix timestamp in seconds. */
    private Long deployedAt;
    /** UTC Unix timestamp in seconds, 0 means no expiration. */
    private Long expireAt;
}
