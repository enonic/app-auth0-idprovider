package com.enonic.app.auth0.impl.utils;

import javax.servlet.http.HttpServletRequest;

public class SessionUtils
{
    public static final String STATE = "com.auth0.state";

    public static String getState( final HttpServletRequest httpServletRequest )
    {
        return (String) httpServletRequest.getSession( true ).getAttribute( STATE );
    }

    public static void setState(final HttpServletRequest httpServletRequest, final String state) {
        httpServletRequest.getSession( true ).setAttribute(STATE, state);
    }
}
