package in.gov.ipie.service.iam.controller;

import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import in.gov.ipie.common.security.permission.RequiresPermission;
import in.gov.ipie.service.iam.dto.request.ChangePasswordRequest;
import in.gov.ipie.service.iam.dto.request.SetInitialPasswordRequest;
import in.gov.ipie.service.iam.dto.request.VerifyCredentialRequest;
import in.gov.ipie.service.iam.dto.response.VerifyCredentialResponse;
import in.gov.ipie.service.iam.permission.IamPermissions;
import in.gov.ipie.service.iam.service.CredentialService;

/**
 * Every route by which a password enters the platform. There are exactly three, and they all end
 * here, because this service is the credential authority (ARCHITECTURE_WORKING_PLAN.md, D2).
 *
 * <p>Browsers call the two public routes <b>directly</b>, never through ipie-user-service. That is
 * not a stylistic choice: routing them through another service would put a synchronous cross-service
 * call on a page a user is waiting in front of, which is exactly the coupling
 * {@code InterServiceClient}'s bulkhead (10 concurrent per target, the 11th rejected outright) makes
 * dangerous under load. It would also mean a second service handling a plaintext credential.
 */
@RestController
public class CredentialController {

    private final CredentialService credentialService;

    public CredentialController(CredentialService credentialService) {
        this.credentialService = credentialService;
    }

    /**
     * The login path. Called only by ipie-keycloak-spi's authenticators, on behalf of Keycloak,
     * which no longer stores passwords and so must ask.
     *
     * <p>This is the one synchronous cross-boundary call the architecture keeps, and deliberately:
     * authentication is a question whose answer the user is waiting for, and there is no
     * asynchronous form of it. It is a single hop that does not pass through
     * {@code InterServiceClient}, so it is not subject to that bulkhead.
     */
    @PostMapping("/internal/credentials/verify")
    @RequiresPermission(IamPermissions.CREDENTIAL_VERIFY)
    public VerifyCredentialResponse verify(@Valid @RequestBody VerifyCredentialRequest request) {
        return new VerifyCredentialResponse(credentialService.verify(request.keycloakUserId(), request.password()));
    }

    /**
     * Sets the first password on an account provisioned without credentials, from the link emailed
     * to the registrant.
     *
     * <p>Public, because the caller has no credentials yet - that is the situation this endpoint
     * exists to end. The one-time token is the authorisation, and rate limiting bounds how fast
     * anyone can guess at one (see {@code ipie.security.rate-limit}).
     */
    @PostMapping("/api/v1/credentials/password")
    public ResponseEntity<Void> setInitialPassword(@Valid @RequestBody SetInitialPasswordRequest request) {
        credentialService.setInitialPassword(request.token(), request.password());
        return ResponseEntity.noContent().build();
    }

    /**
     * Changes the authenticated caller's own password. The account is taken from the JWT subject and
     * never from the request body - a body-supplied id would let any authenticated user change
     * anybody else's password.
     */
    @PostMapping("/api/v1/credentials/password/change")
    public ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ChangePasswordRequest request) {
        credentialService.changePassword(
                UUID.fromString(jwt.getSubject()), request.currentPassword(), request.newPassword());
        return ResponseEntity.noContent().build();
    }
}
