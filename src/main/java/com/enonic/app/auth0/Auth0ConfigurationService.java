package com.enonic.app.auth0;

import com.enonic.xp.security.PrincipalKeys;

public interface Auth0ConfigurationService
{
    String getAppDomain( final String path );

    String getAppClientId( final String path );

    String getAppSecret( final String path );

    String getUserStore( final String path );

    PrincipalKeys getDefaultRoles( final String path );
}
