var authLib = require('/lib/xp/auth');
var mustacheLib = require('/lib/mustache');
var portalLib = require('/lib/xp/portal');
var callbackLib = require('/lib/callback');
var stateLib = require('/lib/state');

exports.handle401 = function (req) {
    var redirectUrl = retrieveRequestUrl();
    return {
        status: 401,
        contentType: 'text/html',
        body: generateLoginPage(redirectUrl)
    };
};

exports.get = function (req) {
    var redirectUrl = stateLib.getFromState('redirect') || generateRedirectUrl();
    if (req.params.error) {
        return {
            contentType: 'text/html',
            body: generateLoginPage(redirectUrl, req.params.error_description)
        };
    } else if (req.params.state) {
        var success = callbackLib.handle();
        if (success) {
            return {
                redirect: redirectUrl
            }
        }
    }
    
    return {
        contentType: 'text/html',
        body: generateLoginPage(redirectUrl)
    };
};

exports.login = function (req) {
    var redirectUrl = req.validTicket ? req.params.redirect : generateRedirectUrl();
    return {
        contentType: 'text/html',
        body: generateLoginPage(redirectUrl)
    };
};

exports.logout = function (req) {
    authLib.logout();

    var redirectUrl = req.validTicket && req.params.redirect;
    var authConfig = authLib.getIdProviderConfig();

    return {
        redirect: "https://" + authConfig.appDomain + "/v2/logout?" +
                  (redirectUrl ? "returnTo=" + encodeURIComponent(redirectUrl) + "&" : "") +
                  "client_id=" + authConfig.appClientId
    }
};

function generateRedirectUrl() {
    var site = portalLib.getSite();
    if (site) {
        return portalLib.pageUrl({id: site._id, type: "absolute"});
    }
    return generateServerUrl() + '/';
}

function generateLoginPage(redirectUrl, error) {
    var idProviderKey = portalLib.getIdProviderKey();
    stateLib.addNonceToState();
    stateLib.addOrReplaceToState('idprovider', idProviderKey);
    var state = stateLib.addOrReplaceToState('redirect', redirectUrl);
    var callbackUrl = portalLib.idProviderUrl({
        idProviderKey: idProviderKey,
        type: 'absolute'
    });

    var configScript = mustacheLib.render(resolve('config.txt'), {
        lockOptions: JSON.stringify(generateLockOptions(callbackUrl, state)),
        auth0Options: JSON.stringify(generateAuth0Options(callbackUrl, state)),
        checkSessionOptions: JSON.stringify(generateCheckSessionOptions(callbackUrl, state)),
        authorizeUrl: generateAuthorizeUrl(callbackUrl, state)
    });

    var params = {
        configScript: configScript,
        authConfig: authLib.getIdProviderConfig(),
        error: error
    };

    var view = resolve('idprovider.html');
    return mustacheLib.render(view, params);
};

function generateLockOptions(callbackUrl, state) {
    var authConfig = authLib.getIdProviderConfig();
    return {
        auth: {
            redirectUrl: callbackUrl,
            sso: true,
            params: {
                state: state,
                scope: 'openid profile email'
            }
        },
        allowedConnections: toArray(authConfig.allowedConnections),
        avatar: authConfig.displayAvatar ? undefined : null,
        closable: false,
        language: authConfig.language || 'en',
        languageDictionary: {
            title: authConfig.title || 'Auth0'
        },
        theme: {
            labeledSubmitButton: authConfig.labeledSubmitButton,
            logo: authConfig.logo || undefined,
            primaryColor: authConfig.primaryColor || undefined
        },
        socialButtonStyle: authConfig.socialButtonStyle || 'small',
        allowLogin: authConfig.allowLogin,
        allowForgotPassword: authConfig.allowForgotPassword,
        allowSignUp: authConfig.allowSignUp,
        initialScreen: authConfig.initialScreen || 'login',
        loginAfterSignUp: authConfig.loginAfterSignUp
    };
}

function generateAuth0Options(callbackUrl, state) {
    var authConfig = authLib.getIdProviderConfig();
    return {
        domain: authConfig.appDomain,
        clientID: authConfig.appClientId,

    };
}

function generateCheckSessionOptions(callbackUrl, state) {
    return {
        responseType: 'token',
        redirect_uri: callbackUrl
    };
}

function generateAuthorizeUrl(callbackUrl, state) {
    var authConfig = authLib.getIdProviderConfig();
    return 'https://' + authConfig.appDomain + '/authorize?' +
           'scope=openid%20profile%20email' +
           '&response_type=code' +
           '&sso=true' +
           '&state=' + encodeURIComponent(state) +
           '&client_id=' + authConfig.appClientId +
           '&redirect_uri=' + encodeURIComponent(callbackUrl) +
           '&prompt=none'
}


function toArray(object, defaultValue) {
    if (!object) {
        return defaultValue;
    }
    if (object.constructor === Array) {
        return object;
    }
    return [object];
}

function retrieveRequestUrl() {
    var requestUrlRetriever = __.newBean('com.enonic.app.auth0.impl.RequestUrlRetriever');
    return __.toNativeObject(requestUrlRetriever.execute());
}

function generateServerUrl() {
    var serverUrlGenerator = __.newBean('com.enonic.app.auth0.impl.ServerUrlGenerator');
    return __.toNativeObject(serverUrlGenerator.execute());
}


