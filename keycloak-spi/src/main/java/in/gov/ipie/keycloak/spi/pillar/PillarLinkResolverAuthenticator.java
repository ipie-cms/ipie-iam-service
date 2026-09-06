package in.gov.ipie.keycloak.spi.pillar;

import java.net.URI;
import java.util.Map;

import jakarta.ws.rs.core.Response;

import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import in.gov.ipie.keycloak.spi.config.SpiConfig;

import org.keycloak.authentication.authenticators.broker.AbstractIdpAuthenticator;
import org.keycloak.authentication.authenticators.broker.util.SerializedBrokeredIdentityContext;
import org.keycloak.broker.oidc.OIDCIdentityProvider;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.JsonWebToken;

/**
 * Custom first-broker-login step for the pillar SSO federation feature: delegates entirely
 * to ipie-user-service's authoritative {@code pillar_links} table (via {@link
 * PillarLinkResolverClient}, now reading ipie-iam-service's read-optimised projection - see
 * that class's Javadoc) instead of Keycloak's default first-broker-login behavior (auto-create a
 * new user, or link by email match). Never writes Keycloak's own {@code FEDERATED_IDENTITY} table
 * - every brokered login is re-resolved fresh, so a link revocation in ipie-user-service takes
 * effect on the very next login with no Keycloak-side cleanup needed.
 *
 * <p>An unrecognized federated identity does not auto-provision an ipie account (a JIT-provisioning
 * design was considered and explicitly rejected - see {@code
 * iPIE_ADR001_PillarLinks_Placement.docx} discussion): the browser is instead redirected to
 * iPIE's own self-registration form ({@link #redirectToRegistration}), where the user registers
 * normally and then links whichever pillars they belong to via {@code
 * LinkedAccountsPage} - the same explicit handshake this class always required, just reachable
 * from a failed SSO attempt too, not only from an already-logged-in session.
 *
 * <p>One class handles every pillar (IBBI/NCLT/NCLAT/MCA/NeSL) via {@link
 * #ALIAS_TO_PILLAR_TYPE} - the identity-provider alias configured in {@code
 * deploy/keycloak/realm-export.json}'s {@code identityProviders[]} determines which {@code
 * PillarType} this login is for, no per-agency subclassing needed.
 */
public class PillarLinkResolverAuthenticator extends AbstractIdpAuthenticator {

    private static final Logger LOG = Logger.getLogger(PillarLinkResolverAuthenticator.class);

    private static final Map<String, String> ALIAS_TO_PILLAR_TYPE =
            Map.of("ibbi", "IBBI", "nclt", "NCLT", "nclat", "NCLAT", "mca", "MCA", "nesl", "NESL");

    private final PillarLinkResolverClient resolverClient = new PillarLinkResolverClient();

    @Override
    protected void authenticateImpl(
            AuthenticationFlowContext context, SerializedBrokeredIdentityContext serializedCtx, BrokeredIdentityContext brokerContext) {
        String alias = brokerContext.getIdpConfig().getAlias();
        String pillarType = ALIAS_TO_PILLAR_TYPE.get(alias);
        if (pillarType == null) {
            LOG.warnf("No PillarType mapping configured for identity provider alias '%s' - rejecting login", alias);
            rejectLogin(context, "unknown_pillar_alias");
            return;
        }

        JsonWebToken idToken = (JsonWebToken) brokerContext.getContextData().get(OIDCIdentityProvider.VALIDATED_ID_TOKEN);
        String pillarId = idToken != null ? asString(idToken.getOtherClaims().get("pillar_id")) : null;
        String ipieId = idToken != null ? asString(idToken.getOtherClaims().get("ipie_id")) : null;

        if (pillarId == null) {
            LOG.warnf("No pillar_id claim present on the ID token from '%s' - rejecting login", alias);
            rejectLogin(context, "missing_pillar_id_claim");
            return;
        }

        PillarResolveResult result;
        try {
            result = resolverClient.resolve(pillarType, pillarId, ipieId);
        } catch (PillarResolveException e) {
            LOG.error("Failed to resolve pillar link against ipie-user-service", e);
            rejectLogin(context, "resolve_call_failed");
            return;
        }

        if (!result.linked() || result.keycloakUserId() == null) {
            LOG.infof(
                    "No ipie account is linked to %s/%s - redirecting to registration instead of provisioning one",
                    pillarType, pillarId);
            redirectToRegistration(context, pillarType);
            return;
        }

        RealmModel realm = context.getRealm();
        KeycloakSession session = context.getSession();
        UserModel user = session.users().getUserById(realm, result.keycloakUserId());
        if (user == null) {
            LOG.errorf(
                    "resolveLink returned keycloakUserId %s but no such user exists in realm %s - rejecting login",
                    result.keycloakUserId(), realm.getName());
            rejectLogin(context, "resolved_user_not_found");
            return;
        }

        context.setUser(user);
        context.success();
    }

    @Override
    protected void actionImpl(
            AuthenticationFlowContext context, SerializedBrokeredIdentityContext serializedCtx, BrokeredIdentityContext brokerContext) {
        // No user interaction/challenge is ever presented (see authenticateImpl) - nothing to do.
    }

    @Override
    public boolean requiresUser() {
        return false;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return true;
    }

    private void rejectLogin(AuthenticationFlowContext context, String reason) {
        context.getEvent().error(reason);
        context.failure(AuthenticationFlowError.ACCESS_DENIED);
    }

    /**
     * The login genuinely does not succeed (no ipie account resolved) - {@code
     * AuthenticationFlowError.ACCESS_DENIED} still records that correctly for Keycloak's own
     * event log - but rather than Keycloak's generic "Something went wrong" error page, the
     * browser is sent to iPIE's own registration form via a real 302, using {@code
     * failure(error, Response)} instead of the plain {@link #rejectLogin} used for the genuine
     * error branches above (unknown alias, missing claim, resolve call failure, dangling
     * resolution).
     */
    private void redirectToRegistration(AuthenticationFlowContext context, String pillarType) {
        context.getEvent().error("not_linked");
        String registerUrl = SpiConfig.get(SpiConfig.WEB_REGISTER_URL);
        URI location = URI.create(registerUrl + "?pillar=" + pillarType.toLowerCase());
        Response response = Response.status(Response.Status.FOUND).location(location).build();
        context.failure(AuthenticationFlowError.ACCESS_DENIED, response);
    }

    private static String asString(Object claim) {
        return claim == null ? null : claim.toString();
    }
}
