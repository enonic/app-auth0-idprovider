package com.enonic.app.auth0.impl;

import java.util.function.Supplier;

import javax.servlet.http.HttpServletRequest;

import com.auth0.NonceUtils;
import com.auth0.QueryParamUtils;
import com.auth0.SessionUtils;

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

    public String getFromState( final String key )
    {
        final HttpServletRequest httpServletRequest = portalRequestSupplier.get().getRawRequest();
        final String stateFromRequest = httpServletRequest.getParameter( "state" );
        return QueryParamUtils.parseFromQueryParams( stateFromRequest, key );
    }

    public void addNonceToState()
    {
        final HttpServletRequest request = portalRequestSupplier.get().getRawRequest();
        NonceUtils.addNonceToStorage( request );
    }

    @Override
    public void initialize( final BeanContext context )
    {
        this.portalRequestSupplier = context.getBinding( PortalRequest.class );
    }


}
