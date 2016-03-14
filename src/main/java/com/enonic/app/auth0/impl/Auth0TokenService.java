package com.enonic.app.auth0.impl;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.JWTVerifyException;
import com.auth0.jwt.internal.org.apache.commons.codec.binary.Base64;

import com.enonic.app.auth0.Auth0ConfigurationService;
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

@Component(service = Auth0TokenService.class)
public class Auth0TokenService
{
    private static final String ID_KEY = "_id";

    private static final String NAME_KEY = "name";

    private static final String EMAIL_KEY = "email";

    private SecurityService securityService;

    private Auth0ConfigurationService configurationService;

    public void handleToken( final HttpServletRequest httpServletRequest, final String token )
        throws SignatureException, NoSuchAlgorithmException, JWTVerifyException, InvalidKeyException, IOException
    {
        //Creates verifier
        final String path = httpServletRequest.getParameter( "state" );
        final byte[] secretDecoded = new Base64( true ).decode( configurationService.getAppSecret( path ) );
        final JWTVerifier jwtVerifier = new JWTVerifier( secretDecoded, configurationService.getAppClientId( path ) );

        //Verifies the token and retrieve the email address
        Map<String, Object> decoded = jwtVerifier.verify( token );
        final Object id = decoded.get( ID_KEY );
        final Object name = decoded.get( NAME_KEY );
        final Object email = decoded.get( EMAIL_KEY );

        //Login the user
        login( httpServletRequest, (String) id, (String) name, (String) email );
    }

    private void login( final HttpServletRequest httpServletRequest, String login, String displayName, String email )
    {
        final String path = httpServletRequest.getParameter( "state" );
        final String userStoreId = configurationService.getUserStore( path );
        final UserStoreKey userStoreKey = UserStoreKey.from( userStoreId );
        final PrincipalKey principalKey = PrincipalKey.ofUser( userStoreKey, login );

        //Retrieves the user
        final Optional<User> user = runAs( () -> securityService.getUser( principalKey ), RoleKeys.AUTHENTICATED );

        //If the user does not exist
        if ( !user.isPresent() )
        {
            //Creates the user
            final PrincipalKeys defaultRoles = configurationService.getDefaultRoles( path );
            final CreateUserParams createUserParams = CreateUserParams.create().
                login( login ).
                displayName( displayName ).
                email( email ).
                userKey( principalKey ).
                build();
            runAs( () -> {
                securityService.createUser( createUserParams );
                for ( PrincipalKey defaultRole : defaultRoles )
                {
                    securityService.addRelationship( PrincipalRelationship.from( defaultRole ).to( principalKey ) );
                }
                return null;
            }, RoleKeys.ADMIN );

        }

        //Authenticates the user
        final VerifiedUsernameAuthToken verifiedUsernameAuthToken = new VerifiedUsernameAuthToken();
        verifiedUsernameAuthToken.setUserStore( userStoreKey );
        verifiedUsernameAuthToken.setUsername( login );
        verifiedUsernameAuthToken.setRememberMe( true );
        final AuthenticationInfo authenticationInfo =
            runAs( () -> securityService.authenticate( verifiedUsernameAuthToken ), RoleKeys.AUTHENTICATED );
        if ( authenticationInfo.isAuthenticated() )
        {
            final HttpSession httpSession = httpServletRequest.getSession( true );
            httpSession.setAttribute( authenticationInfo.getClass().getName(), authenticationInfo );
        }
    }

    private <T> T runAs( Callable<T> runnable, PrincipalKey principalKey )
    {
        final AuthenticationInfo authInfo = AuthenticationInfo.create().principals( principalKey ).user( User.ANONYMOUS ).build();
        return ContextBuilder.from( ContextAccessor.current() ).authInfo( authInfo ).build().callWith( runnable );
    }

    @Reference
    public void setSecurityService( final SecurityService securityService )
    {
        this.securityService = securityService;
    }

    @Reference
    public void setAuth0ConfigurationService( final Auth0ConfigurationService configurationService )
    {
        this.configurationService = configurationService;
    }
}
