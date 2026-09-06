package in.gov.ipie.keycloak.spi.pillar;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Mirrors ipie-user-service's {@code PillarLinkResolveResponse} - the resolve call's response shape. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PillarResolveResult(boolean linked, String userId, String keycloakUserId) {
}
