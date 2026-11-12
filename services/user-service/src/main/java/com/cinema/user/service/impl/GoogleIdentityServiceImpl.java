package com.cinema.user.service.impl;

import com.cinema.user.entity.Role;
import com.cinema.user.entity.User;
import com.cinema.user.entity.UserCredential;
import com.cinema.user.entity.UserExternalIdentity;
import com.cinema.user.entity.UserProfile;
import com.cinema.user.entity.UserRole;
import com.cinema.user.enums.AccountStatus;
import com.cinema.user.enums.RoleName;
import com.cinema.user.repository.RoleRepository;
import com.cinema.user.repository.UserCredentialRepository;
import com.cinema.user.repository.UserExternalIdentityRepository;
import com.cinema.user.repository.UserProfileRepository;
import com.cinema.user.repository.UserRepository;
import com.cinema.user.repository.UserRoleRepository;
import com.cinema.user.security.audit.SecurityAuditEventType;
import com.cinema.user.security.audit.SecurityAuditOutcome;
import com.cinema.user.security.audit.SecurityAuditRecord;
import com.cinema.user.security.audit.SecurityAuditRecorder;
import com.cinema.user.security.audit.SecurityAuditTargetType;
import com.cinema.user.service.GoogleIdentityService;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.text.Normalizer;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class GoogleIdentityServiceImpl implements GoogleIdentityService {

    private static final Set<String> GOOGLE_ISSUERS =
            Set.of("https://accounts.google.com", "accounts.google.com");
    private static final String PASSWORD_ALGORITHM = "bcrypt";

    private final UserExternalIdentityRepository externalIdentityRepository;
    private final UserRepository userRepository;
    private final UserCredentialRepository credentialRepository;
    private final UserProfileRepository profileRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecurityAuditRecorder securityAuditRecorder;
    private final SecureRandom secureRandom;
    private final Clock clock;

    public GoogleIdentityServiceImpl(
            UserExternalIdentityRepository externalIdentityRepository,
            UserRepository userRepository,
            UserCredentialRepository credentialRepository,
            UserProfileRepository profileRepository,
            UserRoleRepository userRoleRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            SecurityAuditRecorder securityAuditRecorder,
            SecureRandom secureRandom,
            Clock clock) {
        this.externalIdentityRepository = externalIdentityRepository;
        this.userRepository = userRepository;
        this.credentialRepository = credentialRepository;
        this.profileRepository = profileRepository;
        this.userRoleRepository = userRoleRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.securityAuditRecorder = securityAuditRecorder;
        this.secureRandom = secureRandom;
        this.clock = clock;
    }

    @Override
    @Transactional
    public GoogleAccount resolveOrCreate(
            String issuer,
            String subject,
            String email,
            Boolean emailVerified,
            String firstName,
            String lastName) {
        String normalizedEmail = normalizeRequired(email);
        if (issuer == null
                || !GOOGLE_ISSUERS.contains(issuer)
                || subject == null
                || subject.isBlank()
                || subject.length() > 255
                || !Boolean.TRUE.equals(emailVerified)) {
            throw unavailable();
        }

        var externalIdentity = externalIdentityRepository.findByIssuerAndSubject(issuer, subject);
        if (externalIdentity.isPresent()) {
            return requireActive(externalIdentity.get().getUser());
        }

        User user = userRepository.findByNormalizedEmail(normalizedEmail).orElse(null);
        OffsetDateTime now = OffsetDateTime.now(clock);
        if (user == null) {
            user = createVerifiedUser(email.trim(), normalizedEmail, firstName, lastName, now);
            securityAuditRecorder.record(
                    new SecurityAuditRecord(
                            SecurityAuditEventType.USER_REGISTERED,
                            SecurityAuditTargetType.USER,
                            user.getId().toString(),
                            SecurityAuditOutcome.SUCCESS,
                            null,
                            "method=google"));
        } else if (user.getStatus() != AccountStatus.ACTIVE || user.getEmailVerifiedAt() == null) {
            throw unavailable();
        }

        UserExternalIdentity identity = new UserExternalIdentity(user, issuer, subject);
        try {
            externalIdentityRepository.saveAndFlush(identity);
        } catch (DataIntegrityViolationException exception) {
            throw exception;
        }
        return new GoogleAccount(user.getId(), user.getUsername());
    }

    private User createVerifiedUser(
            String email,
            String normalizedEmail,
            String firstName,
            String lastName,
            OffsetDateTime now) {
        String username = "g" + UUID.randomUUID().toString().replace("-", "");
        User user = new User(email, normalizedEmail, username, normalizeRequired(username));
        user.verifyEmail(now);
        user = userRepository.saveAndFlush(user);

        byte[] passwordBytes = new byte[32];
        secureRandom.nextBytes(passwordBytes);
        String unusablePassword =
                Base64.getUrlEncoder().withoutPadding().encodeToString(passwordBytes);
        credentialRepository.saveAndFlush(
                new UserCredential(
                        user, passwordEncoder.encode(unusablePassword), PASSWORD_ALGORITHM, now));

        Role userRole =
                roleRepository
                        .findByName(RoleName.USER)
                        .orElseThrow(() -> new IllegalStateException("USER role is not seeded"));
        userRoleRepository.saveAndFlush(new UserRole(user, userRole, now, null));
        profileRepository.saveAndFlush(
                new UserProfile(user, clean(firstName), clean(lastName), null));
        return user;
    }

    private static GoogleAccount requireActive(User user) {
        if (user.getStatus() != AccountStatus.ACTIVE || user.getEmailVerifiedAt() == null) {
            throw unavailable();
        }
        return new GoogleAccount(user.getId(), user.getUsername());
    }

    private static String normalizeRequired(String value) {
        if (value == null || value.isBlank() || value.length() > 320) {
            throw unavailable();
        }
        return Normalizer.normalize(value.trim(), Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);
    }

    private static String clean(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static OAuth2AuthenticationException unavailable() {
        return new OAuth2AuthenticationException(
                new OAuth2Error("google_account_unavailable"),
                "Google account is not eligible for sign-in");
    }
}
