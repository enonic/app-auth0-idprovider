package com.enonic.app.auth0.impl.utils;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class QueryParamUtils
{
    public static String addOrReplaceInQueryParams( final String queryParams, final String key, final String value )
    {
        Objects.requireNonNull( queryParams );
        Objects.requireNonNull( key );
        Objects.requireNonNull( value );

        final Map<String, String> map = parse( queryParams );
        map.put( key, value );

        return format( map );
    }

    public static String parseFromQueryParams( final String queryParams, final String key )
    {
        Objects.requireNonNull( queryParams );
        Objects.requireNonNull( key );
        return parse( queryParams ).get( key );
    }

    private static Map<String, String> parse( final String queryParams )
    {
        final Map<String, String> params = new LinkedHashMap<>();
        final String[] pairs = queryParams.split( "&", -1 );
        for ( final String pair : pairs )
        {
            final String[] split = pair.split( "=", 2 );
            if ( split.length == 2 )
            {
                params.put( urlDecode( split[0] ), urlDecode( split[1] ) );
            }
        }
        return params;
    }

    private static String format( final Map<String, String> params )
    {
        if ( params.isEmpty() )
        {
            return "";
        }

        return params.entrySet()
            .stream()
            .map( entry -> urlEncode( entry.getKey() ) + "=" + urlEncode( entry.getValue() ) )
            .collect( Collectors.joining( "&" ) );
    }

    private static String urlEncode( final String value )
    {
        return URLEncoder.encode( value, StandardCharsets.UTF_8 );
    }

    private static String urlDecode( final String value )
    {
        return URLDecoder.decode( value, StandardCharsets.UTF_8 );
    }
}
