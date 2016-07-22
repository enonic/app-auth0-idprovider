package com.enonic.app.auth0.impl;

import java.util.concurrent.Callable;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.google.common.collect.ImmutableSet;

import com.enonic.app.auth0.Auth0ConfigurationService;
import com.enonic.xp.context.Context;
import com.enonic.xp.context.ContextAccessor;
import com.enonic.xp.context.ContextBuilder;
import com.enonic.xp.data.PropertyTree;
import com.enonic.xp.security.AuthConfig;
import com.enonic.xp.security.PrincipalKey;
import com.enonic.xp.security.PrincipalKeys;
import com.enonic.xp.security.RoleKeys;
import com.enonic.xp.security.SecurityService;
import com.enonic.xp.security.User;
import com.enonic.xp.security.UserStore;
import com.enonic.xp.security.UserStoreKey;
import com.enonic.xp.security.auth.AuthenticationInfo;

@Component
public class Auth0ConfigurationServiceImpl
    implements Auth0ConfigurationService
{
    private SecurityService securityService;

    @Override
    public String getAppDomain( final UserStoreKey userStoreKey )
    {
        return getStringProperty( userStoreKey, "appDomain" );
    }

    @Override
    public String getAppClientId( final UserStoreKey userStoreKey )
    {
        return getStringProperty( userStoreKey, "appClientId" );
    }

    @Override
    public String getAppSecret( final UserStoreKey userStoreKey )
    {
        return getStringProperty( userStoreKey, "appSecret" );
    }

    @Override
    public PrincipalKeys getDefaultGroups( final UserStoreKey userStoreKey )
    {
        final ImmutableSet.Builder<PrincipalKey> principalKeySet = ImmutableSet.builder();
        for ( String propertyValue : getStringProperties( userStoreKey, "defaultGroups" ) )
        {
            principalKeySet.add( PrincipalKey.from( propertyValue ) );
        }
        return PrincipalKeys.from( principalKeySet.build() );
    }

    private String getStringProperty( final UserStoreKey userStoreKey, final String propertyPath )
    {
        final PropertyTree propertyTree = retrieveConfig( userStoreKey );
        return propertyTree == null ? null : propertyTree.getString( propertyPath );
    }

    private Iterable<String> getStringProperties( final UserStoreKey userStoreKey, final String propertyPath )
    {
        final PropertyTree propertyTree = retrieveConfig( userStoreKey );
        return propertyTree == null ? null : propertyTree.getStrings( propertyPath );
    }


    private PropertyTree retrieveConfig( final UserStoreKey userStoreKey )
    {
        final UserStore userStore = userStoreKey == null ? null : runWithAdminRole( () -> securityService.getUserStore( userStoreKey ) );
        final AuthConfig authConfig = userStore == null ? null : userStore.getAuthConfig();
        return authConfig == null ? null : authConfig.getConfig();
    }

    private <T> T runWithAdminRole( final Callable<T> callable )
    {
        final Context context = ContextAccessor.current();
        final AuthenticationInfo authenticationInfo = AuthenticationInfo.create().
            user( User.ANONYMOUS ).
            principals( RoleKeys.ADMIN ).
            build();
        return ContextBuilder.from( context ).
            authInfo( authenticationInfo ).
            build().
            callWith( callable );
    }

    @Reference
    public void setSecurityService( final SecurityService securityService )
    {
        this.securityService = securityService;
    }
}
