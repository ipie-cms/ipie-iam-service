package in.gov.ipie.service.iam.dto.response;

/**
 * Mirrors the shape the Keycloak SPI's {@code PillarResolveResult} already expects
 * (ipie-keycloak-spi) - unchanged from when ipie-user-service's own {@code /resolve} endpoint
 * produced it, only the service answering the call has moved (ADR-001).
 */
public record ResolvePillarLinkResponse(boolean linked, String userId, String keycloakUserId) {

    public static ResolvePillarLinkResponse notLinked() {
        return new ResolvePillarLinkResponse(false, null, null);
    }
}
