package com.enonic.app.auth0.impl;

import java.util.function.Supplier;

import javax.servlet.http.HttpServletRequest;

import com.enonic.xp.portal.PortalRequest;
import com.enonic.xp.script.bean.BeanContext;
import com.enonic.xp.script.bean.ScriptBean;

public class CallbackScriptBean
    implements ScriptBean
{
    private Supplier<PortalRequest> portalRequestSupplier;

    private Supplier<Auth0CallbackService> auth0CallbackServiceSupplier;

    public boolean handle()
    {
        final HttpServletRequest servletRequest = portalRequestSupplier.get().getRawRequest();
        return auth0CallbackServiceSupplier.get().handle( servletRequest );
    }

    @Override
    public void initialize( final BeanContext context )
    {
        this.portalRequestSupplier = context.getBinding( PortalRequest.class );
        this.auth0CallbackServiceSupplier = context.getService( Auth0CallbackService.class );
    }


}
