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
import com.enonic.xp.security.UserStoreKey;

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
            final UserStoreKey userStoreKey = getUserStoreKey( request );
            final AuthenticationController authController = createAuthController( userStoreKey );
            final Tokens tokens = authController.handle( request );
            final UserInfo userInfo = retrieveUserInfo( userStoreKey, tokens );
            loginService.login( request, new UserInfoAdapter( userInfo ), userStoreKey );
            return true;
        }
        catch ( Exception e )
        {
            LOG.error( "Error while handling auth0 callback", e );
        }
        return false;
    }

    private AuthenticationController createAuthController( UserStoreKey userStoreKey )
    {
        final String appClientId = configurationService.getAppClientId( userStoreKey );
        final String appSecret = configurationService.getAppSecret( userStoreKey );
        final String appDomain = configurationService.getAppDomain( userStoreKey );
        return AuthenticationController.newBuilder( appDomain, appClientId, appSecret ).build();
    }

    private UserInfo retrieveUserInfo( UserStoreKey userStoreKey, Tokens tokens )
        throws Auth0Exception
    {
        final String appClientId = configurationService.getAppClientId( userStoreKey );
        final String appSecret = configurationService.getAppSecret( userStoreKey );
        final String appDomain = configurationService.getAppDomain( userStoreKey );

        final UserInfo userInfo = new AuthAPI( appDomain, appClientId, appSecret ).
            userInfo( tokens.getAccessToken() ).
            execute();

        return userInfo;
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
