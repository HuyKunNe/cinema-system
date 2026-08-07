package com.cinema.user.security;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cinema.user.entity.Role;
import com.cinema.user.entity.User;
import com.cinema.user.entity.UserCredential;
import com.cinema.user.entity.UserRole;
import com.cinema.user.enums.RoleName;
import com.cinema.user.repository.RolePermissionRepository;
import com.cinema.user.repository.UserCredentialRepository;
import com.cinema.user.repository.UserRepository;
import com.cinema.user.repository.UserRoleRepository;

@Service
@Transactional(readOnly = true)
public class JpaUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final UserCredentialRepository userCredentialRepository;
    private final UserRoleRepository userRoleRepository;
    private final RolePermissionRepository rolePermissionRepository;

    public JpaUserDetailsService(UserRepository userRepository,
            UserCredentialRepository userCredentialRepository,
            UserRoleRepository userRoleRepository,
            RolePermissionRepository rolePermissionRepository) {
        this.userRepository = userRepository;
        this.userCredentialRepository = userCredentialRepository;
        this.userRoleRepository = userRoleRepository;
        this.rolePermissionRepository = rolePermissionRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String identifier)
            throws UsernameNotFoundException {

        String normalizedIdentifier = normalizeIdentifier(identifier);

        User user = userRepository
                .findByNormalizedEmailOrNormalizedUsername(
                        normalizedIdentifier,
                        normalizedIdentifier)
                .orElseThrow(
                        JpaUserDetailsService::invalidCredentials);

        UserCredential credential = userCredentialRepository
                .findByUser_Id(user.getId())
                .orElseThrow(
                        JpaUserDetailsService::invalidCredentials);

        Set<String> authorityValues = new TreeSet<>();

        userRoleRepository
                .findAllByUser_Id(user.getId())
                .stream()
                .map(UserRole::getRole)
                .map(Role::getName)
                .map(RoleName::name)
                .map(role -> "ROLE_" + role)
                .forEach(authorityValues::add);

        rolePermissionRepository
                .findEffectivePermissionCodesByUserId(
                        user.getId())
                .stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(permission -> !permission.isBlank())
                .forEach(authorityValues::add);

        List<GrantedAuthority> authorities = authorityValues.stream()
                .map(SimpleGrantedAuthority::new)
                .map(GrantedAuthority.class::cast)
                .toList();

        return new CinemaUserDetails(
                user.getId(),
                user.getUsername(),
                credential.getPasswordHash(),
                user.getStatus(),
                authorities);
    }

    private static String normalizeIdentifier(
            String identifier) {

        if (identifier == null || identifier.isBlank()) {
            throw invalidCredentials();
        }

        return identifier
                .trim()
                .toLowerCase(Locale.ROOT);
    }

    private static UsernameNotFoundException invalidCredentials() {

        return new UsernameNotFoundException(
                "Invalid credentials");
    }
}
