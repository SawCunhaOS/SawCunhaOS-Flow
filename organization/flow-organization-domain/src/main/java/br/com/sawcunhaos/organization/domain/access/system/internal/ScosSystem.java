
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

package br.com.sawcunhaos.organization.domain.access.system.internal;

import br.com.sawcunhaos.foundation.utils.annotation.audit.Auditable;
import br.com.sawcunhaos.foundation.utils.entity.BaseEntity;
import br.com.sawcunhaos.organization.shared.converter.SecretKeyConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.UUID;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "SCOS_SYSTEM")
@Auditable
@ToString(exclude = {"secretKey", "previousSecretKey"})
public class ScosSystem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "SYSTEM_ID")
    private UUID id;

    @Column(name = "NAME")
    private String name;
    @Column(name = "CODE")
    private String code;
    @Column(name = "DESCRIPTION")
    private String description;
    @Convert(converter = SecretKeyConverter.class)
    @Column(name = "SECRET_KEY")
    private String secretKey;
    @Convert(converter = SecretKeyConverter.class)
    @Column(name = "PREVIOUS_SECRET_KEY")
    private String previousSecretKey;
    @Column(name = "PREVIOUS_SECRET_EXPIRES_AT")
    private Instant previousSecretExpiresAt;
    @Column(name = "STATUS")
    private String status;
    @Column(name = "VERSION")
    private String version;

    @Transient
    private boolean updateRegistration = false;

    public String rotateSecret(String newRawSecret, Instant expiresAt) {
        this.previousSecretKey = this.secretKey;
        this.secretKey         = newRawSecret;
        this.previousSecretExpiresAt   = expiresAt;
        return newRawSecret;
    }

    public boolean matchesSecret(String provided, Instant now) {
        if (constantTimeEquals(secretKey, provided)) return true;
        boolean inGrace = previousSecretExpiresAt != null && now.isBefore(previousSecretExpiresAt);
        return inGrace && constantTimeEquals(previousSecretKey, provided);
    }

    private static boolean constantTimeEquals(String secretKey, String secretKeyCompare) {
        return secretKey != null && secretKeyCompare != null && MessageDigest.isEqual(
                secretKey.getBytes(StandardCharsets.UTF_8), secretKeyCompare.getBytes(StandardCharsets.UTF_8));
    }
}
