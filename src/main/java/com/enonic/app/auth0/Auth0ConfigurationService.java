package com.enonic.app.auth0;

import com.enonic.xp.security.PrincipalKeys;

public interface Auth0ConfigurationService
{
    public String getAppDomain( final String path );

    public String getAppClientId( final String path );

    public String getAppSecret( final String path );

    public String getUserStore( final String path );

    public PrincipalKeys getDefaultRoles( final String path );
}
