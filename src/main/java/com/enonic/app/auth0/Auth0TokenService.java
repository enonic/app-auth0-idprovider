package com.enonic.app.auth0;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.JWTVerifyException;
import com.auth0.jwt.internal.org.apache.commons.codec.binary.Base64;

import com.enonic.xp.context.ContextAccessor;
import com.enonic.xp.context.ContextBuilder;
import com.enonic.xp.security.CreateUserParams;
import com.enonic.xp.security.PrincipalKey;
import com.enonic.xp.security.PrincipalRelationship;
import com.enonic.xp.security.RoleKeys;
import com.enonic.xp.security.SecurityService;
import com.enonic.xp.security.User;
import com.enonic.xp.security.UserStoreKey;
import com.enonic.xp.security.auth.AuthenticationInfo;
import com.enonic.xp.security.auth.VerifiedEmailAuthToken;

@Component(service = Auth0TokenService.class)
public class Auth0TokenService
{
    private static final String NICKNAME_KEY = "nickname";

    private static final String EMAIL_KEY = "email";

    private SecurityService securityService;

    private Auth0ConfigurationService configurationService;

    private JWTVerifier jwtVerifier;

    @Activate
    public void activate()
    {
        final byte[] secretDecoded = new Base64( true ).decode( configurationService.getAppSecret() );
        jwtVerifier = new JWTVerifier( secretDecoded, configurationService.getAppClientId() );
    }

    public void handleToken( final HttpServletRequest req, final String token )
        throws SignatureException, NoSuchAlgorithmException, JWTVerifyException, InvalidKeyException, IOException
    {
        //Verifies the token and retrieve the email address
        Map<String, Object> decoded = jwtVerifier.verify( token );
        final Object nickname = decoded.get( NICKNAME_KEY );
        final Object email = decoded.get( EMAIL_KEY );

        //Login the user linked to this email address
        if ( nickname instanceof String && email instanceof String )
        {
            login( req, (String) nickname, (String) email );
        }
    }

    private void login( final HttpServletRequest req, String userId, String email )
    {
        final String userStoreId = configurationService.getUserStore();
        final UserStoreKey userStoreKey = UserStoreKey.from( userStoreId );
        final PrincipalKey principalKey = PrincipalKey.ofUser( userStoreKey, userId );

        //Retrieves the user
        final Optional<User> user = runAs( () -> securityService.getUser( principalKey ), RoleKeys.AUTHENTICATED );

        //If the user does not exist
        if ( !user.isPresent() )
        {
            //Creates the user
            final PrincipalKey defaultRole = PrincipalKey.ofRole( configurationService.getDefaultRole() );
            final CreateUserParams createUserParams = CreateUserParams.create().
                login( userId ).
                displayName( userId ).
                email( email ).
                userKey( principalKey ).
                build();
            runAs( () -> {
                securityService.createUser( createUserParams );
                securityService.addRelationship( PrincipalRelationship.from( defaultRole ).to( principalKey ) );
                return null;
            }, RoleKeys.ADMIN );

        }

        //Authenticates the user
        final VerifiedEmailAuthToken verifiedUsernameAuthToken = new VerifiedEmailAuthToken();
        verifiedUsernameAuthToken.setUserStore( userStoreKey );
        verifiedUsernameAuthToken.setEmail( email );
        verifiedUsernameAuthToken.setRememberMe( false );
        final AuthenticationInfo authenticationInfo =
            runAs( () -> securityService.authenticate( verifiedUsernameAuthToken ), RoleKeys.AUTHENTICATED );
        if ( authenticationInfo.isAuthenticated() )
        {
            final HttpSession httpSession = req.getSession( true );

            httpSession.setAttribute( authenticationInfo.getClass().getName(), authenticationInfo );
            System.out.println( "Authenticated" );
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
        System.out.println( "test1" );
    }

    @Reference
    public void setAuth0ConfigurationService( final Auth0ConfigurationService configurationService )
    {
        this.configurationService = configurationService;
        System.out.println( "test2" );
    }
}
