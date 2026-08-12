package com.cinema.user.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;

import com.cinema.user.config.JwtClaimsProperties;
import com.cinema.user.enums.AccountStatus;
import com.cinema.user.security.CinemaUserDetails;

class CinemaJwtTokenCustomizerTest {

    private static final UUID USER_ID = UUID.fromString(
            "0198f4d2-2ea7-7abc-8c3d-a4f812345678");
    private static final String USERNAME = "customer@example.com";
    private static final String CLIENT_ID = "inventory-service";

    private CinemaJwtTokenCustomizer customizer;

    @BeforeEach
    void setUp() {
        customizer = new CinemaJwtTokenCustomizer(
                new JwtClaimsProperties(List.of("cinema-api")));
    }

    @Test
    void shouldIgnoreNonAccessToken() {
        JwtEncodingContext context = mock(JwtEncodingContext.class);

        when(context.getTokenType())
                .thenReturn(new OAuth2TokenType("id_token"));

        customizer.customize(context);

        verify(context).getTokenType();
        verifyNoMoreInteractions(context);
    }

    @Test
    void shouldAddAudienceToUserAccessToken() {
        JwtClaimsSet claims = customizeUserToken(
                userDetails(USER_ID, defaultAuthorities()));

        assertThat(claims.getAudience())
                .containsExactly("cinema-api");
    }

    @Test
    void shouldUseUuidV7UserIdAsSubject() {
        assertThat(USER_ID.version()).isEqualTo(7);

        JwtClaimsSet claims = customizeUserToken(
                userDetails(USER_ID, defaultAuthorities()));

        assertThat(claims.getSubject())
                .isEqualTo(USER_ID.toString());
    }

    @Test
    void shouldAddUsernameRolesAndPermissions() {
        JwtClaimsSet claims = customizeUserToken(
                userDetails(USER_ID, defaultAuthorities()));

        assertThat(claims.getClaimAsString("username"))
                .isEqualTo(USERNAME);
        assertThat(claims.getClaimAsStringList("roles"))
                .containsExactly("ADMIN", "CUSTOMER");
        assertThat(claims.getClaimAsStringList("permissions"))
                .containsExactly("cinema:read", "showtime:write");
    }

    @Test
    void shouldUseJdbcSafeMutableCollectionsForUserClaims() {
        JwtClaimsSet claims = customizeUserToken(
                userDetails(
                        USER_ID,
                        defaultAuthorities()));

        Object roles = claims.getClaim("roles");
        Object permissions = claims.getClaim(
                "permissions");

        assertThat(roles)
                .isInstanceOf(ArrayList.class);

        assertThat(permissions)
                .isInstanceOf(ArrayList.class);
    }

    @Test
    void shouldSortAndDeduplicateRolesAndPermissions() {
        CinemaUserDetails principal = userDetails(
                USER_ID,
                List.of(
                        authority("showtime:write"),
                        authority("ROLE_CUSTOMER"),
                        authority("cinema:read"),
                        authority("ROLE_ADMIN"),
                        authority("showtime:write"),
                        authority("ROLE_ADMIN")));

        JwtClaimsSet claims = customizeUserToken(principal);

        assertThat(claims.getClaimAsStringList("roles"))
                .containsExactly("ADMIN", "CUSTOMER");
        assertThat(claims.getClaimAsStringList("permissions"))
                .containsExactly("cinema:read", "showtime:write");
    }

    @Test
    void shouldRejectNonCinemaUserPrincipal() {
        JwtEncodingContext context = userContext(
                new UsernamePasswordAuthenticationToken(
                        "ordinary-principal",
                        null,
                        List.of()));

        assertThatThrownBy(() -> customizer.customize(context))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(
                        "Authenticated user principal is invalid for JWT issuance");
    }

    @Test
    void shouldRejectNonUuidV7UserSubject() {
        UUID versionFourUserId = UUID.fromString(
                "11111111-1111-4111-8111-111111111111");

        assertThat(versionFourUserId.version()).isEqualTo(4);

        JwtEncodingContext context = userContext(
                authenticated(userDetails(
                        versionFourUserId,
                        defaultAuthorities())));

        assertThatThrownBy(() -> customizer.customize(context))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("JWT user subject must use UUID version 7");
    }

    @Test
    void shouldAddClientIdAndScopesToServiceToken() {
        JwtClaimsSet claims = customizeServiceToken(Set.of(
                "inventory:write",
                "inventory:read"));

        assertThat(claims.getSubject()).isEqualTo(CLIENT_ID);
        assertThat(claims.getAudience()).containsExactly("cinema-api");
        assertThat(claims.getClaimAsStringList("permissions"))
                .containsExactly("inventory:read", "inventory:write");
    }

    @Test
    void shouldUseJdbcSafeMutableCollectionForServicePermissions() {
        JwtClaimsSet claims = customizeServiceToken(
                Set.of(
                        "inventory:write",
                        "inventory:read"));

        Object permissions = claims.getClaim(
                "permissions");

        assertThat(permissions)
                .isInstanceOf(ArrayList.class);
    }

    @Test
    void serviceTokenShouldNotContainUsernameOrRoles() {
        JwtClaimsSet claims = customizeServiceToken(Set.of(
                "inventory:write"));

        assertThat(claims.getClaims())
                .doesNotContainKeys("username", "roles");
    }

    @Test
    void shouldLimitUserPermissionsToAuthorizedScopes() {
        CinemaUserDetails principal = userDetails(
                USER_ID,
                List.of(
                        authority("ROLE_USER"),
                        authority("booking:read"),
                        authority("booking:create"),
                        authority("booking:cancel")));

        JwtEncodingContext context = userContext(
                authenticated(principal));

        when(context.getAuthorizedScopes())
                .thenReturn(Set.of("booking:read"));

        customizer.customize(context);

        JwtClaimsSet claims = context.getClaims().build();

        assertThat(claims.getClaimAsStringList("permissions"))
                .containsExactly("booking:read");
    }

    private JwtClaimsSet customizeUserToken(CinemaUserDetails principal) {
        JwtEncodingContext context = userContext(authenticated(principal));
        customizer.customize(context);
        return context.getClaims().build();
    }

    private JwtEncodingContext userContext(Authentication authentication) {
        JwtEncodingContext context = mock(JwtEncodingContext.class);
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder();

        when(context.getTokenType()).thenReturn(OAuth2TokenType.ACCESS_TOKEN);
        when(context.getAuthorizationGrantType())
                .thenReturn(AuthorizationGrantType.AUTHORIZATION_CODE);
        when(context.getPrincipal()).thenReturn(authentication);
        when(context.getClaims()).thenReturn(claims);
        when(context.getAuthorizedScopes())
                .thenReturn(Set.of(
                        "cinema:read",
                        "showtime:write"));
        return context;
    }

    private JwtClaimsSet customizeServiceToken(Set<String> scopes) {
        JwtEncodingContext context = mock(JwtEncodingContext.class);
        RegisteredClient registeredClient = mock(RegisteredClient.class);
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder();

        when(context.getTokenType()).thenReturn(OAuth2TokenType.ACCESS_TOKEN);
        when(context.getAuthorizationGrantType())
                .thenReturn(AuthorizationGrantType.CLIENT_CREDENTIALS);
        when(context.getRegisteredClient()).thenReturn(registeredClient);
        when(context.getAuthorizedScopes()).thenReturn(scopes);
        when(context.getClaims()).thenReturn(claims);
        when(registeredClient.getClientId()).thenReturn(CLIENT_ID);

        customizer.customize(context);
        return claims.build();
    }

    private CinemaUserDetails userDetails(
            UUID userId,
            List<GrantedAuthority> authorities) {

        return new CinemaUserDetails(
                userId,
                USERNAME,
                "encoded-password",
                AccountStatus.ACTIVE,
                authorities);
    }

    private Authentication authenticated(CinemaUserDetails principal) {
        return new UsernamePasswordAuthenticationToken(
                principal,
                null,
                principal.getAuthorities());
    }

    private List<GrantedAuthority> defaultAuthorities() {
        return List.of(
                authority("showtime:write"),
                authority("ROLE_CUSTOMER"),
                authority("cinema:read"),
                authority("ROLE_ADMIN"));
    }

    private GrantedAuthority authority(String value) {
        return new SimpleGrantedAuthority(value);
    }
}
