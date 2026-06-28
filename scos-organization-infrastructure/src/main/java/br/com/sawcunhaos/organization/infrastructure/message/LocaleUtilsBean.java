package br.com.sawcunhaos.organization.infrastructure.message;

import br.com.sawcunhaos.foundation.utils.specification.LocaleService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

@Component
@RequiredArgsConstructor
final class LocaleUtilsBean implements LocaleService {

    private final MessageSource messageSource;

    @Override
    public Locale getLocale() {
        return LocaleContextHolder.getLocale(LocaleContextHolder.getLocaleContext());
    }

    @Override
    public String getMessage(String code, Object... args) {
        return messageSource.getMessage(code, args, getLocale());
    }

    @Override
    public String getMessage(String code, List<Object> args) {
        return messageSource.getMessage(code, args.toArray(), getLocale());
    }

}
