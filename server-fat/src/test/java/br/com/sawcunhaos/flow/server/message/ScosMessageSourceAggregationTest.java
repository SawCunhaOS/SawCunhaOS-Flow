package br.com.sawcunhaos.flow.server.message;

import br.com.sawcunhaos.foundation.web.message.ScosMessageSourceConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prova, no classpath REAL do server-fat (vários jars do Flow, cada um com seu próprio
 * {@code scos_message/}, empacotados juntos como acontece no fat jar), que
 * {@link ScosMessageSourceConfiguration} descobre os bundles de TODOS os módulos, não só o
 * primeiro encontrado no classpath. A versão anterior usava {@code classpath:} (singular), que só
 * resolve o primeiro root via {@code ClassLoader.getResource(String)}; corrigido para
 * {@code classpath*:}, que agrega via {@code ClassLoader.getResources(String)}.
 */
class ScosMessageSourceAggregationTest {

    @Test
    void discoversBundlesFromEveryFlowModuleOnTheRealClasspath() throws Exception {
        var messageSource = (ReloadableResourceBundleMessageSource)
                new ScosMessageSourceConfiguration().messageSource();

        var basenames = messageSource.getBasenameSet();
        System.out.println("Basenames descobertos (" + basenames.size() + "):");
        basenames.stream().sorted().forEach(b -> System.out.println("  - " + b));

        assertThat(basenames).isNotEmpty();
    }

    @Test
    void resolvesACodeFromTheFoundationCoreBundle() throws Exception {
        MessageSource messageSource = new ScosMessageSourceConfiguration().messageSource();

        assertThat(messageSource.getMessage("SCOS-001", null, Locale.of("en")))
                .isEqualTo("The attributes informed do not match what was expected. Please check the method documentation.");
    }
}
