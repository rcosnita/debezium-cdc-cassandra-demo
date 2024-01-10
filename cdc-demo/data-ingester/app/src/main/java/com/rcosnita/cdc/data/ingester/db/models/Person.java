package com.rcosnita.cdc.data.ingester.db.models;

import com.datastax.oss.driver.api.mapper.annotations.CqlName;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.UUID;

/**
 * Provides the model for describing a Person entity stored in a durable manner.
 */
public class Person implements Serializable {
    private static final Long serialVersionUID = 1L;

    @CqlName("id")
    protected UUID id;

    @CqlName("content")
    protected ByteBuffer content;

    @CqlName("creation_time")
    protected Instant createdTime;

    public Person() {
    }

    public Person(UUID id, ByteBuffer content, Instant createdTime) {
        this.id = id;
        this.content = content;
        this.createdTime = createdTime;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public ByteBuffer getContent() {
        return content;
    }

    public void setContent(ByteBuffer content) {
        this.content = content;
    }

    public Instant getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(Instant createdTime) {
        this.createdTime = createdTime;
    }
}
