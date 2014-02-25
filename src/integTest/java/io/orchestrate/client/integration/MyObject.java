package io.orchestrate.client.integration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * A POJO used to test deserializing JSON in client.
 */
@ToString
@EqualsAndHashCode
@JsonIgnoreProperties(ignoreUnknown=false)
public final class MyObject {

    /** Some data field. */
    private final String aField;

    public MyObject(@JsonProperty("a_field") final String aField) {
        this.aField = aField;
    }

}
