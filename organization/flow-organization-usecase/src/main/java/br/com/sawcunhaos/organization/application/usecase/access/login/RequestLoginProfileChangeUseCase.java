
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

package br.com.sawcunhaos.organization.application.usecase.access.login;

import org.jspecify.annotations.NonNull;

/**
 * Abre a solicitação de troca de Perfil (principal ou adicional) de um Login {@code ACTIVE}
 * (Story 3.4) - o Login continua no Perfil/conjunto atual até a solicitação ser aprovada.
 */
public interface RequestLoginProfileChangeUseCase {

    /** @return o id da {@code LoginApprovalRequest} criada. */
    Long execute(@NonNull Long loginId, @NonNull Long profileId, @NonNull ProfileChangeKind kind);

}
