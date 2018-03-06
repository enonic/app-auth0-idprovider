package com.enonic.app.auth0.impl.user;

import java.time.Instant;

public interface Auth0User
{
    String getUserId();

    String getName();

    String getFamilyName();

    String getGivenName();

    String getMiddleName();

    String getNickname();

    String getPicture();

    Instant getUpdatedAt();

    String getEmail();

    Boolean isEmailVerified();
}
