package com.enonic.app.auth0.impl;

import java.util.Optional;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.google.common.collect.ImmutableSet;

import com.enonic.app.auth0.Auth0ConfigurationService;
import com.enonic.xp.context.ContextBuilder;
import com.enonic.xp.data.PropertyTree;
import com.enonic.xp.security.AuthConfig;
import com.enonic.xp.security.PathGuard;
import com.enonic.xp.security.PrincipalKey;
import com.enonic.xp.security.PrincipalKeys;
import com.enonic.xp.security.RoleKeys;
import com.enonic.xp.security.SecurityService;
import com.enonic.xp.security.User;
import com.enonic.xp.security.auth.AuthenticationInfo;

@Component
public class Auth0ConfigurationServiceImpl
    implements Auth0ConfigurationService
{
    private SecurityService securityService;

    @Override
    public String getAppDomain( final String path )
    {
        return getStringProperty( path, "app.domain" );
    }

    @Override
    public String getAppClientId( final String path )
    {
        return getStringProperty( path, "app.clientId" );
    }

    @Override
    public String getAppSecret( final String path )
    {
        return getStringProperty( path, "app.secret" );
    }

    @Override
    public String getUserStore( final String path )
    {
        return getStringProperty( path, "userStore" );
    }

    @Override
    public PrincipalKeys getDefaultRoles( final String path )
    {
        final ImmutableSet.Builder<PrincipalKey> principalKeySet = ImmutableSet.builder();
        for ( String propertyValue : getStringProperties( path, "defaultRoles" ) )
        {
            principalKeySet.add( PrincipalKey.from( propertyValue ) );
        }
        return PrincipalKeys.from( principalKeySet.build() );
    }

    private String getStringProperty( final String path, final String propertyPath )
    {
        final PropertyTree propertyTree = getConfig( path );
        return propertyTree == null ? null : propertyTree.getString( propertyPath );
    }

    private Iterable<String> getStringProperties( final String path, final String propertyPath )
    {
        final PropertyTree propertyTree = getConfig( path );
        return propertyTree == null ? null : propertyTree.getStrings( propertyPath );
    }


    private PropertyTree getConfig( final String path )
    {
        final AuthenticationInfo authInfo = AuthenticationInfo.create().
            user( User.ANONYMOUS ).
            principals( RoleKeys.ADMIN ).
            build();
        final Optional<PathGuard> pathGuard = ContextBuilder.create().
            authInfo( authInfo ).
            build().
            callWith( () -> securityService.getPathGuardByPath( path ) );

        if ( pathGuard.isPresent() )
        {
            final AuthConfig authConfig = pathGuard.get().getAuthConfig();
            if ( authConfig != null )
            {
                return authConfig.getConfig();
            }
        }
        return null;
    }

    @Reference
    public void setSecurityService( final SecurityService securityService )
    {
        this.securityService = securityService;
    }
}
