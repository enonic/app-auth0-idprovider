package com.enonic.app.auth0.impl.user;

import java.time.Instant;
import java.util.Map;

import com.auth0.json.auth.UserInfo;

public class UserInfoAdapter
    implements Auth0User
{
    private final Map<String, Object> values;


    public UserInfoAdapter( final UserInfo userInfo )
    {
        this.values = userInfo.getValues();
    }

    @Override
    public String getUserId()
    {
        return (String) values.get( "sub" );
    }

    @Override
    public String getName()
    {
        return (String) values.get( "name" );
    }

    @Override
    public String getFamilyName()
    {
        return (String) values.get( "family_name" );
    }

    @Override
    public String getGivenName()
    {
        return (String) values.get( "given_name" );
    }

    @Override
    public String getMiddleName()
    {
        return (String) values.get( "middle_name" );
    }

    @Override
    public String getNickname()
    {
        return (String) values.get( "nickname" );
    }

    @Override
    public String getPicture()
    {
        return (String) values.get( "picture" );
    }

    @Override
    public Instant getUpdatedAt()
    {
        final String updatedAt = (String) values.get( "updated_at" );
        if ( updatedAt != null )
        {
            try
            {
                return Instant.parse( updatedAt );
            }
            catch ( Exception e )
            {

            }
        }
        return null;
    }

    @Override
    public String getEmail()
    {
        return (String) values.get( "email" );
    }

    @Override
    public Boolean isEmailVerified()
    {
        return (Boolean) values.get( "email_verified" );
    }
}
