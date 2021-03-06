package com.enonic.app.auth0.impl;

import java.util.function.Supplier;

import javax.servlet.http.HttpServletRequest;

import com.enonic.app.auth0.impl.utils.NonceUtils;
import com.enonic.app.auth0.impl.utils.QueryParamUtils;
import com.enonic.app.auth0.impl.utils.SessionUtils;
import com.enonic.xp.portal.PortalRequest;
import com.enonic.xp.script.bean.BeanContext;
import com.enonic.xp.script.bean.ScriptBean;

public class StateScriptBean
    implements ScriptBean
{
    private Supplier<PortalRequest> portalRequestSupplier;

    public String addOrReplaceToState( final String key, final String value )
    {
        final HttpServletRequest request = portalRequestSupplier.get().getRawRequest();
        final String stateFromStorage = SessionUtils.getState( request ) != null ? SessionUtils.getState( request ) : "";
        final String updatedState = QueryParamUtils.addOrReplaceInQueryParams( stateFromStorage, key, value );
        SessionUtils.setState( request, updatedState );
        return updatedState;
    }

    public void addNonceToState()
    {
        final HttpServletRequest request = portalRequestSupplier.get().getRawRequest();
        NonceUtils.addNonceToStorage( request );
    }

    public String getFromState( final String key )
    {
        final HttpServletRequest request = portalRequestSupplier.get().getRawRequest();
        final String stateFromSession = SessionUtils.getState( request ) != null ? SessionUtils.getState( request ) : "";
        return QueryParamUtils.parseFromQueryParams( stateFromSession, key );
    }

    @Override
    public void initialize( final BeanContext context )
    {
        this.portalRequestSupplier = context.getBinding( PortalRequest.class );
    }


}
