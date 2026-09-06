package in.gov.ipie.service.iam.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfigurationSource;

import in.gov.ipie.common.i18n.MessageResolver;
import in.gov.ipie.common.security.config.IpieSecurityProperties;
import in.gov.ipie.common.security.config.ResourceServerAutoConfiguration;
import in.gov.ipie.common.security.hmac.HmacSignatureVerificationFilter;
import in.gov.ipie.common.security.hmac.HmacSigningProperties;
import in.gov.ipie.common.security.hmac.NonceStore;
import in.gov.ipie.common.security.ratelimit.RateLimitFilter;
import in.gov.ipie.common.security.ratelimit.RateLimitProperties;
import in.gov.ipie.common.security.ratelimit.RateLimiter;

/**
 * This service's own {@code SecurityFilterChain}, the only way to add a filter on top of the
 * platform baseline (see {@link ResourceServerAutoConfiguration}'s Javadoc) - needed here to
 * verify {@link HmacSignatureVerificationFilter} (fully implemented in {@code ipie-common-libs}
 * already - this class only wires it in) against this service's two internal, service-to-service
 * only endpoints ({@code /internal/accounts}, {@code /internal/pillar-links/resolve}, see
 * {@code ipie.security.hmac.protected-paths}). Runs *before* the existing
 * {@code @RequiresPermission} checks already gating both endpoints - additive defense in depth on
 * top of the OAuth2 bearer token, not a replacement for the permission gating already in place.
 * Reuses {@link ResourceServerAutoConfiguration#configureBaseline} so every other behaviour (JWT
 * validation, CORS, public-path handling) stays identical to every other service.
 *
 * <p>It also wires {@link RateLimitFilter}, for the same reason ipie-user-service's
 * {@code SecurityConfig} does: {@code RateLimitAutoConfiguration} deliberately supplies only the
 * {@link RateLimiter}, leaving each service to register the filter for the paths that need it. The
 * rules in {@code ipie.security.rate-limit} were written when this service took over credentials
 * but the filter was never added, so they configured nothing - and the path they exist to protect,
 * {@code POST /api/v1/credentials/password}, is both public (it is in {@code public-paths}, since
 * the caller has no credential yet) and the only unauthenticated way into the credential store.
 * Its one-time token was bounded by nothing but its own entropy.
 *
 * <p>Filter order matches ipie-user-service's chain exactly - HMAC verification, then rate
 * limiting, then {@code UsernamePasswordAuthenticationFilter}. No path here is both HMAC-protected
 * ({@code /internal/**}) and rate-limited ({@code /api/v1/credentials/**}), so the relative order
 * of those two is immaterial today; it is matched anyway so the two services cannot drift into
 * behaving differently once a path does fall under both.
 *
 * <p>{@link ObjectMapper} and {@link MessageResolver} are needed only so the filter can render its
 * 429 in the platform's standard {@code ApiError} shape - it short-circuits before any controller,
 * so {@code GlobalExceptionHandler} never sees the rejection. They and the HMAC collaborators are
 * injected through this class's constructor rather than as {@code @Bean}-method parameters, purely
 * to keep {@link #iamServiceSecurityFilterChain} under Checkstyle's {@code ParameterNumber} limit
 * (max 7) now that it wires two filters instead of one - the same split ipie-user-service uses.
 *
 * <p>Guarded by the same {@code ipie.security.enabled} condition {@code
 * ResourceServerAutoConfiguration#ipieSecurityFilterChain} uses - without this, defining any
 * {@code SecurityFilterChain} bean here would unconditionally back off *both* of that class's
 * beans (each is {@code @ConditionalOnMissingBean(SecurityFilterChain.class)}), silently breaking
 * the {@code ipie.security.enabled=false} local-development escape hatch for this service only.
 */
@Configuration(proxyBeanMethods = false)
class SecurityConfig {

    private final HmacSigningProperties hmacSigningProperties;
    private final NonceStore nonceStore;
    private final ObjectMapper objectMapper;
    private final MessageResolver messageResolver;

    SecurityConfig(
            HmacSigningProperties hmacSigningProperties,
            NonceStore nonceStore,
            ObjectMapper objectMapper,
            MessageResolver messageResolver) {
        this.hmacSigningProperties = hmacSigningProperties;
        this.nonceStore = nonceStore;
        this.objectMapper = objectMapper;
        this.messageResolver = messageResolver;
    }

    @Bean
    @ConditionalOnProperty(prefix = "ipie.security", name = "enabled", havingValue = "true", matchIfMissing = true)
    SecurityFilterChain iamServiceSecurityFilterChain(
            HttpSecurity http,
            IpieSecurityProperties properties,
            @Qualifier("ipieCorsConfigurationSource") CorsConfigurationSource corsConfigurationSource,
            RateLimitProperties rateLimitProperties,
            RateLimiter rateLimiter,
            MeterRegistry meterRegistry)
            throws Exception {
        ResourceServerAutoConfiguration.configureBaseline(http, properties, corsConfigurationSource);
        http.addFilterBefore(
                new RateLimitFilter(rateLimitProperties, rateLimiter, meterRegistry, objectMapper, messageResolver),
                UsernamePasswordAuthenticationFilter.class);
        http.addFilterBefore(
                new HmacSignatureVerificationFilter(hmacSigningProperties, nonceStore, meterRegistry), RateLimitFilter.class);
        return http.build();
    }
}
