package com.cinema.user.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.cinema.user.entity.Role;
import com.cinema.user.entity.User;
import com.cinema.user.entity.UserCredential;
import com.cinema.user.entity.UserRole;
import com.cinema.user.enums.AccountStatus;
import com.cinema.user.enums.RoleName;
import com.cinema.user.repository.RolePermissionRepository;
import com.cinema.user.repository.UserCredentialRepository;
import com.cinema.user.repository.UserRepository;
import com.cinema.user.repository.UserRoleRepository;

@ExtendWith(MockitoExtension.class)
public class JpaUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserCredentialRepository userCredentialRepository;

    @Mock
    private UserRoleRepository userRoleRepository;

    @Mock
    private RolePermissionRepository rolePermissionRepository;

    private JpaUserDetailsService userDetailsService;

    private static final UUID USER_ID = UUID.fromString(
            "019c4000-0000-7000-8000-000000000001");

    private static final String PASSWORD_HASH = "{bcrypt}$2a$10$encoded";

    @BeforeEach
    void setUp() {
        userDetailsService = new JpaUserDetailsService(
                userRepository,
                userCredentialRepository,
                userRoleRepository,
                rolePermissionRepository);
    }

    @Test
    void shouldLoadUserByNormalizedEmailOrUsername() {
        User user = mockUser(AccountStatus.ACTIVE);
        UserCredential credential = mock(UserCredential.class);

        Role role = mock(Role.class);
        UserRole assignment = mock(UserRole.class);

        when(userRepository
                .findByNormalizedEmailOrNormalizedUsername(
                        "member@example.com",
                        "member@example.com"))
                .thenReturn(Optional.of(user));

        when(userCredentialRepository
                .findByUser_Id(USER_ID))
                .thenReturn(Optional.of(credential));

        when(credential.getPasswordHash())
                .thenReturn(PASSWORD_HASH);

        when(userRoleRepository
                .findAllByUser_Id(USER_ID))
                .thenReturn(List.of(assignment));

        when(assignment.getRole()).thenReturn(role);
        when(role.getName()).thenReturn(RoleName.USER);

        when(rolePermissionRepository
                .findEffectivePermissionCodesByUserId(
                        USER_ID))
                .thenReturn(List.of(
                        "booking:read",
                        "booking:create",
                        "booking:read",
                        " ",
                        ""));

        CinemaUserDetails result = (CinemaUserDetails) userDetailsService
                .loadUserByUsername(
                        "  MEMBER@EXAMPLE.COM ");

        assertThat(result.getUserId())
                .isEqualTo(USER_ID);

        assertThat(result.getUsername())
                .isEqualTo("member");

        assertThat(result.getPassword())
                .isEqualTo(PASSWORD_HASH);

        assertThat(result.isEnabled()).isTrue();

        assertThat(result.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly(
                        "ROLE_USER",
                        "booking:create",
                        "booking:read");

        verify(rolePermissionRepository)
                .findEffectivePermissionCodesByUserId(
                        USER_ID);

        verify(rolePermissionRepository, never())
                .findAllByRole_Id(any());
    }

    @Test
    void shouldReturnGenericErrorWhenUserDoesNotExist() {
        when(userRepository
                .findByNormalizedEmailOrNormalizedUsername(
                        "missing",
                        "missing"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailsService
                .loadUserByUsername("missing"))
                .isInstanceOf(
                        UsernameNotFoundException.class)
                .hasMessage("Invalid credentials");

        verifyNoInteractions(
                userCredentialRepository,
                userRoleRepository,
                rolePermissionRepository);
    }

    @Test
    void shouldReturnSameGenericErrorWhenCredentialDoesNotExist() {
        User user = mock(User.class);

        when(user.getId())
                .thenReturn(USER_ID);

        when(userRepository
                .findByNormalizedEmailOrNormalizedUsername(
                        "member",
                        "member"))
                .thenReturn(Optional.of(user));

        when(userCredentialRepository
                .findByUser_Id(USER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailsService
                .loadUserByUsername("member"))
                .isInstanceOf(
                        UsernameNotFoundException.class)
                .hasMessage("Invalid credentials");

        verify(userRepository)
                .findByNormalizedEmailOrNormalizedUsername(
                        "member",
                        "member");

        verify(userCredentialRepository)
                .findByUser_Id(USER_ID);

        verifyNoInteractions(
                userRoleRepository,
                rolePermissionRepository);
    }

    @Test
    void shouldRejectBlankIdentifier() {
        assertThatThrownBy(() -> userDetailsService
                .loadUserByUsername(" "))
                .isInstanceOf(
                        UsernameNotFoundException.class)
                .hasMessage("Invalid credentials");

        verifyNoInteractions(
                userRepository,
                userCredentialRepository,
                userRoleRepository,
                rolePermissionRepository);
    }

    @Test
    void shouldAllowEmptyAuthorities() {
        User user = mockUser(AccountStatus.ACTIVE);
        UserCredential credential = mock(UserCredential.class);

        when(userRepository
                .findByNormalizedEmailOrNormalizedUsername(
                        "member",
                        "member"))
                .thenReturn(Optional.of(user));

        when(userCredentialRepository
                .findByUser_Id(USER_ID))
                .thenReturn(Optional.of(credential));

        when(credential.getPasswordHash())
                .thenReturn(PASSWORD_HASH);

        when(userRoleRepository
                .findAllByUser_Id(USER_ID))
                .thenReturn(List.of());

        when(rolePermissionRepository
                .findEffectivePermissionCodesByUserId(
                        USER_ID))
                .thenReturn(List.of());

        UserDetails result = userDetailsService
                .loadUserByUsername("member");

        assertThat(result.getAuthorities()).isEmpty();
    }

    @ParameterizedTest
    @EnumSource(AccountStatus.class)
    void shouldPreserveAccountStatus(
            AccountStatus status) {

        User user = mockUser(status);
        UserCredential credential = mock(UserCredential.class);

        when(userRepository
                .findByNormalizedEmailOrNormalizedUsername(
                        "member",
                        "member"))
                .thenReturn(Optional.of(user));

        when(userCredentialRepository
                .findByUser_Id(USER_ID))
                .thenReturn(Optional.of(credential));

        when(credential.getPasswordHash())
                .thenReturn(PASSWORD_HASH);

        when(userRoleRepository
                .findAllByUser_Id(USER_ID))
                .thenReturn(List.of());

        when(rolePermissionRepository
                .findEffectivePermissionCodesByUserId(
                        USER_ID))
                .thenReturn(List.of());

        UserDetails result = userDetailsService
                .loadUserByUsername("member");

        assertThat(result.isEnabled())
                .isEqualTo(
                        status.isAuthenticationAllowed());

        assertThat(result.isAccountNonLocked())
                .isEqualTo(
                        status != AccountStatus.LOCKED);
    }

    private User mockUser(AccountStatus status) {
        User user = mock(User.class);

        when(user.getId()).thenReturn(USER_ID);
        when(user.getUsername()).thenReturn("member");
        when(user.getStatus()).thenReturn(status);

        return user;
    }
}
