package com.cinema.user.service.impl;

import com.cinema.common.exception.exception.InternalServerException;
import com.cinema.user.dto.request.RegisterUserRequest;
import com.cinema.user.dto.response.RegistrationAcceptedResponse;
import com.cinema.user.exception.UserErrorCode;
import com.cinema.user.repository.PendingUserRegistrationRepository;
import com.cinema.user.repository.UserRepository;
import com.cinema.user.service.UserRegistrationService;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.Locale;

@Service
public class UserRegistrationServiceImpl implements UserRegistrationService {

    private final ObjectProvider<JavaMailSender> mailSenders;
    private final UserRegistrationTransaction registrationTransaction;
    private final UserRepository userRepository;
    private final PendingUserRegistrationRepository pendingRegistrationRepository;

    public UserRegistrationServiceImpl(
            ObjectProvider<JavaMailSender> mailSenders,
            UserRegistrationTransaction registrationTransaction,
            UserRepository userRepository,
            PendingUserRegistrationRepository pendingRegistrationRepository) {
        this.mailSenders = mailSenders;
        this.registrationTransaction = registrationTransaction;
        this.userRepository = userRepository;
        this.pendingRegistrationRepository = pendingRegistrationRepository;
    }

    @Override
    public RegistrationAcceptedResponse register(RegisterUserRequest request) {
        JavaMailSender mailSender = mailSenders.getIfAvailable();
        if (mailSender == null) {
            throw new InternalServerException(
                    UserErrorCode.REGISTRATION_EMAIL_DELIVERY_UNAVAILABLE);
        }
        try {
            registrationTransaction.createOrRefresh(request, mailSender);
        } catch (DataIntegrityViolationException concurrentRegistration) {
            String normalizedEmail =
                    Normalizer.normalize(request.email().trim(), Normalizer.Form.NFKC)
                            .toLowerCase(Locale.ROOT);
            if (!userRepository.existsByNormalizedEmail(normalizedEmail)
                    && !pendingRegistrationRepository.existsByNormalizedEmail(normalizedEmail)) {
                throw concurrentRegistration;
            }
        }
        return new RegistrationAcceptedResponse(UserRegistrationTransaction.acceptedMessage());
    }
}
