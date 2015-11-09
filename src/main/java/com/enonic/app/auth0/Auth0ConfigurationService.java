package com.enonic.app.auth0;

import java.util.Map;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

import com.google.common.base.Preconditions;

import com.enonic.xp.config.ConfigBuilder;
import com.enonic.xp.config.Configuration;

@Component(service = Auth0ConfigurationService.class, configurationPid = "com.enonic.app.auth0")
public class Auth0ConfigurationService
{
    private Configuration configuration;

    @Activate
    public void activate( Map<String, String> configuration )
    {
        this.configuration = ConfigBuilder.create().
            load( getClass(), "default.properties" ).
            addAll( configuration ).
            build();

        Preconditions.checkNotNull( getCallbackUrl(), "auth0.callbackUrl property is mandatory" );
        Preconditions.checkNotNull( getAppDomain(), "auth0.app.domainUrl property is mandatory" );
        Preconditions.checkNotNull( getAppClientId(), "auth0.app.clientId property is mandatory" );
        Preconditions.checkNotNull( getAppSecret(), "auth0.app.secret property is mandatory" );
    }

    public String getCallbackUrl()
    {
        return configuration.get( "auth0.callbackUrl" );
    }

    public String getAppDomain()
    {
        return configuration.get( "auth0.app.domainUrl" );
    }

    public String getAppClientId()
    {
        return configuration.get( "auth0.app.clientId" );
    }

    public String getAppSecret()
    {
        return configuration.get( "auth0.app.secret" );
    }
}
