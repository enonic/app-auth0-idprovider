package com.enonic.app.auth0.impl.utils;

import javax.servlet.http.HttpServletRequest;

public class NonceUtils
{
    public static final String NONCE_KEY = "nonce";

    public static void addNonceToStorage(final HttpServletRequest httpServletRequest) {
        final String stateFromStorage = SessionUtils.getState(httpServletRequest) != null ? SessionUtils.getState(httpServletRequest) : "";
        // only add if no existing entry..
        if (!QueryParamUtils.keyInQueryParams(stateFromStorage, NONCE_KEY)) {
            final String updatedState = QueryParamUtils.addOrReplaceInQueryParams(stateFromStorage, NONCE_KEY, NonceFactory.create());
            SessionUtils.setState(httpServletRequest, updatedState);
        }
    }

    public static void removeNonceFromStorage(final HttpServletRequest req) {
        final String stateFromStorage = SessionUtils.getState(req) != null ? SessionUtils.getState(req) : "";
        final String stateFromStorageWithoutNonce = QueryParamUtils.removeFromQueryParams(stateFromStorage, NONCE_KEY);
        SessionUtils.setState(req, stateFromStorageWithoutNonce);
    }

    public static boolean matchesNonceInStorage(final HttpServletRequest req, final String stateFromRequest) {
        final String nonceFromRequest = QueryParamUtils.parseFromQueryParams(stateFromRequest, NONCE_KEY);
        final String stateFromStorage = SessionUtils.getState(req);
        final String nonceFromStorage = QueryParamUtils.parseFromQueryParams(stateFromStorage, NONCE_KEY);
        return nonceFromRequest != null && !nonceFromRequest.isEmpty() && nonceFromRequest.equals(nonceFromStorage);
    }
}
