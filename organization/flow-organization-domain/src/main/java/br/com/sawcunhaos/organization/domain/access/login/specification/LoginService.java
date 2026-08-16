
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

package br.com.sawcunhaos.organization.domain.access.login.specification;

import br.com.sawcunhaos.organization.domain.access.login.dto.LoginInput;
import br.com.sawcunhaos.organization.domain.access.login.dto.LoginOutput;
import br.com.sawcunhaos.organization.domain.access.login.internal.LoginStatus;
import br.com.sawcunhaos.organization.domain.access.login.internal.LoginType;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Casos de uso de domínio do ciclo de vida de {@code Login}.
 */
public interface LoginService {

    /** Cria um Login em PENDING_APPROVAL. Genérico o bastante para EMPLOYEE/EXTERNAL/SERVICE — o Use Case restringe o tipo por endpoint. */
    LoginOutput create(@NonNull LoginInput loginInput);

    /** Busca o Login pelo id, com profile/employee denormalizados. 404 SCOS_LOGIN_016 se não existir. */
    LoginOutput findById(@NonNull Long id);

    /** Lista Logins paginados, filtrando por type/status/employeeId quando informados (todos opcionais). */
    Page<LoginOutput> findAll(LoginType type, LoginStatus status, Long employeeId, @NonNull Pageable pageable);

}
