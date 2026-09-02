package in.gov.ipie.keycloak.spi.config;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.jboss.logging.Logger;

/**
 * Every setting the iPIE Keycloak providers read, and the rule for when a default is allowed.
 *
 * <p><b>Why this exists.</b> This module runs inside Keycloak's own process, so it has no Spring
 * context, no {@code application.yml} and no profile mechanism - configuration can only come from
 * the environment. That is fine for a container on a Linux server and fine for a developer's laptop;
 * what is not fine is a *default* shaped like a laptop. Until 2026-08-13 the Keycloak base URL
 * defaulted to {@code http://localhost:9090}, which is Prometheus in the local stack: the SPI's
 * client-credentials fetch got a 404 and every login failed closed with
 * {@code 503 temporarily_unavailable}. A wrong endpoint is indistinguishable from an outage of the
 * service it points at, and it stays that way until someone reads Keycloak's log.
 *
 * <p>Deployed environments must therefore not be able to inherit a developer's value. The rule:
 *
 * <ul>
 *   <li><b>Platform constants</b> - the realm name, the client id, the HMAC key ids. Identical in
 *       every environment, so they keep their defaults everywhere and may still be overridden.
 *   <li><b>Environment facts</b> - every service URL and every secret. Defaults apply in
 *       {@code dev} only. In {@code test}, {@code uat}, {@code pre-prod} and {@code prod} they are
 *       required, and their absence stops Keycloak from starting.
 *   <li><b>Tunables</b> - the call timeouts. A working default applies in every environment, so
 *       absence is never a problem; what is checked is that a value someone <em>did</em> set is
 *       usable. These exist so that the operator responsible for the deployment can widen or
 *       tighten a budget without editing this module: before 2026-08-27 the numbers were literals
 *       in the three client classes, which made them invisible to review and unchangeable without
 *       a rebuild.
 * </ul>
 *
 * <p><b>Failure happens at startup, not at the first login.</b> {@link #validateAll()} is called
 * from every provider factory's {@code init}, so a missing setting surfaces as a boot failure
 * naming the variable - not as a mysterious 503 for the first person who tries to sign in, hours
 * later, in someone else's timezone.
 *
 * <p>The one setting that is *not* environment-dependent is where Keycloak is: these providers run
 * inside Keycloak, so it is always localhost. It is still required outside dev, because the port and
 * scheme are deployment choices and getting them wrong reproduces exactly the 9090 failure.
 */
public final class SpiConfig {

    private static final Logger LOG = Logger.getLogger(SpiConfig.class);

    /** The environment this Keycloak belongs to. Unset means {@link #DEV}. */
    public static final String IPIE_ENV = "IPIE_ENV";

    public static final String DEV = "dev";
    private static final Set<String> KNOWN_ENVIRONMENTS = Set.of(DEV, "test", "uat", "pre-prod", "prod");

    // --- platform constants: same value in every environment, default everywhere ------------------
    public static final String KEYCLOAK_REALM = "IPIE_KEYCLOAK_REALM";
    public static final String SPI_CLIENT_ID = "IPIE_KEYCLOAK_SPI_CLIENT_ID";
    public static final String HMAC_KEY_ID_IAM = "IPIE_SPI_HMAC_KEY_ID";
    public static final String HMAC_KEY_ID_USER = "IPIE_SPI_HMAC_KEY_ID_USER";

    // --- environment facts: dev defaults only, required everywhere else ---------------------------
    public static final String KEYCLOAK_BASE_URL = "IPIE_KEYCLOAK_BASE_URL";
    public static final String IAM_SERVICE_BASE_URL = "IPIE_IAM_SERVICE_BASE_URL";
    public static final String USER_SERVICE_BASE_URL = "IPIE_USER_SERVICE_BASE_URL";
    public static final String WEB_REGISTER_URL = "IPIE_WEB_REGISTER_URL";
    public static final String SPI_CLIENT_SECRET = "IPIE_KEYCLOAK_SPI_CLIENT_SECRET";
    public static final String HMAC_SECRET_IAM = "IPIE_SPI_HMAC_SECRET";
    public static final String HMAC_SECRET_USER = "IPIE_SPI_HMAC_SECRET_USER";

    // --- tunables: a working default everywhere, overridable per environment ----------------------
    // Milliseconds. Each client keeps its own pair because the calls are not alike: see the note on
    // TUNABLES below.
    public static final String CREDENTIAL_CONNECT_TIMEOUT_MS = "IPIE_SPI_CREDENTIAL_CONNECT_TIMEOUT_MS";
    public static final String CREDENTIAL_REQUEST_TIMEOUT_MS = "IPIE_SPI_CREDENTIAL_REQUEST_TIMEOUT_MS";
    public static final String LOGIN_NOTIFY_CONNECT_TIMEOUT_MS = "IPIE_SPI_LOGIN_NOTIFY_CONNECT_TIMEOUT_MS";
    public static final String LOGIN_NOTIFY_REQUEST_TIMEOUT_MS = "IPIE_SPI_LOGIN_NOTIFY_REQUEST_TIMEOUT_MS";
    public static final String PILLAR_CONNECT_TIMEOUT_MS = "IPIE_SPI_PILLAR_CONNECT_TIMEOUT_MS";
    public static final String PILLAR_REQUEST_TIMEOUT_MS = "IPIE_SPI_PILLAR_REQUEST_TIMEOUT_MS";

    private static final Map<String, String> PLATFORM_CONSTANTS = new LinkedHashMap<>();
    private static final Map<String, String> ENVIRONMENT_FACTS = new LinkedHashMap<>();
    private static final Map<String, String> TUNABLES = new LinkedHashMap<>();

    static {
        PLATFORM_CONSTANTS.put(KEYCLOAK_REALM, "ipie");
        PLATFORM_CONSTANTS.put(SPI_CLIENT_ID, "ipie-keycloak-spi");
        PLATFORM_CONSTANTS.put(HMAC_KEY_ID_IAM, "spi-to-iam");
        PLATFORM_CONSTANTS.put(HMAC_KEY_ID_USER, "spi-to-user");

        // Keycloak reaching itself; the services as published on a developer's own machine.
        ENVIRONMENT_FACTS.put(KEYCLOAK_BASE_URL, "http://localhost:8080");
        ENVIRONMENT_FACTS.put(IAM_SERVICE_BASE_URL, "http://localhost:8093");
        ENVIRONMENT_FACTS.put(USER_SERVICE_BASE_URL, "http://localhost:8092");
        ENVIRONMENT_FACTS.put(WEB_REGISTER_URL, "http://localhost:5173/register");
        ENVIRONMENT_FACTS.put(SPI_CLIENT_SECRET, "ipie-keycloak-spi-secret");
        // The two HMAC secrets have no usable dev default: an empty key makes the signer throw and
        // the call fail closed. Local development supplies them the same way a deployment does.
        ENVIRONMENT_FACTS.put(HMAC_SECRET_IAM, "");
        ENVIRONMENT_FACTS.put(HMAC_SECRET_USER, "");

        // These are the values the code carried literally until 2026-08-27, restated so that they
        // can be reviewed and tuned rather than edited. Adopting them changed no behaviour.
        //
        // They are NOT one shared timeout, because the three calls fail differently:
        //   - credential verify sits on the interactive login path with a person waiting, so it is
        //     deliberately the tightest: a slow dependency should surface as a failed login rather
        //     than a hung browser, and it is never retried (each attempt counts as a login attempt).
        //   - login notification is a side effect of a login that has already succeeded.
        //   - pillar resolve is the most tolerant of the three.
        // Widening all three together would undo that, which is why there is a pair per client.
        //
        // Within one client the outbound call and the client-credentials token fetch share the
        // request timeout, because they already carried the same number. Split them only if the two
        // hops are found to need different budgets - Keycloak is always local to this code, the
        // service it calls is not.
        TUNABLES.put(CREDENTIAL_CONNECT_TIMEOUT_MS, "3000");
        TUNABLES.put(CREDENTIAL_REQUEST_TIMEOUT_MS, "5000");
        TUNABLES.put(LOGIN_NOTIFY_CONNECT_TIMEOUT_MS, "5000");
        TUNABLES.put(LOGIN_NOTIFY_REQUEST_TIMEOUT_MS, "5000");
        TUNABLES.put(PILLAR_CONNECT_TIMEOUT_MS, "5000");
        TUNABLES.put(PILLAR_REQUEST_TIMEOUT_MS, "10000");
    }

    private SpiConfig() {
    }

    /** The current environment, lowercased; {@value #DEV} when {@link #IPIE_ENV} is unset. */
    public static String environment() {
        String value = System.getenv(IPIE_ENV);
        return (value == null || value.isBlank()) ? DEV : value.trim().toLowerCase(Locale.ROOT);
    }

    public static boolean isDev() {
        return DEV.equals(environment());
    }

    /**
     * The value of {@code name}, or its default where one may be applied.
     *
     * <p>Never throws: it is called from field initialisers that run when Keycloak instantiates a
     * provider factory, which is <em>before</em> {@code init} and therefore before
     * {@link #validateAll()} has had its chance to report every problem at once. Outside dev a
     * missing environment fact returns {@code null} here and is reported there.
     */
    public static String get(String name) {
        String value = System.getenv(name);
        if (value != null && !value.isBlank()) {
            return value;
        }
        if (PLATFORM_CONSTANTS.containsKey(name)) {
            return PLATFORM_CONSTANTS.get(name);
        }
        if (ENVIRONMENT_FACTS.containsKey(name)) {
            return isDev() ? ENVIRONMENT_FACTS.get(name) : null;
        }
        if (TUNABLES.containsKey(name)) {
            return TUNABLES.get(name);
        }
        throw new IllegalArgumentException("Unknown iPIE SPI setting: " + name);
    }

    /**
     * A tunable read as a duration in milliseconds.
     *
     * <p>Falls back to the shipped default if the value cannot be parsed, rather than throwing:
     * like {@link #get(String)} this runs in a field initialiser, before {@link #validateAll()} has
     * reported anything, and a client that fails to construct would take Keycloak's whole provider
     * down over a typo. {@code validateAll} is what turns that typo into a named boot failure.
     */
    public static Duration duration(String name) {
        String configured = System.getenv(name);
        Long parsed = positiveMillis(configured);
        if (parsed != null) {
            return Duration.ofMillis(parsed);
        }
        if (configured != null && !configured.isBlank()) {
            LOG.warnf("%s='%s' is not a positive whole number of milliseconds - using %s until"
                    + " startup validation reports it", name, configured, TUNABLES.get(name));
        }
        return Duration.ofMillis(Long.parseLong(TUNABLES.get(name)));
    }

    /**
     * The value as a positive millisecond count, or {@code null} if it is absent or not one.
     *
     * <p>Package-private rather than private so the parsing rules can be asserted directly: they
     * cannot be reached through {@link #duration(String)} in a test, because that reads the real
     * process environment and a test cannot set one.
     */
    static Long positiveMillis(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            long millis = Long.parseLong(value.trim());
            return millis > 0 ? millis : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Checks every setting and throws once, listing all of them.
     *
     * <p>Called from each provider factory's {@code init}. Throwing there aborts Keycloak's startup,
     * which is the whole point: the alternative is a Keycloak that starts happily and rejects every
     * login.
     *
     * @throws IllegalStateException if the environment name is unknown, or any environment fact is
     *     missing outside dev
     */
    public static void validateAll() {
        String environment = environment();
        List<String> problems = new ArrayList<>();

        if (!KNOWN_ENVIRONMENTS.contains(environment)) {
            problems.add(IPIE_ENV + "='" + environment + "' is not one of " + KNOWN_ENVIRONMENTS);
        }

        if (!DEV.equals(environment)) {
            for (String name : ENVIRONMENT_FACTS.keySet()) {
                String value = System.getenv(name);
                if (value == null || value.isBlank()) {
                    problems.add(name + " is required when " + IPIE_ENV + '=' + environment);
                }
            }
        }

        // Tunables always have a default, so absence is fine and only an unusable value is a
        // problem. It is still reported here rather than tolerated: a timeout of "5s" or "-1" means
        // someone intended to change a budget and did not, and finding that out at the first failed
        // login - with the old value silently still in force - is exactly the failure mode this
        // class exists to prevent.
        for (String name : TUNABLES.keySet()) {
            String value = System.getenv(name);
            if (value != null && !value.isBlank() && positiveMillis(value) == null) {
                problems.add(name + "='" + value + "' is not a positive whole number of milliseconds");
            }
        }

        if (!problems.isEmpty()) {
            throw new IllegalStateException(
                    "ipie-keycloak-spi is not configured for " + IPIE_ENV + '=' + environment + ":\n  - "
                            + String.join("\n  - ", problems)
                            + "\nDeployed environments must state their own endpoints and secrets; a default"
                            + " belonging to a developer's machine would fail every login closed with"
                            + " 503 temporarily_unavailable instead of failing here.");
        }

        if (System.getenv(IPIE_ENV) == null || System.getenv(IPIE_ENV).isBlank()) {
            LOG.warnf("%s is not set - assuming '%s' and applying localhost defaults for %s."
                            + " Set %s explicitly in every deployed environment.",
                    IPIE_ENV, DEV, ENVIRONMENT_FACTS.keySet(), IPIE_ENV);
        } else {
            LOG.infof("ipie-keycloak-spi configured for %s=%s", IPIE_ENV, environment);
        }
    }
}
