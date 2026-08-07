package com.cinema.user.security;

import java.time.Clock;
import java.time.OffsetDateTime;

import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsPasswordService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cinema.user.entity.UserCredential;
import com.cinema.user.repository.UserCredentialRepository;

@Service
public class JpaUserDetailsPasswordService
        implements UserDetailsPasswordService {

    private static final String PASSWORD_ALGORITHM = "bcrypt";

    private final UserCredentialRepository userCredentialRepository;

    private final Clock clock;

    public JpaUserDetailsPasswordService(
            UserCredentialRepository userCredentialRepository,
            Clock clock) {

        this.userCredentialRepository = userCredentialRepository;

        this.clock = clock;
    }

    @Override
    @Transactional
    public UserDetails updatePassword(
            UserDetails userDetails,
            String newPassword) {

        if (!(userDetails instanceof CinemaUserDetails principal)) {

            throw new InternalAuthenticationServiceException("Unable to update credentials");
        }

        UserCredential credential = userCredentialRepository
                .findByUser_Id(principal.getUserId())
                .orElseThrow(() -> new InternalAuthenticationServiceException("Unable to update credentials"));

        credential.changePassword(
                newPassword,
                PASSWORD_ALGORITHM,
                OffsetDateTime.now(clock));

        return new CinemaUserDetails(
                principal.getUserId(),
                principal.getUsername(),
                newPassword,
                credential.getUser().getStatus(),
                principal.getAuthorities());
    }
}
