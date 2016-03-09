package com.enonic.app.auth0.impl;


import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.enonic.app.auth0.Auth0ConfigurationService;
import com.enonic.xp.web.filter.OncePerRequestFilter;

@Component(immediate = true, service = Filter.class,
    property = {"osgi.http.whiteboard.filter.pattern=/auth0", "service.ranking:Integer=49",
        "osgi.http.whiteboard.filter.dispatcher=FORWARD", "osgi.http.whiteboard.filter.dispatcher=REQUEST"})
public class Auth0CallbackFilter
    extends OncePerRequestFilter
{
    private static final Pattern TOKEN_PATTERN = Pattern.compile( "\"id_token\":\"([^\"]+)\"" );

    private Auth0TokenService tokenService;

    private Auth0ConfigurationService configurationService;

    @Override
    protected void doHandle( final HttpServletRequest req, final HttpServletResponse res, final FilterChain chain )
        throws Exception
    {
        //Gets the token
        String token = getToken( req );

        //If there is a token specified
        if ( token != null )
        {
            this.tokenService.handleToken( req, token );
        }

        //If there is a page callback
        final String callback = req.getParameter( "state" );
        if ( callback != null )
        {
            res.sendRedirect( callback );
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
            final String path = httpServletRequest.getParameter( "state" );

            //Retrieves the token
            final String tokenRequestResult = new HttpRequest().
                setUrl( "https://" + configurationService.getAppDomain( path ) + "/oauth/token" ).
                addParam( "client_id", configurationService.getAppClientId( path ) ).
                addParam( "redirect_uri", httpServletRequest.getRequestURL().toString() ).
                addParam( "client_secret", configurationService.getAppSecret( path ) ).
                addParam( "code", callbackCode ).
                addParam( "grant_type", "authorization_code" ).
                addParam( "scope", "openid nickname email" ).
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
        this.tokenService = auth0TokenService;
    }

    @Reference
    public void setAuth0ConfigurationService( final Auth0ConfigurationService configurationService )
    {
        this.configurationService = configurationService;
    }

}
