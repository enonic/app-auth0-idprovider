package com.enonic.app.auth0;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.squareup.okhttp.FormEncodingBuilder;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

public final class HttpRequest
{
    private String url;

    private Map<String, String> params = new HashMap<>();

    public HttpRequest setUrl( final String url )
    {
        this.url = url;
        return this;
    }

    public HttpRequest addParam( final String key, String value )
    {
        this.params.put( key, value );
        return this;
    }

    public String execute()
        throws IOException
    {
        final RequestBody requestBody = generateRequestBody();
        final String responseBody = sendRequest( url, requestBody );
        return responseBody;
    }

    private RequestBody generateRequestBody()
    {
        if ( params == null )
        {
            return null;
        }

        final FormEncodingBuilder formEncodingBuilder = new FormEncodingBuilder();
        for ( Map.Entry<String, String> param : params.entrySet() )
        {
            formEncodingBuilder.add( param.getKey(), param.getValue() );
        }

        return formEncodingBuilder.build();
    }

    private String sendRequest( final String url, final RequestBody post )
        throws IOException
    {
        //Builds the request
        Request.Builder requestBuilder = new Request.Builder().url( url );
        if ( post != null )
        {
            requestBuilder.post( post );
        }
        Request request = requestBuilder.build();

        //Executes the request
        final OkHttpClient okHttpClient = new OkHttpClient();
        Response response = okHttpClient.newCall( request ).execute();
        final String responseBody = response.body().string();
        return responseBody;
    }

}