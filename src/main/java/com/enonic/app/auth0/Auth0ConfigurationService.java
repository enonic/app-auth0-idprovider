package com.enonic.app.auth0;

import com.enonic.xp.security.PrincipalKeys;
import com.enonic.xp.security.UserStoreKey;

public interface Auth0ConfigurationService
{
    String getAppDomain( final UserStoreKey userStoreKey );

    String getAppClientId( final UserStoreKey userStoreKey );

    String getAppSecret( final UserStoreKey userStoreKey );

    PrincipalKeys getDefaultPrincipals( final UserStoreKey userStoreKey );
}
