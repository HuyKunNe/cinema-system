package com.cinema.user.dto.request;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ChangeCurrentUserPasswordRequestTest {

    @Test
    void toStringShouldNotExposePasswords() {

        ChangeCurrentUserPasswordRequest request =
                new ChangeCurrentUserPasswordRequest(
                        "current-password",
                        "new-password-value");

        assertThat(request.toString())
                .doesNotContain("current-password")
                .doesNotContain("new-password-value")
                .contains("REDACTED");
    }
}
