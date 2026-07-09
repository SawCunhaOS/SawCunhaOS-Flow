
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

package br.com.sawcunhaos.organization.boot.infrastructure;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * Emissão de JWT para os testes integrados com o Keycloak mockado (WireMock).
 *
 * <p>Gera UM par RSA em memória (uma vez por JVM), expõe o público como JWKS
 * ({@link #publicJwks()}) que o WireMock serve no endpoint {@code /certs}, e assina
 * tokens ({@link #bearer(String)}) com esse par. Assim o resource server valida a
 * assinatura contra o JWKS mockado, com {@code iss} = {@link #ISSUER} e {@code exp}
 * ~100 anos (efetivamente infinito para o teste).
 *
 * <p>{@code iss} DEVE bater com {@code spring.security.oauth2.resourceserver.jwt.issuer-uri}
 * (default de teste: realm {@code Scos}). O {@code preferred_username} é o login lido
 * pelo {@code JwtAuthConverter}.
 */
public final class ScosJwtTestSupport {

    /** Deve ser igual ao issuer-uri configurado (realm Scos). */
    public static final String ISSUER = "http://localhost:7080/realms/Scos";

    private static final String KID = "scos-test-key";
    private static final RSAKey RSA_KEY = generate(KID);
    /** Chave paralela, ausente do JWKS — assina tokens que DEVEM falhar (401). */
    private static final RSAKey FOREIGN_KEY = generate("scos-foreign-key");

    private ScosJwtTestSupport() {
    }

    private static RSAKey generate(String kid) {
        try {
            return new RSAKeyGenerator(2048)
                    .keyID(kid)
                    .keyUse(KeyUse.SIGNATURE)
                    .algorithm(JWSAlgorithm.RS256)
                    .generate();
        } catch (JOSEException e) {
            throw new IllegalStateException("Falha ao gerar chave RSA de teste", e);
        }
    }

    /** JWKS público (só a chave de assinatura válida) — resposta do endpoint /certs. */
    public static String publicJwks() {
        return new JWKSet(RSA_KEY.toPublicJWK()).toString();
    }

    /** Bearer válido (exp ~100 anos) para o login informado. */
    public static String bearer(String preferredUsername) {
        return "Bearer " + sign(RSA_KEY, preferredUsername, Instant.now().plus(Duration.ofDays(36500)));
    }

    /** Bearer assinado por chave fora do JWKS → resource server rejeita (401). */
    public static String invalidBearer() {
        return "Bearer " + sign(FOREIGN_KEY, "intruder", Instant.now().plus(Duration.ofDays(1)));
    }

    /** Bearer expirado (assinatura válida, mas exp no passado) → 401. */
    public static String expiredBearer(String preferredUsername) {
        return "Bearer " + sign(RSA_KEY, preferredUsername, Instant.now().minus(Duration.ofMinutes(5)));
    }

    private static String sign(RSAKey key, String preferredUsername, Instant expiration) {
        try {
            Instant now = Instant.now();
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .issuer(ISSUER)
                    .subject(UUID.randomUUID().toString())
                    .claim("preferred_username", preferredUsername)
                    .claim("name", preferredUsername + " " + preferredUsername)
                    .claim("email", preferredUsername + "@scos.local")
                    .issueTime(Date.from(now))
                    .expirationTime(Date.from(expiration))
                    .build();

            SignedJWT jwt = new SignedJWT(
                    new JWSHeader.Builder(JWSAlgorithm.RS256)
                            .keyID(key.getKeyID())
                            .type(JOSEObjectType.JWT)
                            .build(),
                    claims);
            jwt.sign(new RSASSASigner(key.toPrivateKey()));
            return jwt.serialize();
        } catch (JOSEException e) {
            throw new IllegalStateException("Falha ao assinar JWT de teste", e);
        }
    }
}
