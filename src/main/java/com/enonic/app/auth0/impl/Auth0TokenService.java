package com.enonic.app.auth0.impl;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.JWTVerifyException;
import com.auth0.jwt.internal.org.apache.commons.codec.binary.Base64;
import com.google.common.collect.ImmutableSet;

import com.enonic.app.auth0.Auth0ConfigurationService;
import com.enonic.xp.context.ContextAccessor;
import com.enonic.xp.context.ContextBuilder;
import com.enonic.xp.data.PropertyTree;
import com.enonic.xp.security.CreateUserParams;
import com.enonic.xp.security.PrincipalKey;
import com.enonic.xp.security.PrincipalKeys;
import com.enonic.xp.security.PrincipalRelationship;
import com.enonic.xp.security.RoleKeys;
import com.enonic.xp.security.SecurityService;
import com.enonic.xp.security.User;
import com.enonic.xp.security.UserStoreKey;
import com.enonic.xp.security.auth.AuthenticationInfo;
import com.enonic.xp.security.auth.VerifiedUsernameAuthToken;

@Component(service = Auth0TokenService.class)
public class Auth0TokenService
{
    private static final String ID_KEY = "_id";

    private static final String NAME_KEY = "name";

    private static final String EMAIL_KEY = "email";

    private static final ImmutableSet<String> TOKEN_RESERVED_KEYS = ImmutableSet.of( "iss", "sub", "aud", "exp", "iat" );

    private SecurityService securityService;

    private Auth0ConfigurationService configurationService;

    public void handleToken( final HttpServletRequest httpServletRequest, final String token )
        throws SignatureException, NoSuchAlgorithmException, JWTVerifyException, InvalidKeyException, IOException
    {
        //Creates verifier
        final String path = httpServletRequest.getParameter( "state" );
        final byte[] secretDecoded = new Base64( true ).decode( configurationService.getAppSecret( path ) );
        final JWTVerifier jwtVerifier = new JWTVerifier( secretDecoded, configurationService.getAppClientId( path ) );

        //Verifies the token and retrieve the email address
        Map<String, Object> decodedToken = jwtVerifier.verify( token );

        //Login the user
        login( httpServletRequest, decodedToken );
    }

    private void login( final HttpServletRequest httpServletRequest, final Map<String, Object> decodedToken )
    {
        final String login = (String) decodedToken.get( ID_KEY );
        final String displayName = (String) decodedToken.get( NAME_KEY );
        final String email = (String) decodedToken.get( EMAIL_KEY );

        final String path = httpServletRequest.getParameter( "state" );
        final String userStoreId = configurationService.getUserStore( path );
        final UserStoreKey userStoreKey = UserStoreKey.from( userStoreId );
        final PrincipalKey principalKey = PrincipalKey.ofUser( userStoreKey, login );

        //Retrieves the user
        User user = runAs( () -> securityService.getUser( principalKey ), RoleKeys.AUTHENTICATED ).orElse( null );

        //If the user does not exist
        if ( user == null )
        {
            //Creates the user
            final PrincipalKeys defaultRoles = configurationService.getDefaultRoles( path );
            final PropertyTree profile = createProfile( decodedToken );
            final CreateUserParams createUserParams = CreateUserParams.create().
                login( login ).
                displayName( displayName ).
                email( email ).
                userKey( principalKey ).
                profile( profile ).
                build();

            user = runAs( () -> {
                final User createdUser = securityService.createUser( createUserParams );
                for ( PrincipalKey defaultRole : defaultRoles )
                {
                    securityService.addRelationship( PrincipalRelationship.from( defaultRole ).to( principalKey ) );
                }
                return createdUser;
            }, RoleKeys.ADMIN );
        }

        if ( user != null )
        {
            //Authenticates the user
            final VerifiedUsernameAuthToken verifiedUsernameAuthToken = new VerifiedUsernameAuthToken();
            verifiedUsernameAuthToken.setUserStore( userStoreKey );
            verifiedUsernameAuthToken.setUsername( login );
            verifiedUsernameAuthToken.setRememberMe( true );
            final AuthenticationInfo authenticationInfo =
                runAs( () -> securityService.authenticate( verifiedUsernameAuthToken ), RoleKeys.AUTHENTICATED );
            if ( authenticationInfo.isAuthenticated() )
            {
                final HttpSession httpSession = httpServletRequest.getSession( true );
                httpSession.setAttribute( authenticationInfo.getClass().getName(), authenticationInfo );
            }
        }
    }

    private PropertyTree createProfile( final Map<String, Object> decodedToken )
    {
        final PropertyTree profile = new PropertyTree();
        decodedToken.entrySet().
            stream().
            filter( profileEntry -> !TOKEN_RESERVED_KEYS.contains( profileEntry.getKey() ) ).
            forEach( profileEntry -> {
                final Object profileEntryValue = profileEntry.getValue();
                final String profileEntryKey = profileEntry.getKey();
                if ( profileEntryValue instanceof Boolean )
                {
                    profile.addBoolean( profileEntryKey, (Boolean) profileEntryValue );
                }
                else if ( profileEntryValue instanceof Integer || profileEntryValue instanceof Long )
                {
                    profile.addLong( profileEntryKey, ( (Number) profileEntryValue ).longValue() );
                }
                else if ( profileEntryValue instanceof Float || profileEntryValue instanceof Double )
                {
                    profile.addDouble( profileEntryKey, ( (Number) profileEntryValue ).doubleValue() );
                }
                else if ( profileEntryValue instanceof String )
                {
                    profile.addString( profileEntryKey, profileEntryValue.toString() );
                }
            } );
        return profile;
    }

    private <T> T runAs( Callable<T> runnable, PrincipalKey principalKey )
    {
        final AuthenticationInfo authInfo = AuthenticationInfo.create().principals( principalKey ).user( User.ANONYMOUS ).build();
        return ContextBuilder.from( ContextAccessor.current() ).authInfo( authInfo ).build().callWith( runnable );
    }

    @Reference
    public void setSecurityService( final SecurityService securityService )
    {
        this.securityService = securityService;
    }

    @Reference
    public void setAuth0ConfigurationService( final Auth0ConfigurationService configurationService )
    {
        this.configurationService = configurationService;
    }
}
