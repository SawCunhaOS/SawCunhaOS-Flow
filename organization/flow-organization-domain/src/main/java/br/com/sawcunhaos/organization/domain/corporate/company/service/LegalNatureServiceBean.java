
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
import br.com.sawcunhaos.organization.domain.corporate.company.dto.LegalNatureInput;
import br.com.sawcunhaos.organization.domain.corporate.company.dto.LegalNatureOutput;
import br.com.sawcunhaos.organization.domain.corporate.company.internal.CompanyRepository;
import br.com.sawcunhaos.organization.domain.corporate.company.internal.LegalNature;
import br.com.sawcunhaos.organization.domain.corporate.company.internal.LegalNatureRepository;
import br.com.sawcunhaos.organization.domain.corporate.company.specification.LegalNatureService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_LEGAL_NATURE_001;
import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_LEGAL_NATURE_002;
import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_LEGAL_NATURE_003;

/**
 * Implementação de {@link LegalNatureService} — unicidade de {@code code} e guarda de exclusão física
 * (bloqueia remoção de natureza jurídica ainda vinculada a empresa via {@code legalNatureId}).
 */
@Service
@RequiredArgsConstructor
@Slf4j
class LegalNatureServiceBean implements LegalNatureService {

    private final LegalNatureRepository legalNatureRepository;
    private final CompanyRepository companyRepository;
    private final LegalNatureMapper legalNatureMapper;

    /**
     * @throws ScosException SCOS_LEGAL_NATURE_002 se já existir uma {@link LegalNature} com o mesmo {@code code}.
     */
    @Override
    @Transactional(rollbackFor = ScosException.class)
    public LegalNatureOutput create(@NonNull LegalNatureInput legalNatureInput) {
        log.info("Create LegalNature: {}", legalNatureInput.code());

        if (legalNatureRepository.existsByCode(legalNatureInput.code())) {
            throw new ScosException(SCOS_LEGAL_NATURE_002);
        }

        LegalNature legalNature = LegalNature.builder()
                .code(legalNatureInput.code())
                .description(legalNatureInput.description())
                .build();

        legalNature = legalNatureRepository.merge(legalNature);

        return legalNatureMapper.toLegalNatureOutput(legalNature);
    }

    /**
     * @throws ScosException SCOS_LEGAL_NATURE_001 se o {@code id} não existir.
     * @throws ScosException SCOS_LEGAL_NATURE_002 se o novo {@code code} colidir com outro registro.
     */
    @Override
    @Transactional(rollbackFor = ScosException.class)
    public void update(@NonNull LegalNatureInput legalNatureInput) {
        log.info("Update LegalNature: {}", legalNatureInput.id());
        LegalNature legalNature = findLegalNatureById(legalNatureInput.id());

        if (legalNatureRepository.existsByCodeAndNotId(legalNatureInput.code(), legalNatureInput.id())) {
            throw new ScosException(SCOS_LEGAL_NATURE_002);
        }

        legalNature.setCode(legalNatureInput.code());
        legalNature.setDescription(legalNatureInput.description());
        legalNatureRepository.update(legalNature);
    }

    /**
     * @throws ScosException SCOS_LEGAL_NATURE_001 se o {@code id} não existir.
     */
    @Override
    @Transactional(readOnly = true)
    public LegalNatureOutput findById(@NonNull Long legalNatureId) {
        log.info("Find LegalNature by Id: {}", legalNatureId);
        return legalNatureMapper.toLegalNatureOutput(findLegalNatureById(legalNatureId));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LegalNatureOutput> findAll(@NonNull Pageable pageable) {
        log.info("Find All LegalNatures");
        return legalNatureRepository.findAll(pageable).map(legalNatureMapper::toLegalNatureOutput);
    }

    /**
     * @throws ScosException SCOS_LEGAL_NATURE_001 se o {@code id} não existir.
     * @throws ScosException SCOS_LEGAL_NATURE_003 se a natureza jurídica estiver vinculada a alguma empresa.
     */
    @Override
    @Transactional(rollbackFor = ScosException.class)
    public void delete(@NonNull Long legalNatureId) {
        log.info("Delete LegalNature: {}", legalNatureId);
        LegalNature legalNature = findLegalNatureById(legalNatureId);

        if (companyRepository.existsByLegalNatureId(legalNatureId)) {
            throw new ScosException(SCOS_LEGAL_NATURE_003);
        }

        legalNatureRepository.delete(legalNature);
        log.info("LegalNature deleted");
    }

    @Override
    @Transactional(readOnly = true)
    public LegalNature findLegalNatureById(@NonNull Long legalNatureId) {
        return legalNatureRepository.findById(legalNatureId).orElseThrow(
                () -> new ScosException(SCOS_LEGAL_NATURE_001)
        );
    }
}
