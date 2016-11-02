package com.enonic.app.auth0.impl;


import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

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

@Component(immediate = true, service = Auth0CallbackService.class)
public class Auth0CallbackService
{
    private final static Logger LOG = LoggerFactory.getLogger( Auth0CallbackService.class );

    private Auth0LoginService loginService;

    private Auth0ConfigurationService configurationService;

    public boolean handle( final HttpServletRequest request )
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
                return true;
            }
            catch ( Exception e )
            {
                LOG.error( "Error while handling auth0 callback", e );
            }
        }
        else
        {
            LOG.error( "Error while handling auth0 callback", new IllegalStateException( "Invalid state or error" ) );
        }
        return false;
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

    protected boolean isValidRequest( final HttpServletRequest req )
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
