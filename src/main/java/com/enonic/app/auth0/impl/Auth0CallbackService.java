package com.enonic.app.auth0.impl;


import javax.servlet.http.HttpServletRequest;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.auth0.AuthenticationController;
import com.auth0.Tokens;
import com.auth0.client.auth.AuthAPI;
import com.auth0.exception.Auth0Exception;
import com.auth0.json.auth.UserInfo;

import com.enonic.app.auth0.impl.user.UserInfoAdapter;
import com.enonic.app.auth0.impl.utils.QueryParamUtils;
import com.enonic.xp.security.IdProviderKey;

@Component(immediate = true, service = Auth0CallbackService.class)
public class Auth0CallbackService
{
    private final static Logger LOG = LoggerFactory.getLogger( Auth0CallbackService.class );

    private Auth0LoginService loginService;

    private Auth0ConfigurationService configurationService;

    public boolean handle( final HttpServletRequest request )
    {
        try
        {
            final IdProviderKey idProviderKey = getIdProviderKey( request );
            final AuthenticationController authController = createAuthController( idProviderKey );
            final Tokens tokens = authController.handle( request );
            final UserInfo userInfo = retrieveUserInfo( idProviderKey, tokens );
            loginService.login( request, new UserInfoAdapter( userInfo ), idProviderKey );
            return true;
        }
        catch ( Exception e )
        {
            LOG.error( "Error while handling auth0 callback", e );
        }
        return false;
    }

    private AuthenticationController createAuthController( IdProviderKey idProviderKey )
    {
        final String appClientId = configurationService.getAppClientId( idProviderKey );
        final String appSecret = configurationService.getAppSecret( idProviderKey );
        final String appDomain = configurationService.getAppDomain( idProviderKey );
        return AuthenticationController.newBuilder( appDomain, appClientId, appSecret ).build();
    }

    private UserInfo retrieveUserInfo( IdProviderKey idProviderKey, Tokens tokens )
        throws Auth0Exception
    {
        final String appClientId = configurationService.getAppClientId( idProviderKey );
        final String appSecret = configurationService.getAppSecret( idProviderKey );
        final String appDomain = configurationService.getAppDomain( idProviderKey );

        final UserInfo userInfo = new AuthAPI( appDomain, appClientId, appSecret ).
            userInfo( tokens.getAccessToken() ).
            execute();

        return userInfo;
    }


    private IdProviderKey getIdProviderKey( final HttpServletRequest httpServletRequest )
    {
        final String stateFromRequest = httpServletRequest.getParameter( "state" );
        final String idProviderKeyString = QueryParamUtils.parseFromQueryParams( stateFromRequest, "idprovider" );
        return IdProviderKey.from( idProviderKeyString );
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
