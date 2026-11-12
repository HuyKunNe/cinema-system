package com.cinema.user.service.impl;

import com.cinema.common.exception.exception.InternalServerException;
import com.cinema.user.config.EmailVerificationProperties;
import com.cinema.user.config.UserRegistrationProperties;
import com.cinema.user.dto.request.RegisterUserRequest;
import com.cinema.user.entity.PendingUserRegistration;
import com.cinema.user.exception.UserErrorCode;
import com.cinema.user.repository.PendingUserRegistrationRepository;
import com.cinema.user.repository.UserRepository;
import com.cinema.user.security.EmailVerificationTokenCodec;

import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.text.Normalizer;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Locale;

@Service
public class UserRegistrationTransaction {

    private static final String ACCEPTED_MESSAGE =
            "If this email can be registered, a verification link will be sent.";

    private final UserRepository userRepository;
    private final PendingUserRegistrationRepository pendingRegistrationRepository;
    private final EmailVerificationTokenCodec tokenCodec;
    private final EmailVerificationProperties verificationProperties;
    private final UserRegistrationProperties properties;
    private final Clock clock;

    public UserRegistrationTransaction(
            UserRepository userRepository,
            PendingUserRegistrationRepository pendingRegistrationRepository,
            EmailVerificationTokenCodec tokenCodec,
            EmailVerificationProperties verificationProperties,
            UserRegistrationProperties properties,
            Clock clock) {
        this.userRepository = userRepository;
        this.pendingRegistrationRepository = pendingRegistrationRepository;
        this.tokenCodec = tokenCodec;
        this.verificationProperties = verificationProperties;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public void createOrRefresh(RegisterUserRequest request, JavaMailSender mailSender) {
        String email = request.email().trim();
        String normalizedEmail = normalize(email);
        if (userRepository.existsByNormalizedEmail(normalizedEmail)) {
            return;
        }

        OffsetDateTime now = OffsetDateTime.now(clock);
        PendingUserRegistration pending =
                pendingRegistrationRepository.findByNormalizedEmail(normalizedEmail).orElse(null);
        if (pending != null
                && pending.getUpdatedAt() != null
                && pending.getUpdatedAt().plus(properties.resendCooldown()).isAfter(now)) {
            return;
        }

        String rawToken = tokenCodec.generateRawToken();
        OffsetDateTime expiresAt = now.plus(verificationProperties.lifetime());
        if (pending == null) {
            pending =
                    new PendingUserRegistration(
                            email,
                            normalizedEmail,
                            clean(request.firstName()),
                            clean(request.lastName()),
                            clean(request.phoneNumber()),
                            tokenCodec.hash(rawToken),
                            expiresAt);
        } else {
            pending.replace(
                    email,
                    clean(request.firstName()),
                    clean(request.lastName()),
                    clean(request.phoneNumber()),
                    tokenCodec.hash(rawToken),
                    expiresAt);
        }
        pendingRegistrationRepository.saveAndFlush(pending);
        sendVerificationEmail(mailSender, email, rawToken, expiresAt);
    }

    private void sendVerificationEmail(
            JavaMailSender mailSender, String email, String rawToken, OffsetDateTime expiresAt) {
        String verificationLink =
                UriComponentsBuilder.fromUriString(properties.verificationUrl())
                        .fragment("token=" + rawToken)
                        .build()
                        .toUriString();
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(properties.emailFrom());
        message.setTo(email);
        message.setSubject("Xác minh email Cinematic");
        message.setText(
                "Mở liên kết sau, chọn mật khẩu và xác nhận để hoàn tất đăng ký:"
                        + System.lineSeparator()
                        + verificationLink
                        + System.lineSeparator()
                        + System.lineSeparator()
                        + "Liên kết hết hạn lúc "
                        + expiresAt
                        + ". Nếu bạn không yêu cầu đăng ký, hãy bỏ qua email này.");
        try {
            mailSender.send(message);
        } catch (MailException exception) {
            throw new InternalServerException(
                    UserErrorCode.REGISTRATION_EMAIL_DELIVERY_UNAVAILABLE, exception);
        }
    }

    private static String normalize(String value) {
        return Normalizer.normalize(value.trim(), Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);
    }

    private static String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public static String acceptedMessage() {
        return ACCEPTED_MESSAGE;
    }
}
