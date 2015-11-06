package com.enonic.app.auth0;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

public class ResponseWrapper
    extends HttpServletResponseWrapper
{

    private final String callbackUrl;

    private final String refererUrl;

    private boolean redirected;

    /**
     * Constructs a response adaptor wrapping the given response.
     *
     * @param response
     * @throws IllegalArgumentException if the response is null
     */
    public ResponseWrapper( final HttpServletResponse response, final String callbackUrl, final String refererUrl )
    {
        super( response );
        this.callbackUrl = callbackUrl;
        this.refererUrl = refererUrl;
    }

    @Override
    public void setStatus( final int sc )
    {
        if ( 403 == sc )
        {
            try
            {
                setOauth0Redirected();
            }
            catch ( UnsupportedEncodingException e )
            {
                e.printStackTrace();
            }
        }
        else
        {
            super.setStatus( sc );
        }
    }

    @Override
    public PrintWriter getWriter()
        throws IOException
    {
        if ( redirected )
        {
            return new PrintWriter( new StringWriter() );
        }
        return super.getWriter();
    }

    @Override
    public ServletOutputStream getOutputStream()
        throws IOException
    {
        if ( redirected )
        {
            return new ServletOutputStream()
            {
                @Override
                public boolean isReady()
                {
                    return true;
                }

                @Override
                public void setWriteListener( final WriteListener writeListener )
                {

                }

                @Override
                public void write( final int b )
                    throws IOException
                {

                }
            };
        }
        return super.getOutputStream();
    }

    @Override
    public void setHeader( final String name, final String value )
    {
        if ( !redirected )
        {
            super.setHeader( name, value );
        }
    }

    @Override
    public void sendError( final int sc )
        throws IOException
    {
        if ( 403 == sc )
        {
            setOauth0Redirected();
        }
        else
        {
            super.sendError( sc );
        }
    }

    @Override
    public void sendError( final int sc, final String msg )
        throws IOException
    {
        if ( 403 == sc )
        {
            setOauth0Redirected();
        }
        else
        {
            super.sendError( sc, msg );
        }
    }

    private void setOauth0Redirected()
        throws UnsupportedEncodingException
    {
        super.setStatus( 303 );
        super.setHeader( "Location", BearerAuthFilter.AUTH0_DOMAIN + "/login" +
            "?client=" + BearerAuthFilter.CLIENT_ID +
            "&protocol=oauth2" +
            "&redirect_uri=" + URLEncoder.encode( callbackUrl + "?_callback=" + refererUrl, "UTF-8" ) +
            "&response_type=code" +
            "&connection=Username-Password-Authentication" +
            "&scope=openid%20email" );
        redirected = true;
    }
}
