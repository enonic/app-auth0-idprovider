package com.enonic.app.auth0.impl;

import java.util.concurrent.Callable;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.auth0.Auth0User;

import com.enonic.xp.context.ContextAccessor;
import com.enonic.xp.context.ContextBuilder;
import com.enonic.xp.security.CreateUserParams;
import com.enonic.xp.security.PrincipalKey;
import com.enonic.xp.security.PrincipalKeys;
import com.enonic.xp.security.PrincipalRelationship;
import com.enonic.xp.security.RoleKeys;
import com.enonic.xp.security.SecurityService;
import com.enonic.xp.security.User;
import com.enonic.xp.security.UserStoreKey;
import com.enonic.xp.security.auth.AuthenticationInfo;
import com.enonic.xp.security.auth.VerifiedUsernameAuthToken;

@Component(service = Auth0LoginService.class)
public class Auth0LoginService
{
    private Auth0ConfigurationService configurationService;

    private SecurityService securityService;

    public void login( final HttpServletRequest request, final Auth0User auth0User, final UserStoreKey userStoreKey )
    {
        final String userId = auth0User.getUserId().replace( '|', '-' );
        final String email = auth0User.getEmail();
        final String name = auth0User.getName();

        final PrincipalKey principalKey = PrincipalKey.ofUser( userStoreKey, userId );

        //Retrieves the user
        User user = runAs( () -> securityService.getUser( principalKey ), RoleKeys.AUTHENTICATED ).orElse( null );

        //If the user does not exist
        if ( user == null )
        {
            //Creates the user
            final PrincipalKeys defaultPrincipals = configurationService.getDefaultPrincipals( userStoreKey );
            final CreateUserParams createUserParams = CreateUserParams.create().
                login( userId ).
                displayName( name ).
                email( email ).
                userKey( principalKey ).
                build();

            user = runAs( () -> {
                final User createdUser = securityService.createUser( createUserParams );
                for ( PrincipalKey defaultPrincipal : defaultPrincipals )
                {
                    securityService.addRelationship( PrincipalRelationship.from( defaultPrincipal ).to( principalKey ) );
                }
                return createdUser;
            }, RoleKeys.ADMIN );
        }

        if ( user != null )
        {
            //Authenticates the user
            final VerifiedUsernameAuthToken verifiedUsernameAuthToken = new VerifiedUsernameAuthToken();
            verifiedUsernameAuthToken.setUserStore( userStoreKey );
            verifiedUsernameAuthToken.setUsername( userId );
            verifiedUsernameAuthToken.setRememberMe( true );
            final AuthenticationInfo authenticationInfo =
                runAs( () -> securityService.authenticate( verifiedUsernameAuthToken ), RoleKeys.AUTHENTICATED );
            if ( authenticationInfo.isAuthenticated() )
            {
                final HttpSession httpSession = request.getSession( true );
                httpSession.setAttribute( authenticationInfo.getClass().getName(), authenticationInfo );
            }
        }
    }


    private <T> T runAs( Callable<T> runnable, PrincipalKey principalKey )
    {
        final AuthenticationInfo authInfo = AuthenticationInfo.create().principals( principalKey ).user( User.ANONYMOUS ).build();
        return ContextBuilder.from( ContextAccessor.current() ).authInfo( authInfo ).build().callWith( runnable );
    }

    @Reference
    public void setConfigurationService( final Auth0ConfigurationService configurationService )
    {
        this.configurationService = configurationService;
    }

    @Reference
    public void setSecurityService( final SecurityService securityService )
    {
        this.securityService = securityService;
    }
}
