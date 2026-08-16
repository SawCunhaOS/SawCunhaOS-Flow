
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

package br.com.sawcunhaos.organization.domain.access.login.dto;

import br.com.sawcunhaos.organization.domain.access.login.internal.LoginStatus;
import br.com.sawcunhaos.organization.domain.access.login.internal.LoginType;
import lombok.Builder;

import java.util.UUID;

/**
 * Retorno de leitura de {@code Login}. {@code profileCode}/{@code profileDescription}/{@code employeeName}
 * são nullable de propósito — só {@code findById} os preenche (1 linha, custo irrelevante de inicializar
 * 2 proxies LAZY). {@code findAll} (paginado) os deixa {@code null}: ninguém consome esses 3 campos no
 * caminho de listagem ({@code toApiLoginSummary}), então lê-los por linha de uma página seria N+1 sem
 * consumidor — mesmo padrão de {@code EmployeeServiceBean.toEmployeeOutput}, que só lê {@code .getId()}
 * de relação LAZY num mapper de lista.
 */
@Builder
public record LoginOutput(
        Long id,
        String login,
        UUID externalId,
        LoginType type,
        LoginStatus status,
        Long profileId,
        Long employeeId,
        String profileCode,
        String profileDescription,
        String employeeName
) {
}
