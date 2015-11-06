package com.enonic.app.auth0;


import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.enonic.xp.web.filter.OncePerRequestFilter;

@Component(immediate = true, service = Filter.class,
    property = {"osgi.http.whiteboard.filter.pattern=/auth0", "service.ranking:Integer=49",
        "osgi.http.whiteboard.filter.dispatcher=FORWARD", "osgi.http.whiteboard.filter.dispatcher=REQUEST"})
public class Auth0CallbackFilter
    extends OncePerRequestFilter
{
    static final String AUTH0_DOMAIN = "https://auth0-test.eu.auth0.com";

    private static final String SECRET = "Wr5vaQm2rg2HNUGLwW4hal3qaRK-Ud5Papotz61Fji5Df-NqbbCl8SFRfm-qbg-M";

    static final String CLIENT_ID = "XVe71J5BHZ0o8QPKQEYs7bx2Xa5FHkqL";

    private static final Pattern TOKEN_PATTERN = Pattern.compile( "\"id_token\":\"([^\"]+)\"" );

    private Auth0TokenService auth0TokenService;

    @Override
    protected void doHandle( final HttpServletRequest req, final HttpServletResponse res, final FilterChain chain )
        throws Exception
    {
        //Gets the token
        String token = getToken( req );

        //If there is a token specified
        if ( token != null )
        {
            this.auth0TokenService.handleToken( req, token );
        }

        //If there is a page callback
        final String callback = req.getParameter( "_callback" );
        if ( callback != null )
        {
            res.setStatus( 303 );
            res.setHeader( "Location", callback );
            return;
        }

        //Otherwise executes the rest of the filters
        chain.doFilter( req, res );
    }

    private String getToken( final HttpServletRequest httpServletRequest )
        throws IOException
    {
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

    @Reference
    public void setAuth0TokenService( final Auth0TokenService auth0TokenService )
    {
        this.auth0TokenService = auth0TokenService;
    }

}
