package in.gov.ipie.service.iam.permission;

/** Permission-name constants (master standards doc, 5.5) - matches the seeded {@code ROLES_MANAGE} permission. */
public final class IamPermissions {

    /**
     * Gates <b>assigning</b> a role to a user and withdrawing it again - not defining what roles
     * exist, which is {@link #RBAC_DEFINE}. Held by PILLAR_ADMIN and SUPER_ADMIN.
     *
     * <p>The name predates the split and is kept deliberately: it is a Keycloak realm role carried
     * in every issued token and named in {@code realm-export.json}'s composites, so renaming it
     * would strip access from everyone holding a current token until it was re-issued. Read it as
     * "manage a user's roles", not "manage the set of roles".
     */
    public static final String ROLES_MANAGE = "ROLES_MANAGE";

    /**
     * Gates <b>defining</b> the RBAC catalogue itself: creating, editing and deleting roles, and
     * creating permissions. Granted to SUPER_ADMIN only.
     *
     * <p>Separate from {@link #ROLES_MANAGE} because the two are different powers. Assigning an
     * existing role hands a user a capability someone already decided the platform should have;
     * defining a role or permission decides what capabilities exist at all, and a role composed
     * with the wrong permissions grants them to every holder at once. PILLAR_ADMIN administers
     * users and therefore holds the first; only the platform operator holds the second.
     */
    public static final String RBAC_DEFINE = "RBAC_DEFINE";

    /** Gates {@code POST /internal/accounts} - granted only to ipie-user-service's own service account. */
    public static final String ACCOUNT_PROVISION = "ACCOUNT_PROVISION";

    /**
     * Gates {@code POST /internal/credentials/verify} - granted only to ipie-keycloak-spi's service
     * account, which is the sole legitimate caller: it is Keycloak's authenticator asking whether a
     * submitted password is correct.
     *
     * <p>Deliberately its own permission rather than reusing {@link #ACCOUNT_PROVISION}. That one is
     * held by ipie-user-service, which must never be able to test passwords - the whole point of
     * moving credentials here is that no other service can reach them.
     */
    public static final String CREDENTIAL_VERIFY = "CREDENTIAL_VERIFY";

    /**
     * Gates {@code POST /internal/pillar-links/resolve} - the same realm role/permission name
     * ipie-user-service's now-removed {@code /resolve} endpoint used, granted to
     * ipie-keycloak-spi's service account (see {@code deploy/keycloak/realm-export.json}) and
     * reused here unchanged since it is the same caller, now hitting a different service.
     */
    public static final String PILLAR_LINK_RESOLVE = "PILLAR_LINK_RESOLVE";

    private IamPermissions() {
    }
}
