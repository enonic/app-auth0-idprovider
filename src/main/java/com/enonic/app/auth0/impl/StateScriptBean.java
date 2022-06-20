package com.enonic.app.auth0.impl;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Objects;
import java.util.function.Supplier;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import com.enonic.app.auth0.impl.utils.QueryParamUtils;
import com.enonic.xp.portal.PortalRequest;
import com.enonic.xp.script.bean.BeanContext;
import com.enonic.xp.script.bean.ScriptBean;

public class StateScriptBean
    implements ScriptBean
{
    private static final String NONCE_KEY = "nonce";

    public static final String STATE = "com.auth0.state";

    private Supplier<PortalRequest> portalRequestSupplier;

    public String addOrReplaceToState( final String key, final String value )
    {
        final HttpSession session = getSession();
        final String stateFromStorage = getStateFromStorage( session );
        final String updatedState = QueryParamUtils.addOrReplaceInQueryParams( stateFromStorage, key, value );
        session.setAttribute( STATE, updatedState );
        return updatedState;
    }

    public void addNonceToState()
    {
        // only add if no existing entry..
        if ( getFromState( NONCE_KEY ) == null )
        {
            addOrReplaceToState( NONCE_KEY, new BigInteger( 128, new SecureRandom() ).toString( 36 ) );
        }
    }

    public String getFromState( final String key )
    {
        return QueryParamUtils.parseFromQueryParams( getStateFromStorage( getSession() ), key );
    }

    private HttpSession getSession()
    {
        return portalRequestSupplier.get().getRawRequest().getSession( true );
    }

    private static String getStateFromStorage( final HttpSession session )
    {
        return Objects.requireNonNullElse( (String) session.getAttribute( STATE ), "" );
    }

    @Override
    public void initialize( final BeanContext context )
    {
        this.portalRequestSupplier = context.getBinding( PortalRequest.class );
    }
}
