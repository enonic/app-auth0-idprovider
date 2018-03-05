package com.enonic.app.auth0.impl;

import java.util.Map;
import java.util.concurrent.Callable;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.enonic.app.auth0.impl.user.Auth0User;
import com.enonic.xp.context.ContextAccessor;
import com.enonic.xp.context.ContextBuilder;
import com.enonic.xp.data.PropertySet;
import com.enonic.xp.data.PropertyTree;
import com.enonic.xp.lib.content.mapper.JsonToPropertyTreeTranslator;
import com.enonic.xp.query.expr.ConstraintExpr;
import com.enonic.xp.query.expr.QueryExpr;
import com.enonic.xp.query.parser.QueryParser;
import com.enonic.xp.security.CreateUserParams;
import com.enonic.xp.security.PrincipalKey;
import com.enonic.xp.security.PrincipalKeys;
import com.enonic.xp.security.PrincipalRelationship;
import com.enonic.xp.security.RoleKeys;
import com.enonic.xp.security.SecurityService;
import com.enonic.xp.security.UpdateUserParams;
import com.enonic.xp.security.User;
import com.enonic.xp.security.UserQuery;
import com.enonic.xp.security.UserStoreKey;
import com.enonic.xp.security.auth.AuthenticationInfo;
import com.enonic.xp.security.auth.VerifiedUsernameAuthToken;

@Component(service = Auth0LoginService.class)
public class Auth0LoginService
{
    private Auth0ConfigurationService configurationService;

    private SecurityService securityService;

    public void login( final HttpServletRequest request, final Auth0User auth0User, final UserStoreKey userStoreKey )
    {
        //Retrieves the user by key
        final String userId = auth0User.getUserId().replace( '|', '-' );
        final PrincipalKey principalKey = PrincipalKey.ofUser( userStoreKey, userId );
        User user = runAs( () -> securityService.getUser( principalKey ), RoleKeys.AUTHENTICATED ).orElse( null );

        //If the user does not exist with this id
        if ( user == null && auth0User.getEmail() != null )
        {
            //Retrieves the user by email
            final ConstraintExpr constraintExpr =
                QueryParser.parseCostraintExpression( "userstorekey = '" + userStoreKey + "' AND email = '" + auth0User.getEmail() + "'" );
            final QueryExpr queryExpr = QueryExpr.from( constraintExpr );
            final UserQuery userQuery = UserQuery.create().
                size( 1 ).
                queryExpr( queryExpr ).
                build();
            user = (User) runAs( () -> securityService.query( userQuery ), RoleKeys.AUTHENTICATED ).
                getUsers().first();
        }

        //If the user does not exist
        if ( user == null )
        {
            //Creates the user
            user = createUser( auth0User, principalKey );
        }

        //Updates the profile
        updateProfile( auth0User, user.getKey() );

        //Authenticates the user
        authenticate( request, user.getKey() );

    }

    private User createUser( final Auth0User auth0User, final PrincipalKey principalKey )
    {
        final String email = auth0User.getEmail();
        final String name = auth0User.getName();
        final PrincipalKeys defaultPrincipals = configurationService.getDefaultPrincipals( principalKey.getUserStore() );
        final CreateUserParams createUserParams = CreateUserParams.create().
            login( principalKey.getId() ).
            displayName( name ).
            email( email ).
            userKey( principalKey ).
            build();

        return runAs( () -> {
            final User user = securityService.createUser( createUserParams );
            for ( PrincipalKey defaultPrincipal : defaultPrincipals )
            {
                securityService.addRelationship( PrincipalRelationship.from( defaultPrincipal ).to( principalKey ) );
            }
            return user;
        }, RoleKeys.ADMIN );
    }

    private void updateProfile( final Auth0User auth0User, final PrincipalKey principalKey )
    {
        final UpdateUserParams updateUserParams = UpdateUserParams.create().
            userKey( principalKey ).
            editor( editableUser -> this.updateProfile( editableUser.profile, auth0User ) ).
            build();
        runAs( () -> securityService.updateUser( updateUserParams ), RoleKeys.ADMIN );
    }

    private void authenticate( final HttpServletRequest request, final PrincipalKey principalKey )
    {
        final VerifiedUsernameAuthToken verifiedUsernameAuthToken = new VerifiedUsernameAuthToken();
        verifiedUsernameAuthToken.setUserStore( principalKey.getUserStore() );
        verifiedUsernameAuthToken.setUsername( principalKey.getId() );
        verifiedUsernameAuthToken.setRememberMe( true );
        final AuthenticationInfo authenticationInfo =
            runAs( () -> securityService.authenticate( verifiedUsernameAuthToken ), RoleKeys.AUTHENTICATED );
        if ( authenticationInfo.isAuthenticated() )
        {
            final HttpSession httpSession = request.getSession( true );
            httpSession.setAttribute( authenticationInfo.getClass().getName(), authenticationInfo );
        }
    }


    private void updateProfile( final PropertyTree profile, final Auth0User auth0User )
    {
        //Retrieves the existing auth0 identity in the profile
        PropertySet currentAuth0Identity = null;
        final Iterable<PropertySet> identities = profile.getSets( "auth0Identities" );
        for ( PropertySet identity : identities )
        {
            if ( auth0User.getUserId().equals( identity.getString( "userId" ) ) )
            {
                currentAuth0Identity = identity;
                break;
            }
        }

        //If there is no existing auth0 identity in the profile
        if ( currentAuth0Identity == null )
        {
            //Creates the auth0 identity
            currentAuth0Identity = profile.addSet( "auth0Identities" );
        }

        //Update the auth0 identity
        currentAuth0Identity.setString( "userId", auth0User.getUserId() );
        currentAuth0Identity.setString( "name", auth0User.getName() );
        currentAuth0Identity.setString( "nickname", auth0User.getNickname() );
        currentAuth0Identity.setString( "picture", auth0User.getPicture() );
        currentAuth0Identity.setString( "email", auth0User.getEmail() );
        currentAuth0Identity.setBoolean( "emailVerified", auth0User.isEmailVerified() );

        //TODO
//        currentAuth0Identity.setString( "givenName", auth0User.getGivenName() );
//        currentAuth0Identity.setString( "familyName", auth0User.getFamilyName() );
//        currentAuth0Identity.setSet( "userMetaData", createPropertySet( auth0User.getUserMetadata() ) );
//        currentAuth0Identity.setSet( "appMetaData", createPropertySet( auth0User.getAppMetadata() ) );
//        currentAuth0Identity.setInstant( "createdAt", auth0User.getCreatedAt().toInstant() );
//
//        currentAuth0Identity.removeProperty( "identities" );
//        for ( UserIdentity userIdentity : auth0User.getIdentities() )
//        {
//            currentAuth0Identity.setSet( "identities", createPropertySet( userIdentity ) );
//        }
//
//        currentAuth0Identity.setSet( "extraInfo", createPropertySet( auth0User.getExtraInfo() ) );
//
//        currentAuth0Identity.removeProperty( "roles" );
//        for ( String role : auth0User.getRoles() )
//        {
//            currentAuth0Identity.addString( "roles", role );
//        }
//
//        currentAuth0Identity.removeProperty( "groups" );
//        for ( String group : auth0User.getGroups() )
//        {
//            currentAuth0Identity.addString( "groups", group );
//        }
    }

//    private PropertySet createPropertySet( final UserIdentity userIdentity )
//    {
//        final PropertySet propertySet = new PropertySet();
//        propertySet.setString( "id", userIdentity.getId() );
//        propertySet.setString( "connection", userIdentity.getConnection() );
//        propertySet.setString( "provider", userIdentity.getProvider() );
//        propertySet.setBoolean( "social", userIdentity.isSocial() );
//        propertySet.setSet( "profileInfo", createPropertySet( userIdentity.getProfileInfo() ) );
//        return propertySet;
//    }

    private PropertySet createPropertySet( final Map<String, Object> value )
    {
        if ( value == null )
        {
            return null;
        }

        //TODO Is there no more optimized method for Map to PropertySet?
        final JsonNode jsonNode = createJsonNode( value );
        return new JsonToPropertyTreeTranslator( null, false ).
            translate( jsonNode ).
            getRoot();
    }

    private JsonNode createJsonNode( final Map<String, Object> value )
    {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.valueToTree( value );
    }


    private <T> T runAs( Callable<T> runnable, PrincipalKey principalKey )
    {
        final AuthenticationInfo authInfo = AuthenticationInfo.create().principals( principalKey ).user( User.ANONYMOUS ).build();
        return ContextBuilder.from( ContextAccessor.current() ).authInfo( authInfo ).build().callWith( runnable );
    }

    @Reference
    public void setConfigurationService( final Auth0ConfigurationService configurationService )
    {
        this.configurationService = configurationService;
    }

    @Reference
    public void setSecurityService( final SecurityService securityService )
    {
        this.securityService = securityService;
    }
}
