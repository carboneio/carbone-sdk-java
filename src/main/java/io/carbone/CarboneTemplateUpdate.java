package io.carbone;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Template metadata to update. Only non-null fields are sent, at least one is required.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CarboneTemplateUpdate {

    /** Moves the version to another Template ID (64-bit). */
    private String id;
    private String name;
    private String comment;
    private String category;
    private List<String> tags;
    /** UTC Unix timestamp in seconds, cannot be earlier than the current value. */
    private Long deployedAt;
    /** UTC Unix timestamp in seconds, 0 cancels a scheduled deletion. */
    private Long expireAt;
}
