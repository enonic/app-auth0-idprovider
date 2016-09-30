package com.enonic.app.auth0.impl;


import java.io.IOException;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.auth0.Auth0Client;
import com.auth0.Auth0ClientImpl;
import com.auth0.Auth0User;
import com.auth0.NonceUtils;
import com.auth0.QueryParamUtils;
import com.auth0.SessionUtils;
import com.auth0.Tokens;

import com.enonic.app.auth0.Auth0ConfigurationService;
import com.enonic.xp.security.UserStoreKey;

@Component(immediate = true, service = Servlet.class, property = {"osgi.http.whiteboard.servlet.pattern=/auth0"})
public class Auth0CallbackServlet
    extends HttpServlet
{
    private Auth0TokenService tokenService;

    private Auth0ConfigurationService configurationService;

    @Override
    protected void doGet( final HttpServletRequest req, final HttpServletResponse res )
        throws ServletException, IOException
    {
        if ( isValidRequest( req ) )
        {
            try
            {
                final Auth0Client auth0Client = createAuth0Client( req );
                final Tokens tokens = fetchTokens( req, auth0Client );
                final Auth0User auth0User = auth0Client.getUserProfile( tokens );
                store( tokens, auth0User, req ); //Replace by create/update user + login
                NonceUtils.removeNonceFromStorage( req );
                onSuccess( req, res );
            }
            catch ( RuntimeException ex )
            {
                onFailure( req, res, ex );
            }
        }
        else
        {
            onFailure( req, res, new IllegalStateException( "Invalid state or error" ) );
        }
    }

    private Auth0Client createAuth0Client( HttpServletRequest httpServletRequest )
    {
        final UserStoreKey userStoreKey = getUserStoreKey( httpServletRequest );
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
        res.sendRedirect( req.getContextPath() + getRedirectUrl( req ) );
    }

    protected void onFailure( final HttpServletRequest req, final HttpServletResponse res, Exception ex )
        throws ServletException, IOException
    {
        ex.printStackTrace();
        final String redirectOnFailLocation = req.getContextPath() + getRedirectUrl( req );
        res.sendRedirect( redirectOnFailLocation );
    }

    //TODO Remove
    protected void store( final Tokens tokens, final Auth0User user, final HttpServletRequest req )
    {
        SessionUtils.setTokens( req, tokens );
        SessionUtils.setAuth0User( req, user );
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
