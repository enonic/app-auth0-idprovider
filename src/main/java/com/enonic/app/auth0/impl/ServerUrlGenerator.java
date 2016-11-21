package com.enonic.app.auth0.impl;

import com.enonic.xp.portal.PortalRequest;
import com.enonic.xp.script.bean.BeanContext;
import com.enonic.xp.script.bean.ScriptBean;
import com.enonic.xp.web.servlet.ServletRequestUrlHelper;

public class ServerUrlGenerator
    implements ScriptBean
{
    private PortalRequest request;

    public String execute()
    {
        return ServletRequestUrlHelper.getServerUrl( this.request.getRawRequest() );
    }

    @Override
    public void initialize( final BeanContext context )
    {
        this.request = context.getBinding( PortalRequest.class ).get();
    }
}
