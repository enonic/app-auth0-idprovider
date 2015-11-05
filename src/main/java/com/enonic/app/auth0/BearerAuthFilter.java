package com.enonic.app.auth0;


import java.io.IOException;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.internal.org.apache.commons.codec.binary.Base64;

import com.enonic.xp.context.ContextAccessor;
import com.enonic.xp.context.ContextBuilder;
import com.enonic.xp.security.RoleKeys;
import com.enonic.xp.security.SecurityService;
import com.enonic.xp.security.User;
import com.enonic.xp.security.UserStoreKey;
import com.enonic.xp.security.auth.AuthenticationInfo;
import com.enonic.xp.security.auth.VerifiedEmailAuthToken;
import com.enonic.xp.web.filter.OncePerRequestFilter;

@Component(immediate = true, service = Filter.class,
    property = {"osgi.http.whiteboard.filter.pattern=/", "service.ranking:Integer=50", "osgi.http.whiteboard.filter.dispatcher=FORWARD",
        "osgi.http.whiteboard.filter.dispatcher=REQUEST"})
public class BearerAuthFilter
    extends OncePerRequestFilter
{
    static final String CALLBACK_URL = "http://localhost:8080/auth0/forbidden";

    static final String AUTH0_DOMAIN = "https://auth0-test.eu.auth0.com";

    private static final String SECRET = "Wr5vaQm2rg2HNUGLwW4hal3qaRK-Ud5Papotz61Fji5Df-NqbbCl8SFRfm-qbg-M";

    static final String CLIENT_ID = "XVe71J5BHZ0o8QPKQEYs7bx2Xa5FHkqL";

    private static final String BEARER_SCHEME = "Bearer";

    private static final String EMAIL_KEY = "email";

    private static final Pattern AUTHORIZATION_HEADER_PATTERN = Pattern.compile( "^" + BEARER_SCHEME + " (.*)$" );

    private static final Pattern TOKEN_PATTERN = Pattern.compile( "\"id_token\":\"([^\"]+)\"" );

    private SecurityService securityService;

    private JWTVerifier jwtVerifier;

    @Activate
    public void activate()
    {
        final byte[] secretDecoded = new Base64( true ).decode( SECRET );
        jwtVerifier = new JWTVerifier( secretDecoded, CLIENT_ID );
    }

    @Override
    protected void doHandle( final HttpServletRequest req, final HttpServletResponse res, final FilterChain chain )
        throws Exception
    {
        //Gets the token
        String token = getToken( req );

        //If there is a token specified
        if ( token != null )
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

        //Executes the rest of the filters with a wrapped response
        final ResponseWrapper responseWrapper = new ResponseWrapper( res, CALLBACK_URL, req.getRequestURL().toString() );
        chain.doFilter( req, responseWrapper );
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

    private String getToken( final HttpServletRequest httpServletRequest )
        throws IOException
    {
        //If header "authorization: Bearer <jwt>"
        final String authorizationHeader = httpServletRequest.getHeader( "authorization" );
        if ( authorizationHeader != null )
        {
            String[] parts = authorizationHeader.split( " " );
            if ( parts.length == 2 )
            {
                String scheme = parts[0];
                String credentials = parts[1];

                if ( BEARER_SCHEME.equalsIgnoreCase( scheme ) )
                {
                    return credentials;
                }
            }
        }

        //If parameter "code=<callback-code>"
        final String callbackCode = httpServletRequest.getParameter( "code" );
        if ( callbackCode != null )
        {
            //Retrieves the token
            final String tokenRequestResult = new HttpRequest().
                setUrl( AUTH0_DOMAIN + "/oauth/token" ).
                addParam( "client_id", CLIENT_ID ).
                addParam( "redirect_uri", httpServletRequest.getRequestURL().toString() ).
                addParam( "client_secret", SECRET ).
                addParam( "code", callbackCode ).
                addParam( "grant_type", "authorization_code" ).
                addParam( "scope", "openid email" ).
                execute();

            final Matcher tokenMatcher = TOKEN_PATTERN.matcher( tokenRequestResult );

            if ( tokenMatcher.find() )
            {
                return tokenMatcher.group( 1 );
            }
        }

        return null;
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
