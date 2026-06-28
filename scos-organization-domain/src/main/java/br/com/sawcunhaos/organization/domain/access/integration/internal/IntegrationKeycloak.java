
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

package br.com.sawcunhaos.organization.domain.access.integration.internal;

import br.com.sawcunhaos.foundation.utils.annotation.audit.Auditable;
import br.com.sawcunhaos.foundation.utils.entity.BaseEntity;
import br.com.sawcunhaos.foundation.utils.valueobjects.Email;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Type;

import java.time.OffsetDateTime;
import java.util.UUID;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "SCOS_INTEGRATION_KEYCLOAK")
@Auditable(auditRead = true)
public class IntegrationKeycloak extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "INTEGRATION_KEYCLOAK_ID")
    private UUID id;

    @Column(name = "KEYCLOAK_ID")
    private UUID keycloakId;
    @Column(name = "REALM")
    private String realm;
    @Embedded
    @AttributeOverride(name = "email", column = @Column(name = "EMAIL"))
    private Email email;
    @Column(name = "USERNAME")
    private String username;
    @Column(name = "REQUESTING")
    private String requesting;
    @Column(name = "TYPE")
    private String type;
    @Column(name = "STATUS")
    private String status;
    @Column(name = "DATE_START")
    private OffsetDateTime dateStart;
    @Column(name = "DATE_END")
    private OffsetDateTime dateEnd;
    @Column(name = "MESSAGE")
    private String message;
    @Type(JsonBinaryType.class)
    @Column(name = "REQUEST", columnDefinition = "jsonb")
    private String request;
    @Column(name = "RETRY_COUNT")
    private int retryCount;
    @Column(name = "MAX_RETRIES")
    private int maxRetries;
}
