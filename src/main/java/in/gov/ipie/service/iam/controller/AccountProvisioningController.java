package in.gov.ipie.service.iam.controller;

import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import in.gov.ipie.common.security.permission.RequiresPermission;
import in.gov.ipie.common.web.idempotency.Idempotent;
import in.gov.ipie.service.iam.command.ProvisionAccountCommand;
import in.gov.ipie.service.iam.dto.request.ProvisionAccountRequest;
import in.gov.ipie.service.iam.dto.response.ProvisionAccountResponse;
import in.gov.ipie.service.iam.permission.IamPermissions;
import in.gov.ipie.service.iam.service.AccountProvisioningService;

/**
 * Internal, service-to-service only - called by ipie-user-service at self-registration step 2
 * (see its own {@code UserServiceImpl.completeRegistration}), never by a browser. Locked to
 * {@link IamPermissions#ACCOUNT_PROVISION}, granted only to ipie-user-service's own service
 * account (see {@code deploy/keycloak/realm-export.json}) - mirrors {@code
 * PillarLinkPermissions.PILLAR_LINK_RESOLVE}'s exact one-permission-one-caller shape.
 */
@RestController
public class AccountProvisioningController {

    private final AccountProvisioningService accountProvisioningService;

    public AccountProvisioningController(AccountProvisioningService accountProvisioningService) {
        this.accountProvisioningService = accountProvisioningService;
    }

    @PostMapping("/internal/accounts")
    @RequiresPermission(IamPermissions.ACCOUNT_PROVISION)
    @Idempotent
    public ProvisionAccountResponse provisionAccount(@Valid @RequestBody ProvisionAccountRequest request) {
        UUID keycloakUserId = accountProvisioningService.provisionAccount(new ProvisionAccountCommand(
                UUID.fromString(request.ipieUserId()), request.username(), request.email(), request.firstName(),
                request.lastName(), request.password()));
        return new ProvisionAccountResponse(keycloakUserId.toString());
    }

}
