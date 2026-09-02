package in.gov.ipie.service.iam.controller;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import in.gov.ipie.common.security.permission.RequiresPermission;
import in.gov.ipie.service.iam.dto.request.ResolvePillarLinkRequest;
import in.gov.ipie.service.iam.dto.response.ResolvePillarLinkResponse;
import in.gov.ipie.service.iam.permission.IamPermissions;
import in.gov.ipie.service.iam.service.PillarResolutionService;

/**
 * The Keycloak SPI's first-broker-login Authenticator calls this on every federated SSO login -
 * previously ipie-user-service's own {@code /api/v1/pillar-links/resolve}, moved here per
 * ADR-001 ("Placement of pillar_links Data and /resolve Endpoint") so this service's own
 * database answers it (one indexed query against the {@code pillar_resolution} projection),
 * with zero cross-service calls at read time - iam-service's login-path availability no longer
 * depends on ipie-user-service's.
 *
 * <p>Does not return a {@code roles} list: once {@code context.setUser(...)} resolves the login to
 * the found Keycloak user, Keycloak's own token issuance already includes that user's assigned
 * realm roles via the existing {@code permissions}-claim protocol mapper - nothing extra to
 * communicate here.
 */
@RestController
public class PillarLinkResolveController {

    private final PillarResolutionService resolutionService;

    public PillarLinkResolveController(PillarResolutionService resolutionService) {
        this.resolutionService = resolutionService;
    }

    @PostMapping("/internal/pillar-links/resolve")
    @RequiresPermission(IamPermissions.PILLAR_LINK_RESOLVE)
    public ResolvePillarLinkResponse resolve(@Valid @RequestBody ResolvePillarLinkRequest request) {
        return resolutionService.resolve(request.pillarType(), request.externalPillarId())
                .map(resolution -> new ResolvePillarLinkResponse(
                        true, resolution.ipieUserId().toString(), resolution.keycloakUserId().toString()))
                .orElseGet(ResolvePillarLinkResponse::notLinked);
    }
}
