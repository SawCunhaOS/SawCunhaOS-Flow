
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

package br.com.sawcunhaos.organization.application.usecase.access.status.reasonenable;

import br.com.sawcunhaos.organization.api.dto.UpdateReasonEnableRequest;
import org.jspecify.annotations.NonNull;

/** Atualiza um motivo de desbloqueio existente (UC-134). */
public interface UpdateReasonEnableUseCase {
    void execute(@NonNull Long id, @NonNull UpdateReasonEnableRequest updateReasonEnableRequest);
}
