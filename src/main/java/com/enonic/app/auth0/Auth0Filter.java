package com.enonic.app.auth0;


import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.enonic.xp.web.filter.OncePerRequestFilter;

@Component(immediate = true, service = Filter.class,
    property = {"osgi.http.whiteboard.filter.pattern=/", "service.ranking:Integer=50", "osgi.http.whiteboard.filter.dispatcher=FORWARD",
        "osgi.http.whiteboard.filter.dispatcher=REQUEST"})
public class Auth0Filter
    extends OncePerRequestFilter
{
    static final String CALLBACK_URL = "http://localhost:8080/auth0";

    static final String AUTH0_DOMAIN = "https://auth0-test.eu.auth0.com";

    static final String CLIENT_ID = "XVe71J5BHZ0o8QPKQEYs7bx2Xa5FHkqL";

    private static final String BEARER_SCHEME = "Bearer";

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

        //Executes the rest of the filters with a wrapped response
        final ResponseWrapper responseWrapper = new ResponseWrapper( res, CALLBACK_URL, req.getRequestURL().toString() );
        chain.doFilter( req, responseWrapper );
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

        return null;
    }

    @Reference
    public void setAuth0TokenService( final Auth0TokenService auth0TokenService )
    {
        this.auth0TokenService = auth0TokenService;
    }
}
