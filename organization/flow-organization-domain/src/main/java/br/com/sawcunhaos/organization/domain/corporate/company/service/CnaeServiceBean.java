
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

package br.com.sawcunhaos.organization.domain.corporate.company.service;

import br.com.sawcunhaos.foundation.utils.exception.ScosException;
import br.com.sawcunhaos.organization.domain.corporate.company.dto.CnaeInput;
import br.com.sawcunhaos.organization.domain.corporate.company.dto.CnaeOutput;
import br.com.sawcunhaos.organization.domain.corporate.company.internal.Cnae;
import br.com.sawcunhaos.organization.domain.corporate.company.internal.CnaeRepository;
import br.com.sawcunhaos.organization.domain.corporate.company.internal.CompanyCnaeSecondaryRepository;
import br.com.sawcunhaos.organization.domain.corporate.company.internal.CompanyRepository;
import br.com.sawcunhaos.organization.domain.corporate.company.specification.CnaeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_CNAE_001;
import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_CNAE_002;
import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_CNAE_003;

/**
 * Implementação de {@link CnaeService} — unicidade de {@code code} e guarda de exclusão física
 * (bloqueia remoção de CNAE ainda vinculado a empresa como principal ou secundário).
 */
@Service
@RequiredArgsConstructor
@Slf4j
class CnaeServiceBean implements CnaeService {

    private final CnaeRepository cnaeRepository;
    private final CompanyRepository companyRepository;
    private final CompanyCnaeSecondaryRepository companyCnaeSecondaryRepository;
    private final CnaeMapper cnaeMapper;

    /**
     * @throws ScosException SCOS_CNAE_002 se já existir um {@link Cnae} com o mesmo {@code code}.
     */
    @Override
    @Transactional(rollbackFor = ScosException.class)
    public CnaeOutput create(@NonNull CnaeInput cnaeInput) {
        log.info("Create Cnae: {}", cnaeInput.code());

        if (cnaeRepository.existsByCode(cnaeInput.code())) {
            throw new ScosException(SCOS_CNAE_002);
        }

        Cnae cnae = Cnae.builder()
                .code(cnaeInput.code())
                .description(cnaeInput.description())
                .build();

        cnae = cnaeRepository.merge(cnae);

        return cnaeMapper.toCnaeOutput(cnae);
    }

    /**
     * @throws ScosException SCOS_CNAE_001 se o {@code id} não existir.
     * @throws ScosException SCOS_CNAE_002 se o novo {@code code} colidir com outro registro.
     */
    @Override
    @Transactional(rollbackFor = ScosException.class)
    public void update(@NonNull CnaeInput cnaeInput) {
        log.info("Update Cnae: {}", cnaeInput.id());
        Cnae cnae = findCnaeById(cnaeInput.id());

        if (cnaeRepository.existsByCodeAndNotId(cnaeInput.code(), cnaeInput.id())) {
            throw new ScosException(SCOS_CNAE_002);
        }

        cnae.setCode(cnaeInput.code());
        cnae.setDescription(cnaeInput.description());
        cnaeRepository.update(cnae);
    }

    /**
     * @throws ScosException SCOS_CNAE_001 se o {@code id} não existir.
     */
    @Override
    @Transactional(readOnly = true)
    public CnaeOutput findById(@NonNull Long cnaeId) {
        log.info("Find Cnae by Id: {}", cnaeId);
        return cnaeMapper.toCnaeOutput(findCnaeById(cnaeId));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CnaeOutput> findAll(@NonNull Pageable pageable) {
        log.info("Find All Cnaes");
        return cnaeRepository.findAll(pageable).map(cnaeMapper::toCnaeOutput);
    }

    /**
     * @throws ScosException SCOS_CNAE_001 se o {@code id} não existir.
     * @throws ScosException SCOS_CNAE_003 se o CNAE estiver vinculado a alguma empresa (principal ou secundário).
     */
    @Override
    @Transactional(rollbackFor = ScosException.class)
    public void delete(@NonNull Long cnaeId) {
        log.info("Delete Cnae: {}", cnaeId);
        Cnae cnae = findCnaeById(cnaeId);

        if (companyRepository.existsByCnaePrincipalId(cnaeId)
                || companyCnaeSecondaryRepository.existsByCnaeId(cnaeId)) {
            throw new ScosException(SCOS_CNAE_003);
        }

        cnaeRepository.delete(cnae);
        log.info("Cnae deleted");
    }

    @Override
    @Transactional(readOnly = true)
    public Cnae findCnaeById(@NonNull Long cnaeId) {
        return cnaeRepository.findById(cnaeId).orElseThrow(
                () -> new ScosException(SCOS_CNAE_001)
        );
    }
}
