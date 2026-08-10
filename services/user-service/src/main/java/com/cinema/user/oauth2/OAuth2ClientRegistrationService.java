package com.cinema.user.oauth2;

import com.cinema.user.oauth2.model.PublicClientRegistration;
import com.cinema.user.oauth2.model.RegisteredClientRegistrationResult;
import com.cinema.user.oauth2.model.ServiceClientRegistration;

public interface OAuth2ClientRegistrationService {

    RegisteredClientRegistrationResult registerPublicClient(
            PublicClientRegistration registration);

    RegisteredClientRegistrationResult registerServiceClient(
            ServiceClientRegistration registration);
}
