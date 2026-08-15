
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

import br.com.sawcunhaos.security.grpc.proto.AuthorityResponse;
import br.com.sawcunhaos.security.grpc.proto.Empty;
import br.com.sawcunhaos.security.grpc.proto.RegistryServiceGrpc;
import br.com.sawcunhaos.security.grpc.proto.RegistrySystemResponse;
import br.com.sawcunhaos.security.grpc.proto.ValidateAuthorityServiceGrpc;
import br.com.sawcunhaos.organization.infrastructure.enumaration.ScosOrganizationPermission;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.grpcmock.GrpcMock;
import org.junit.jupiter.api.BeforeEach;

import java.util.Arrays;
import java.util.List;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.grpcmock.GrpcMock.resetMappings;
import static org.grpcmock.GrpcMock.stubFor;
import static org.grpcmock.GrpcMock.unaryMethod;

/**
 * Base para testes integrados: sobe os mocks das dependências externas que o boot consome ao
 * subir e a cada request autenticada.
 *
 * <ul>
 *   <li><b>WireMock (HTTP :7080)</b> — Keycloak. Carrega {@code mappings/keycloack.json}
 *       (OIDC discovery {@code /.well-known/openid-configuration} + {@code /certs}) do classpath;
 *       o {@code @BeforeEach} sobrepõe {@code /certs} com o JWKS de teste (prioridade 1).</li>
 *   <li><b>GrpcMock (TCP :8090)</b> — scos-registry. O canal gRPC do app é montado em
 *       {@code ScosGrpcClientConfiguration} via {@code forAddress(host, 8090)} plaintext, então o
 *       mock precisa escutar na porta TCP real 8090.</li>
 * </ul>
 *
 * <p><b>Mocks SINGLETON.</b> Ambos os servidores sobem UMA vez por JVM no bloco {@code static} e
 * permanecem ativos para TODAS as classes de teste. Não usamos {@code @RegisterExtension}
 * ({@code WireMockExtension}/{@code GrpcMockExtension}), cujo ciclo é por classe: como o contexto
 * Spring é cacheado/compartilhado entre as classes (ver {@link ScosOrganizationTestUtil}), parar o
 * servidor no {@code afterAll} de uma classe deixava o canal gRPC do contexto reaproveitado
 * apontando para uma porta sem ninguém escutando → {@code UNAVAILABLE}/"Conexão recusada" em 8090.
 * Mantendo os servidores de pé durante toda a suíte, o canal permanece válido. Os servidores morrem
 * com o JVM.
 *
 * <p>Stubs gRPC:
 * <ul>
 *   <li>{@code RegistryService.registrySystem} — resposta do registro no startup
 *       ({@code ApplicationReadyEvent}). {@code update=true} encadeia {@code registryResources}.</li>
 *   <li>{@code RegistryService.registryResources} — {@code Empty} (o seed já popula SCOS_RESOURCE;
 *       aqui só evita o {@code UNIMPLEMENTED} que abortaria o startup).</li>
 *   <li>{@code ValidateAuthorityService.validateAuthority} — dirige o {@code @PreAuthorize} de cada
 *       request. Devolve todas as permissões (full admin); os cenários de 403 sobrescrevem via
 *       {@link #stubValidateAuthorityWithoutPermissions()}.</li>
 * </ul>
 */
public class ScosOrganizationWiremockUtil {

    // Alinhado à identidade fixa semeada em SCOS_SYSTEM (etc/database/seed_data.sql).
    protected static final String SYSTEM_ID  = "03000000-0000-0000-0000-000000000003";
    protected static final String SECRET_KEY = "03000000-0000-0000-0000-0000000000AA";

    private static final WireMockServer WIRE_MOCK = new WireMockServer(
            wireMockConfig()
                    .port(7080)
                    // Carrega mappings/keycloack.json (OIDC discovery) do disco. O Surefire roda com
                    // cwd = diretório do módulo (mesmo motivo dos compose "../etc/infra/..."), então
                    // src/test/resources/mappings/*.json é resolvido a partir daqui.
                    .usingFilesUnderDirectory("src/test/resources")
    );

    private static final GrpcMock GRPC_MOCK = GrpcMock.grpcMock(8090).build();

    static {
        WIRE_MOCK.start();
        GRPC_MOCK.start();
        // Direciona a API estática (stubFor/resetMappings/unaryMethod) para este servidor.
        GrpcMock.configureFor(GRPC_MOCK);

        // Stubs de startup: o registro gRPC dispara no ApplicationReadyEvent, ao criar o contexto
        // (que ocorre depois deste static, na construção da 1ª instância de teste).
        configureGrpcStubs();
        stubCerts();
    }

    /**
     * Reseta e re-registra os stubs antes de CADA teste (os servidores são singleton e não resetam
     * sozinhos entre métodos). gRPC: limpa e re-registra registry + full admin. WireMock: recarrega
     * o {@code keycloack.json} e re-aplica o JWKS de teste (prioridade 1 vence o mapping estático).
     */
    @BeforeEach
    void reconfigureMocks() {
        resetMappings();
        configureGrpcStubs();

        WIRE_MOCK.resetToDefaultMappings();
        stubCerts();
    }

    private static void stubCerts() {
        WIRE_MOCK.stubFor(
                WireMock.get("/realms/Scos/protocol/openid-connect/certs")
                        .atPriority(1)
                        .willReturn(WireMock.okJson(ScosJwtTestSupport.publicJwks()))
        );
    }

    private static void configureGrpcStubs() {
        configureRegistryStubs();
        stubValidateAuthority(fullAdminAuthority());
    }

    /**
     * Stubs do registro gRPC ({@code registrySystem} + {@code registryResources}).
     * Precisam existir SEMPRE — o cliente do app re-registra o sistema (com retry
     * "Retrying em 3000ms") e, sem estes stubs, recebe {@code UNIMPLEMENTED}. Por
     * isso qualquer reset dos mappings deve re-registrá-los.
     */
    private static void configureRegistryStubs() {
        stubFor(
                unaryMethod(RegistryServiceGrpc.getRegistrySystemMethod())
                        .willReturn(RegistrySystemResponse.newBuilder()
                                .setSystemId(SYSTEM_ID)
                                .setSecretKey(SECRET_KEY)
                                .setUpdate(true)
                                .build())
        );

        stubFor(
                unaryMethod(RegistryServiceGrpc.getRegistryResourcesMethod())
                        .willReturn(Empty.newBuilder().build())
        );
    }

    private static void stubValidateAuthority(AuthorityResponse authorityResponse) {
        stubFor(
                unaryMethod(ValidateAuthorityServiceGrpc.getValidateAuthorityMethod())
                        .willReturn(authorityResponse)
        );
    }

    /**
     * Substitui o stub de {@code validateAuthority} por um usuário autenticado, porém
     * SEM nenhuma permissão. Dirige os cenários de 403 ({@code AuthorizationDeniedException}
     * do {@code @PreAuthorize}), validando que a negação de autorização segue o mesmo
     * padrão RFC 9457 dos demais erros.
     *
     * <p>Faz {@link org.grpcmock.GrpcMock#resetMappings()} para descartar o stub de
     * full-admin registrado no {@code @BeforeEach} e RE-REGISTRA os stubs de registro
     * (senão o re-registro do sistema falha com {@code UNIMPLEMENTED}). O JWKS é servido
     * pelo WireMock (HTTP), então o token continua válido — a request passa da
     * autenticação e falha apenas na autorização.
     */
    protected static void stubValidateAuthorityWithoutPermissions() {
        resetMappings();
        configureRegistryStubs();
        stubValidateAuthority(baseAuthority().build());
    }

    private static AuthorityResponse fullAdminAuthority() {
        List<String> permissions = Arrays.stream(ScosOrganizationPermission.values())
                .map(ScosOrganizationPermission::getPermission)
                .toList();

        return baseAuthority()
                .addAllPermissions(permissions)
                .build();
    }

    // login/name/email espelham as claims do JWT de teste (preferred_username
    // = "inside.admin") — validate() ignora o argumento e devolve isto.
    private static AuthorityResponse.Builder baseAuthority() {
        return AuthorityResponse.newBuilder()
                .setLogin("inside.admin")
                .setName("inside.admin inside.admin")
                .setEmail("inside.admin@insidesoftwares.com.br")
                .setCompanyId(1L)
                .setCompanyName("SawCunhaOS Tecnologia LTDA")
                .setBranchId(1L)
                .setBranchName("SawCunhaOS Tecnologia LTDA")
                .setEmployeeId(1L);
    }
}
