
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

package br.com.sawcunhaos.organization.shared.utils;

import br.com.sawcunhaos.foundation.utils.exception.ScosException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_SECURITY_DECRYPT;
import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_SECURITY_ENCRYPT;

@Component
@Slf4j
public class SystemSecretCryptoService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";

    @Value("${scos.security.master-key}")
    private String masterKeyBase64;
    @Value("${scos.security.iv-length:12}")
    private int ivLength = 12;
    @Value("${scos.security.tag-length:128}")
    private int tagLength = 128;


    private SecretKeySpec secretKey;

    @PostConstruct
    void init() {

        byte[] key = Base64.getDecoder().decode(masterKeyBase64);

        if (key.length != 32) {
            log.error("master-key deve possuir exatamente 32 bytes");
            throw new IllegalStateException(
                    "master-key deve possuir exatamente 32 bytes");
        }

        if (ivLength < 12) {
            log.error("O Valor do iv-length deve ser maior ou igual a 12");
            throw new IllegalStateException(
                    "O Valor do iv-length deve ser maior ou igual a 12");
        }

        if (tagLength < 128) {
            log.error("O Valor do tag-length deve ser maior ou igual a 128");
            throw new IllegalStateException(
                    "O Valor do tag-length deve ser maior ou igual a 128");
        }

        this.secretKey = new SecretKeySpec(key, "AES");
    }

    public String encrypt(String plain) {

        try {
            SecureRandom secureRandom = new SecureRandom();

            byte[] iv = new byte[ivLength];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);

            cipher.init(
                    Cipher.ENCRYPT_MODE,
                    secretKey,
                    new GCMParameterSpec(tagLength, iv));

            byte[] encrypted =
                    cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));

            byte[] result = new byte[iv.length + encrypted.length];

            System.arraycopy(iv, 0, result, 0, iv.length);
            System.arraycopy(encrypted, 0, result, iv.length, encrypted.length);

            return Base64.getEncoder().encodeToString(result);

        } catch (Exception ex) {
            log.error("Erro ao criptografar: {}", ex.getMessage());
            throw new ScosException(SCOS_SECURITY_ENCRYPT);
        }
    }

    public String decrypt(String stored) {

        try {

            byte[] data = Base64.getDecoder().decode(stored);

            byte[] iv = Arrays.copyOfRange(data, 0, ivLength);

            byte[] encrypted =
                    Arrays.copyOfRange(data, ivLength, data.length);

            Cipher cipher = Cipher.getInstance(ALGORITHM);

            cipher.init(
                    Cipher.DECRYPT_MODE,
                    secretKey,
                    new GCMParameterSpec(tagLength, iv));

            byte[] decrypted = cipher.doFinal(encrypted);

            return new String(decrypted, StandardCharsets.UTF_8);

        } catch (Exception ex) {
            log.error("Erro ao descriptografar: {}", ex.getMessage());
            throw new ScosException(SCOS_SECURITY_DECRYPT);
        }
    }



}
