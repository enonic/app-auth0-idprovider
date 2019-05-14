package com.enonic.app.auth0.impl;

import java.util.concurrent.Callable;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.google.common.collect.ImmutableSet;

import com.enonic.xp.context.Context;
import com.enonic.xp.context.ContextAccessor;
import com.enonic.xp.context.ContextBuilder;
import com.enonic.xp.data.PropertyTree;
import com.enonic.xp.security.IdProvider;
import com.enonic.xp.security.IdProviderConfig;
import com.enonic.xp.security.IdProviderKey;
import com.enonic.xp.security.PrincipalKey;
import com.enonic.xp.security.PrincipalKeys;
import com.enonic.xp.security.RoleKeys;
import com.enonic.xp.security.SecurityService;
import com.enonic.xp.security.User;
import com.enonic.xp.security.auth.AuthenticationInfo;

@Component(service = Auth0ConfigurationService.class)
public class Auth0ConfigurationService
{
    private SecurityService securityService;

    public String getAppDomain( final IdProviderKey idProviderKey )
    {
        return getStringProperty( idProviderKey, "appDomain" );
    }

    public String getAppClientId( final IdProviderKey idProviderKey )
    {
        return getStringProperty( idProviderKey, "appClientId" );
    }

    public String getAppSecret( final IdProviderKey idProviderKey )
    {
        return getStringProperty( idProviderKey, "appSecret" );
    }

    public PrincipalKeys getDefaultPrincipals( final IdProviderKey idProviderKey )
    {
        final ImmutableSet.Builder<PrincipalKey> principalKeySet = ImmutableSet.builder();
        for ( String propertyValue : getStringProperties( idProviderKey, "defaultPrincipals" ) )
        {
            principalKeySet.add( PrincipalKey.from( propertyValue ) );
        }
        return PrincipalKeys.from( principalKeySet.build() );
    }

    private String getStringProperty( final IdProviderKey idProviderKey, final String propertyPath )
    {
        final PropertyTree propertyTree = retrieveConfig( idProviderKey );
        return propertyTree == null ? null : propertyTree.getString( propertyPath );
    }

    private Iterable<String> getStringProperties( final IdProviderKey idProviderKey, final String propertyPath )
    {
        final PropertyTree propertyTree = retrieveConfig( idProviderKey );
        return propertyTree == null ? null : propertyTree.getStrings( propertyPath );
    }


    private PropertyTree retrieveConfig( final IdProviderKey idProviderKey )
    {
        final IdProvider idProvider =
            idProviderKey == null ? null : runWithAdminRole( () -> securityService.getIdProvider( idProviderKey ) );
        final IdProviderConfig idProviderConfig = idProvider == null ? null : idProvider.getIdProviderConfig();
        return idProviderConfig == null ? null : idProviderConfig.getConfig();
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
