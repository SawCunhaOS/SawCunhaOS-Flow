
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

package br.com.sawcunhaos.security.starter.specification;

import java.time.LocalDate;

/**
 * getPermission       -> Indica o codigo da permissao
 * getCodeDescription  -> Indica a descricao da permissao
 * getActive           -> Indica se a permissao esta ativa
 * getVersion          -> Indica a versao da permissao
 * getCreatedAt        -> Indica a data de criacao da permissao
 */
public interface ScosPermission {
    String getPermission();
    String getCodeDescription();
    String getGroup();
    String getSubGroup();
    Boolean getActive();
    String getVersion();
    LocalDate getUpdatedAt();
}
