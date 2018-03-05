package com.enonic.app.auth0.impl.user;

public interface Auth0User
{
    String getUserId();

    String getEmail();

    String getName();

    String getNickname();

    String getPicture();

    Boolean isEmailVerified();
}
