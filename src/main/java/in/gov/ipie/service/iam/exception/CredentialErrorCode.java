package in.gov.ipie.service.iam.exception;

import in.gov.ipie.common.core.exception.ErrorCode;

/** Stable error codes for credential handling (master standards doc, 5.4). */
public enum CredentialErrorCode implements ErrorCode {

    /** The set-password link is unknown, expired, or already used - deliberately indistinguishable. */
    INVALID_CREDENTIAL_SETUP_TOKEN,

    /** Change-password was called with the wrong current password. */
    INVALID_CURRENT_PASSWORD,

    /** No password has been set for this account yet - it was provisioned without credentials. */
    CREDENTIAL_NOT_SET;

    @Override
    public String code() {
        return name();
    }
}
