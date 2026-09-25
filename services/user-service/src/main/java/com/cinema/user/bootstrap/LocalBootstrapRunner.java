package com.cinema.user.bootstrap;

import com.cinema.user.entity.Role;
import com.cinema.user.entity.User;
import com.cinema.user.entity.UserProfile;
import com.cinema.user.entity.UserRole;
import com.cinema.user.enums.AccountStatus;
import com.cinema.user.enums.RoleName;
import com.cinema.user.oauth2.RegisteredClientFactory;
import com.cinema.user.oauth2.model.PublicClientRegistration;
import com.cinema.user.repository.RoleRepository;
import com.cinema.user.repository.UserCredentialRepository;
import com.cinema.user.repository.UserProfileRepository;
import com.cinema.user.repository.UserRepository;
import com.cinema.user.repository.UserRoleRepository;
import com.cinema.user.service.UserCredentialService;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsent;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Set;

@Component
@ConditionalOnProperty(name = "cinema.local-bootstrap.enabled", havingValue = "true")
public class LocalBootstrapRunner implements ApplicationRunner {

    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_EMAIL = "admin@cinema.local";
    private static final String ADMIN_PASSWORD = "Admin@123456";
    private static final String CLIENT_ID = "cinema-swagger";
    private static final String CINEMA_WEB_CLIENT_ID = "cinema-web";

    private static final Set<String> CINEMA_WEB_REDIRECT_URIS =
            Set.of("http://localhost:5173/auth/callback");

    private static final Set<String> CINEMA_WEB_POST_LOGOUT_REDIRECT_URIS =
            Set.of("http://localhost:5173/");

    private static final Set<String> CINEMA_WEB_SCOPES = Set.of("openid", "profile", "email");

    private static final Set<String> REDIRECT_URIS =
            Set.of(
                    "http://localhost:8081/swagger-ui/oauth2-redirect.html",
                    "http://localhost:8083/swagger-ui/oauth2-redirect.html",
                    "http://localhost:8084/swagger-ui/oauth2-redirect.html",
                    "http://localhost:8085/swagger-ui/oauth2-redirect.html");

    private static final Set<String> SCOPES =
            Set.of(
                    "openid",
                    "profile",
                    "email",
                    "booking:create",
                    "booking:read",
                    "booking:cancel",
                    "movie:manage",
                    "showtime:manage",
                    "inventory:manage",
                    "payment:read",
                    "user:manage");

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserCredentialRepository userCredentialRepository;
    private final UserCredentialService userCredentialService;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final RegisteredClientFactory registeredClientFactory;
    private final RegisteredClientRepository registeredClientRepository;
    private final OAuth2AuthorizationConsentService authorizationConsentService;

    public LocalBootstrapRunner(
            UserRepository userRepository,
            UserProfileRepository userProfileRepository,
            UserCredentialRepository userCredentialRepository,
            UserCredentialService userCredentialService,
            RoleRepository roleRepository,
            UserRoleRepository userRoleRepository,
            RegisteredClientFactory registeredClientFactory,
            RegisteredClientRepository registeredClientRepository,
            OAuth2AuthorizationConsentService authorizationConsentService) {

        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.userCredentialRepository = userCredentialRepository;
        this.userCredentialService = userCredentialService;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.registeredClientFactory = registeredClientFactory;
        this.registeredClientRepository = registeredClientRepository;
        this.authorizationConsentService = authorizationConsentService;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {

        User admin = bootstrapAdmin();
        RegisteredClient swaggerClient = bootstrapSwaggerClient();
        bootstrapCinemaWebClient();
        bootstrapConsent(admin, swaggerClient);

        System.out.println("========================================");
        System.out.println("LOCAL BOOTSTRAP COMPLETED");
        System.out.println("username = " + ADMIN_USERNAME);
        System.out.println("password = " + ADMIN_PASSWORD);
        System.out.println("clientId = " + CLIENT_ID);
        System.out.println("redirectUris = " + REDIRECT_URIS);
        System.out.println("cinemaWebClientId = " + CINEMA_WEB_CLIENT_ID);
        System.out.println("cinemaWebRedirectUris = " + CINEMA_WEB_REDIRECT_URIS);
        System.out.println("========================================");
    }

    private User bootstrapAdmin() {

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        User admin =
                userRepository
                        .findByNormalizedUsername(ADMIN_USERNAME)
                        .orElseGet(
                                () ->
                                        userRepository.saveAndFlush(
                                                new User(
                                                        ADMIN_EMAIL,
                                                        ADMIN_EMAIL,
                                                        ADMIN_USERNAME,
                                                        ADMIN_USERNAME)));

        if (!userCredentialRepository.existsByUser_Id(admin.getId())) {
            userCredentialService.createCredential(admin.getId(), ADMIN_PASSWORD);
        }

        if (admin.getStatus() == AccountStatus.PENDING_VERIFICATION) {
            admin.verifyEmail(now);
            admin = userRepository.saveAndFlush(admin);
        }

        if (!userProfileRepository.existsByUser_Id(admin.getId())) {
            userProfileRepository.saveAndFlush(new UserProfile(admin, "Local", "Admin", null));
        }

        Role adminRole =
                roleRepository
                        .findByName(RoleName.ADMIN)
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "ADMIN role was not seeded by Flyway"));

        if (!userRoleRepository.existsByUser_IdAndRole_Id(admin.getId(), adminRole.getId())) {

            userRoleRepository.saveAndFlush(new UserRole(admin, adminRole, now, admin));
        }

        return admin;
    }

    private RegisteredClient bootstrapSwaggerClient() {

        RegisteredClient existing = registeredClientRepository.findByClientId(CLIENT_ID);

        if (existing != null) {
            validateExistingSwaggerClient(existing);
            return existing;
        }

        RegisteredClient client =
                registeredClientFactory.createPublicClient(
                        new PublicClientRegistration(
                                CLIENT_ID, "Cinema Swagger", REDIRECT_URIS, Set.of(), SCOPES));

        registeredClientRepository.save(client);

        return client;
    }

    private RegisteredClient bootstrapCinemaWebClient() {

        RegisteredClient existing = registeredClientRepository.findByClientId(CINEMA_WEB_CLIENT_ID);

        if (existing != null) {
            validateExistingCinemaWebClient(existing);
            return existing;
        }

        RegisteredClient client =
                registeredClientFactory.createPublicClient(
                        new PublicClientRegistration(
                                CINEMA_WEB_CLIENT_ID,
                                "Cinema Web",
                                CINEMA_WEB_REDIRECT_URIS,
                                CINEMA_WEB_POST_LOGOUT_REDIRECT_URIS,
                                CINEMA_WEB_SCOPES));

        registeredClientRepository.save(client);

        return client;
    }

    private void validateExistingCinemaWebClient(RegisteredClient client) {

        boolean valid =
                client.getClientSecret() == null
                        && client.getClientAuthenticationMethods()
                                .equals(Set.of(ClientAuthenticationMethod.NONE))
                        && client.getAuthorizationGrantTypes()
                                .equals(Set.of(AuthorizationGrantType.AUTHORIZATION_CODE))
                        && client.getRedirectUris().equals(CINEMA_WEB_REDIRECT_URIS)
                        && client.getPostLogoutRedirectUris()
                                .equals(CINEMA_WEB_POST_LOGOUT_REDIRECT_URIS)
                        && client.getScopes().equals(CINEMA_WEB_SCOPES)
                        && client.getClientSettings().isRequireProofKey()
                        && client.getClientSettings().isRequireAuthorizationConsent();

        if (!valid) {
            throw new IllegalStateException(
                    "Existing cinema-web client does not match the required public PKCE "
                            + "configuration. Recreate only the cinema-web client before "
                            + "starting with CINEMA_LOCAL_BOOTSTRAP_ENABLED=true.");
        }
    }

    private void validateExistingSwaggerClient(RegisteredClient client) {

        if (!client.getRedirectUris().containsAll(REDIRECT_URIS)
                || !client.getScopes().containsAll(SCOPES)) {

            throw new IllegalStateException(
                    "Existing cinema-swagger client does not match local bootstrap configuration. "
                            + "Reset cinema_user_db or recreate the client before starting with "
                            + "CINEMA_LOCAL_BOOTSTRAP_ENABLED=true.");
        }
    }

    private void bootstrapConsent(User admin, RegisteredClient client) {

        OAuth2AuthorizationConsent existing =
                authorizationConsentService.findById(client.getId(), admin.getUsername());

        if (existing != null && existing.getScopes().containsAll(SCOPES)) {
            return;
        }

        OAuth2AuthorizationConsent.Builder builder =
                OAuth2AuthorizationConsent.withId(client.getId(), admin.getUsername());

        SCOPES.forEach(builder::scope);

        authorizationConsentService.save(builder.build());
    }
}
