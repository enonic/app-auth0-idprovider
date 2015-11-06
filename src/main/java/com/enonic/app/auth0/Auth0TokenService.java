package com.enonic.app.auth0;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.util.Map;
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
import com.enonic.xp.security.RoleKeys;
import com.enonic.xp.security.SecurityService;
import com.enonic.xp.security.User;
import com.enonic.xp.security.UserStoreKey;
import com.enonic.xp.security.auth.AuthenticationInfo;
import com.enonic.xp.security.auth.VerifiedEmailAuthToken;

@Component(service = Auth0TokenService.class)
public class Auth0TokenService
{
    private static final String SECRET = "Wr5vaQm2rg2HNUGLwW4hal3qaRK-Ud5Papotz61Fji5Df-NqbbCl8SFRfm-qbg-M";

    static final String CLIENT_ID = "XVe71J5BHZ0o8QPKQEYs7bx2Xa5FHkqL";

    private static final String EMAIL_KEY = "email";

    private SecurityService securityService;

    private JWTVerifier jwtVerifier;

    @Activate
    public void activate()
    {
        final byte[] secretDecoded = new Base64( true ).decode( SECRET );
        jwtVerifier = new JWTVerifier( secretDecoded, CLIENT_ID );
    }

    public void handleToken( final HttpServletRequest req, final String token )
        throws SignatureException, NoSuchAlgorithmException, JWTVerifyException, InvalidKeyException, IOException
    {
        //Verifies the token and retrieve the email address
        Map<String, Object> decoded = jwtVerifier.verify( token );
        final Object email = decoded.get( EMAIL_KEY );

        //Login the user linked to this email address
        if ( email instanceof String )
        {
            login( req, (String) email );
        }
    }

    private void login( final HttpServletRequest req, String email )
    {
        final VerifiedEmailAuthToken verifiedUsernameAuthToken = new VerifiedEmailAuthToken();
        verifiedUsernameAuthToken.setUserStore( UserStoreKey.from( "system" ) );
        verifiedUsernameAuthToken.setEmail( email );
        verifiedUsernameAuthToken.setRememberMe( false );

        final AuthenticationInfo authenticationInfo = runAsAuthenticated( () -> securityService.authenticate( verifiedUsernameAuthToken ) );

        if ( authenticationInfo.isAuthenticated() )
        {
            final HttpSession httpSession = req.getSession( true );

            httpSession.setAttribute( authenticationInfo.getClass().getName(), authenticationInfo );
            System.out.println( "Authenticated" );
        }
    }

    private <T> T runAsAuthenticated( Callable<T> runnable )
    {
        final AuthenticationInfo authInfo = AuthenticationInfo.create().principals( RoleKeys.AUTHENTICATED ).user( User.ANONYMOUS ).build();
        return ContextBuilder.from( ContextAccessor.current() ).authInfo( authInfo ).build().callWith( runnable );
    }

    @Reference
    public void setSecurityService( final SecurityService securityService )
    {
        this.securityService = securityService;
    }
}
