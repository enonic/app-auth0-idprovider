package com.enonic.app.auth0.impl;


import java.io.IOException;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.auth0.Auth0Client;
import com.auth0.Auth0ClientImpl;
import com.auth0.Auth0User;
import com.auth0.NonceUtils;
import com.auth0.QueryParamUtils;
import com.auth0.Tokens;

import com.enonic.xp.security.UserStoreKey;

@Component(immediate = true, service = Servlet.class, property = {"osgi.http.whiteboard.servlet.pattern=/auth0"})
public class Auth0CallbackServlet
    extends HttpServlet
{
    private final static Logger LOG = LoggerFactory.getLogger( Auth0CallbackServlet.class );

    private Auth0LoginService loginService;

    private Auth0ConfigurationService configurationService;

    @Override
    protected void doGet( final HttpServletRequest request, final HttpServletResponse response )
        throws ServletException, IOException
    {
        if ( isValidRequest( request ) )
        {
            try
            {
                final UserStoreKey userStoreKey = getUserStoreKey( request );
                final Auth0Client auth0Client = createAuth0Client( userStoreKey );
                final Tokens tokens = fetchTokens( request, auth0Client );
                final Auth0User auth0User = auth0Client.getUserProfile( tokens );
                loginService.login( request, auth0User, userStoreKey );
                NonceUtils.removeNonceFromStorage( request );
                onSuccess( request, response );
            }
            catch ( RuntimeException ex )
            {
                onFailure( request, response, ex );
            }
        }
        else
        {
            onFailure( request, response, new IllegalStateException( "Invalid state or error" ) );
        }
    }

    private Auth0Client createAuth0Client( UserStoreKey userStoreKey )
    {
        final String appClientId = configurationService.getAppClientId( userStoreKey );
        final String appSecret = configurationService.getAppSecret( userStoreKey );
        final String appDomain = configurationService.getAppDomain( userStoreKey );
        return new Auth0ClientImpl( appClientId, appSecret, appDomain );
    }


    protected Tokens fetchTokens( final HttpServletRequest req, final Auth0Client auth0Client )
        throws IOException
    {
        final String authorizationCode = req.getParameter( "code" );
        final String redirectUri = req.getRequestURL().toString();
        return auth0Client.getTokens( authorizationCode, redirectUri );
    }

    protected void onSuccess( final HttpServletRequest req, final HttpServletResponse res )
        throws ServletException, IOException
    {

        res.sendRedirect( getRedirectUrl( req ) );
    }

    protected void onFailure( final HttpServletRequest req, final HttpServletResponse res, Exception e )
        throws ServletException, IOException
    {
        LOG.error( "Error while handling auth0 callback", e );
        res.sendRedirect( getRedirectUrl( req ) );
    }

    protected boolean isValidRequest( final HttpServletRequest req )
        throws IOException
    {
        return !hasError( req ) && isValidState( req );
    }

    protected boolean hasError( final HttpServletRequest req )
    {
        return req.getParameter( "error" ) != null;
    }

    protected boolean isValidState( final HttpServletRequest req )
    {
        final String stateFromRequest = req.getParameter( "state" );
        return NonceUtils.matchesNonceInStorage( req, stateFromRequest );
    }

    private UserStoreKey getUserStoreKey( final HttpServletRequest httpServletRequest )
    {
        final String stateFromRequest = httpServletRequest.getParameter( "state" );
        final String userStoreKeyString = QueryParamUtils.parseFromQueryParams( stateFromRequest, "userstore" );
        return UserStoreKey.from( userStoreKeyString );
    }

    public String getRedirectUrl( final HttpServletRequest httpServletRequest )
    {
        final String stateFromRequest = httpServletRequest.getParameter( "state" );
        return QueryParamUtils.parseFromQueryParams( stateFromRequest, "redirect" );
    }

    @Reference
    public void setLoginService( final Auth0LoginService loginService )
    {
        this.loginService = loginService;
    }

    @Reference
    public void setAuth0ConfigurationService( final Auth0ConfigurationService configurationService )
    {
        this.configurationService = configurationService;
    }

}
