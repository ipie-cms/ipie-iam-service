package in.gov.ipie.keycloak.spi.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * The timeouts these providers use were literals in the three client classes until 2026-08-27.
 * Moving them into {@link SpiConfig} was meant to change nothing except who can change them, so
 * both halves of that are asserted here.
 *
 * <p>The defaults below are the exact values the code carried before. If one of these fails, the
 * timeout budget of a login path moved - check that was intended rather than adjusting the
 * expectation, because the three budgets are deliberately different from each other.
 */
class SpiConfigTimeoutsTest {

    /** Only meaningful when the surrounding process has not set the variable itself. */
    private static void assertDefault(String name, long expectedMillis) {
        assumeTrue(System.getenv(name) == null || System.getenv(name).isBlank(),
                name + " is set in this environment, so its default cannot be observed");
        assertThat(SpiConfig.duration(name)).isEqualTo(Duration.ofMillis(expectedMillis));
    }

    @Test
    @DisplayName("credential verify keeps the tightest budget - a person is waiting on it")
    void credentialDefaults() {
        assertDefault(SpiConfig.CREDENTIAL_CONNECT_TIMEOUT_MS, 3000);
        assertDefault(SpiConfig.CREDENTIAL_REQUEST_TIMEOUT_MS, 5000);
    }

    @Test
    @DisplayName("login notification keeps its five-second budget")
    void loginNotifyDefaults() {
        assertDefault(SpiConfig.LOGIN_NOTIFY_CONNECT_TIMEOUT_MS, 5000);
        assertDefault(SpiConfig.LOGIN_NOTIFY_REQUEST_TIMEOUT_MS, 5000);
    }

    @Test
    @DisplayName("pillar resolve stays the most tolerant of the three")
    void pillarDefaults() {
        assertDefault(SpiConfig.PILLAR_CONNECT_TIMEOUT_MS, 5000);
        assertDefault(SpiConfig.PILLAR_REQUEST_TIMEOUT_MS, 10000);
    }

    @Test
    @DisplayName("the credential budget is tighter than the pillar one, which is the point of separate knobs")
    void credentialIsTighterThanPillar() {
        assumeTrue(System.getenv(SpiConfig.CREDENTIAL_REQUEST_TIMEOUT_MS) == null
                && System.getenv(SpiConfig.PILLAR_REQUEST_TIMEOUT_MS) == null);
        assertThat(SpiConfig.duration(SpiConfig.CREDENTIAL_REQUEST_TIMEOUT_MS))
                .isLessThan(SpiConfig.duration(SpiConfig.PILLAR_REQUEST_TIMEOUT_MS));
    }

    @ParameterizedTest
    @ValueSource(strings = {"1", "250", "3000", " 5000 "})
    @DisplayName("a positive whole number of milliseconds is accepted")
    void acceptsPositiveMillis(String value) {
        assertThat(SpiConfig.positiveMillis(value)).isNotNull().isPositive();
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "5s", "5000ms", "PT5S", "abc", "5.0", ""})
    @DisplayName("anything that is not a positive whole number of milliseconds is rejected")
    void rejectsEverythingElse(String value) {
        // Rejection is what makes validateAll() report it at startup instead of the value being
        // quietly ignored and the old budget staying in force. "5s" and "PT5S" are the plausible
        // mistakes: both are how a timeout is written elsewhere in the platform's YAML.
        assertThat(SpiConfig.positiveMillis(value)).isNull();
    }

    @Test
    @DisplayName("an absent value is rejected too, so the shipped default is used")
    void rejectsNull() {
        assertThat(SpiConfig.positiveMillis(null)).isNull();
    }
}
