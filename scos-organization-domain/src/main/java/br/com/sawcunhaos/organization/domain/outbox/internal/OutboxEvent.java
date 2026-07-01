
/*
 *
 *  * Copyright 2026 SawCunha Open System - SawCunhaOS-Organization
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *
 */

package br.com.sawcunhaos.organization.domain.outbox.internal;

import br.com.sawcunhaos.foundation.utils.annotation.audit.Auditable;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;

import java.time.LocalDateTime;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "SCOS_OUTBOX_EVENT")
@Auditable
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "OUTBOX_EVENT_ID")
    private Long id;

    @Column(name = "EVENT_HASH")
    private String eventHash;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TOPIC")
    private OutboxTopic topic;

    @Column(name = "AGGREGATE_ID")
    private String aggregateId;

    @Type(JsonBinaryType.class)
    @Column(name = "PAYLOAD")
    private String payload;
    @Type(JsonBinaryType.class)
    @Column(name = "RESPONSE_DATA")
    private String responseData;

    @Column(name = "STATUS")
    @Enumerated(EnumType.STRING)
    private OutboxEventStatus status;
    @Column(name = "RETRY_COUNT")
    private int retryCount;
    @Column(name = "MAX_RETRIES")
    private int maxRetries;
    @Column(name = "MESSAGE")
    private String message;
    @Column(name = "REQUESTING")
    private String requesting;

    @Column(name = "STARTED_AT")
    private LocalDateTime startedAt;
    @Column(name = "PROCESSED_AT")
    private LocalDateTime processedAt;

    @CreationTimestamp
    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;
    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;
    @Column(name = "USER_AT")
    private String userAt;
}
