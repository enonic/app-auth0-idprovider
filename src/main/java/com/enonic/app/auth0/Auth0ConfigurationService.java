package com.enonic.app.auth0;

import com.enonic.xp.security.PrincipalKeys;
import com.enonic.xp.security.UserStoreKey;

public interface Auth0ConfigurationService
{
    String getAppDomain( final String path );

    String getAppClientId( final String path );

    String getAppSecret( final String path );

    UserStoreKey getUserStoreKey( final String path );

    PrincipalKeys getDefaultRoles( final String path );
}
