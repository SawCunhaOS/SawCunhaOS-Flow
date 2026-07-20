
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

package br.com.sawcunhaos.organization.infrastructure.enumaration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guarda a consistência entre o contrato OpenAPI e o enum de permissões.
 *
 * <p>O {@code x-authorize} dos YAMLs em {@code etc/api/organization} é o que vira
 * {@code @PreAuthorize} no código gerado. Se a permissão declarada ali não existir em
 * {@link ScosOrganizationPermission}, ela nunca é propagada ao Keycloak nem ao seed do banco —
 * o {@code @PreAuthorize} não casa com nenhuma authority e o endpoint responde <b>403 permanente</b>.
 * O compilador não pega: o {@code x-authorize} é uma string livre no YAML.
 *
 * <p>Cobertura:
 * <ul>
 *   <li><b>x-authorize → enum</b> (falha): toda permissão citada em qualquer spec tem constante.</li>
 *   <li><b>operation → x-authorize</b> (falha): toda operation declara autorização. Sem
 *       {@code x-authorize} não há {@code @PreAuthorize} e o endpoint fica <b>público</b>.</li>
 *   <li><b>enum → x-authorize</b> (apenas aviso): constante não referenciada por nenhuma spec.
 *       Não falha — a permissão pode ser reservada ou consumida fora do OpenAPI (ex.: gRPC).</li>
 * </ul>
 *
 * <p>{@code ScosComponents.yml} é excluído: é a biblioteca de componentes reutilizáveis, e seu
 * {@code GET /} é um exemplo ilustrativo, sem execution no {@code openapi-generator} de nenhum módulo.
 *
 * <p>O path das specs é relativo ao módulo ({@code ../etc/api/organization}), mesmo padrão do
 * {@code ComposeContainer} nos testes de integração do {@code boot}.
 */
@DisplayName("Consistência entre x-authorize (OpenAPI) e ScosOrganizationPermission")
class PermissionsConsistencyTest {

    private static final Path SPEC_DIR = Path.of("..", "..", "etc", "api", "organization");
    private static final String SPEC_EXTENSION = ".yml";
    private static final String COMPONENTS_SPEC = "ScosComponents.yml";
    private static final String X_AUTHORIZE = "x-authorize";
    private static final String PATHS = "paths";

    private static final Set<String> HTTP_METHODS =
            Set.of("get", "post", "put", "delete", "patch", "head", "options");

    /** Toda operation de todas as specs, exceto a de componentes. */
    private static List<Operation> operations;

    /** Nome de cada constante de {@link ScosOrganizationPermission}. */
    private static Set<String> declaredPermissions;

    /** Uma operation de uma spec, com as permissões que ela declara. */
    private record Operation(String spec, String method, String path, List<String> permissions) {

        String describe() {
            return "%s  %s %s".formatted(spec, method.toUpperCase(Locale.ROOT), path);
        }
    }

    @BeforeAll
    static void loadSpecsAndPermissions() throws IOException {
        declaredPermissions = Arrays.stream(ScosOrganizationPermission.values())
                .map(Enum::name)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        operations = new ArrayList<>();
        try (var specFiles = Files.list(SPEC_DIR)) {
            List<Path> specs = specFiles
                    .filter(path -> path.getFileName().toString().endsWith(SPEC_EXTENSION))
                    .filter(path -> !path.getFileName().toString().equals(COMPONENTS_SPEC))
                    .sorted()
                    .toList();

            // Sem specs o teste passaria vacuamente — path errado é falha, não sucesso.
            assertFalse(specs.isEmpty(), () -> "Nenhuma spec OpenAPI encontrada em " + SPEC_DIR.toAbsolutePath());

            for (Path spec : specs) {
                operations.addAll(readOperations(spec));
            }
        }

        assertFalse(operations.isEmpty(), () -> "Nenhuma operation lida das specs em " + SPEC_DIR.toAbsolutePath());
    }

    @Test
    @DisplayName("Toda permissão em x-authorize deve ter constante em ScosOrganizationPermission")
    void everyAuthorizedPermissionMustExistInEnum() {
        // Given — permissão ausente do enum, agrupada por spec para o relatório
        Map<String, Set<String>> missingBySpec = new TreeMap<>();
        for (Operation operation : operations) {
            for (String permission : operation.permissions()) {
                if (!declaredPermissions.contains(permission)) {
                    missingBySpec.computeIfAbsent(operation.spec(), spec -> new LinkedHashSet<>()).add(permission);
                }
            }
        }

        // When / Then
        assertTrue(missingBySpec.isEmpty(), () -> """
                %d permissão(ões) declarada(s) em x-authorize não existe(m) em ScosOrganizationPermission.
                Consequência: @PreAuthorize nunca casa com uma authority → endpoint responde 403 permanente.
                Corrija adicionando a constante ao enum (e sincronize Keycloak + seed do banco).

                %s"""
                .formatted(
                        missingBySpec.values().stream().mapToInt(Set::size).sum(),
                        format(missingBySpec)));
    }

    @Test
    @DisplayName("Toda operation deve declarar x-authorize")
    void everyOperationMustDeclareAuthorization() {
        // Given — operation sem x-authorize não recebe @PreAuthorize
        List<String> unprotected = operations.stream()
                .filter(operation -> operation.permissions().isEmpty())
                .map(Operation::describe)
                .toList();

        // When / Then
        assertTrue(unprotected.isEmpty(), () -> """
                %d operation(s) sem x-authorize.
                Consequência: sem @PreAuthorize o endpoint fica público.

                %s"""
                .formatted(unprotected.size(), String.join(System.lineSeparator(), unprotected)));
    }

    @Test
    @DisplayName("Permissões do enum sem uso em x-authorize são apenas reportadas")
    void unusedPermissionsAreReportedOnly() {
        // Given
        Set<String> used = operations.stream()
                .flatMap(operation -> operation.permissions().stream())
                .collect(Collectors.toSet());

        // When
        List<String> unused = declaredPermissions.stream()
                .filter(permission -> !used.contains(permission))
                .toList();

        // Then — não falha: a constante pode ser reservada ou usada fora do OpenAPI (ex.: gRPC)
        if (!unused.isEmpty()) {
            System.out.printf(
                    "[AVISO] %d permissão(ões) em ScosOrganizationPermission sem uso em nenhum x-authorize: %s%n",
                    unused.size(), String.join(", ", unused));
        }
    }

    private static List<Operation> readOperations(Path spec) throws IOException {
        String specName = spec.getFileName().toString();
        List<Operation> found = new ArrayList<>();

        Map<String, Object> document;
        try (InputStream input = Files.newInputStream(spec)) {
            document = new Yaml().load(input);
        }
        if (document == null) {
            return found;
        }

        Map<String, Object> paths = asMap(document.get(PATHS));
        for (Map.Entry<String, Object> pathEntry : paths.entrySet()) {
            Map<String, Object> pathItem = asMap(pathEntry.getValue());

            for (Map.Entry<String, Object> methodEntry : pathItem.entrySet()) {
                if (!HTTP_METHODS.contains(methodEntry.getKey().toLowerCase(Locale.ROOT))) {
                    continue;
                }
                Map<String, Object> operation = asMap(methodEntry.getValue());
                found.add(new Operation(
                        specName,
                        methodEntry.getKey(),
                        pathEntry.getKey(),
                        asPermissions(operation.get(X_AUTHORIZE))));
            }
        }
        return found;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object value) {
        return value instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
    }

    /** Aceita tanto a forma de lista (`[A, B]`) quanto o escalar solto, tolerando espaços. */
    private static List<String> asPermissions(Object value) {
        return switch (value) {
            case null -> List.of();
            case Iterable<?> items -> {
                List<String> permissions = new ArrayList<>();
                for (Object item : items) {
                    if (item != null && !item.toString().isBlank()) {
                        permissions.add(item.toString().trim());
                    }
                }
                yield permissions;
            }
            default -> value.toString().isBlank() ? List.of() : List.of(value.toString().trim());
        };
    }

    private static String format(Map<String, Set<String>> missingBySpec) {
        return missingBySpec.entrySet().stream()
                .map(entry -> "  %s%n%s".formatted(
                        entry.getKey(),
                        entry.getValue().stream()
                                .map("    - %s"::formatted)
                                .collect(Collectors.joining(System.lineSeparator()))))
                .collect(Collectors.joining(System.lineSeparator()));
    }
}
