package br.com.sawcunhaos.organization.infrastructure.validation;

import br.com.sawcunhaos.organization.infrastructure.enumaration.ScosOrganizationPermission;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class PermissionsConsistencyTest {

    private static final Pattern X_AUTHORIZE_PATTERN = Pattern.compile("x-authorize:\\s*\\[([^\\]]+)\\]", Pattern.CASE_INSENSITIVE);

    @Test
    void allXAuthorizeValuesMustExistInScosOrganizationPermission() throws IOException {
        Path apiDir = Paths.get("..", "etc", "api", "organization");
        assertTrue(Files.exists(apiDir), "API directory not found: " + apiDir.toAbsolutePath());

        List<String> missing = new ArrayList<>();

        Files.list(apiDir)
                .filter(p -> p.toString().endsWith(".yml") || p.toString().endsWith(".yaml"))
                .forEach(path -> {
                    try {
                        String content = Files.readString(path);
                        Matcher matcher = X_AUTHORIZE_PATTERN.matcher(content);
                        while (matcher.find()) {
                            String group = matcher.group(1);
                            String[] tokens = group.split(",");
                            for (String t : tokens) {
                                String perm = t.trim();
                                if (perm.isEmpty()) continue;
                                try {
                                    ScosOrganizationPermission.valueOf(perm);
                                } catch (IllegalArgumentException ex) {
                                    missing.add(path.getFileName() + ":" + perm);
                                }
                            }
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });

        assertTrue(missing.isEmpty(), "Missing permissions in ScosOrganizationPermission enum: " + missing);
    }
}
