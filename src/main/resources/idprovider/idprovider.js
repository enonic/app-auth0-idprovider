var authLib = require('/lib/xp/auth');
var mustacheLib = require('/lib/xp/mustache');
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

function redirectToSso(redirectUrl) {
    var userStoreKey = portalLib.getUserStoreKey();
    stateLib.addNonceToState();

    stateLib.addOrReplaceToState('userstore', userStoreKey);
    var state = stateLib.addOrReplaceToState('redirect', redirectUrl);
    var callbackUrl = portalLib.idProviderUrl({
        userStore: userStoreKey,
        type: 'absolute'
    });
    var authConfig = authLib.getIdProviderConfig();

    return {
        redirect: 'https://' + authConfig.appDomain + "/authorize?" +
                  "scope=openid%20profile" +
                  "&response_type=code" +
                  "&sso=true" +
                  "&state=" + encodeURIComponent(state) +
                  "&client_id=" + authConfig.appClientId + "" +
                  "&redirect_uri=" + encodeURIComponent(callbackUrl)
    };
}

exports.get = function (req) {
    if (req.params.error) {
        log.info("stateLib.getFromState('redirect'): + " + stateLib.getFromState('redirect'));
        log.info("Error description: + " + req.params.error_description);
        return {
            contentType: 'text/html',
            body: generateLoginPage(stateLib.getFromState('redirect'), req.params.error_description)
        };
    } else if (req.params.state) {
        callbackLib.handle();
        return {
            redirect: stateLib.getFromState('redirect')
        }
    } else {
        var redirectUrl = generateRedirectUrl();
        return redirectToSso(redirectUrl);
    }
};

exports.login = function (req) {
    var redirectUrl = req.validTicket ? req.params.redirect : generateRedirectUrl();
    return redirectToSso(redirectUrl);
};

exports.logout = function (req) {
    authLib.logout();

    var redirectUrl = req.validTicket ? req.params.redirect : generateRedirectUrl();
    var authConfig = authLib.getIdProviderConfig();

    return {
        redirect: "https://" + authConfig.appDomain + "/v2/logout" +
                  "?returnTo=" + encodeURIComponent(redirectUrl) +
                  "&client_id=" + authConfig.appClientId
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
    var userStoreKey = portalLib.getUserStoreKey();
    stateLib.addNonceToState();

    stateLib.addOrReplaceToState('userstore', userStoreKey);
    var state = stateLib.addOrReplaceToState('redirect', redirectUrl);
    var callbackUrl = portalLib.idProviderUrl({
        userStore: userStoreKey,
        type: 'absolute'
    });
    var authConfig = authLib.getIdProviderConfig();
    var showLock = mustacheLib.render(resolve('show-lock.txt'), {error: error});

    var params = {
        authConfig: authConfig,
        callbackUrl: callbackUrl,
        state: state,
        showLock: showLock
    };

    var view = resolve('idprovider.html');
    return mustacheLib.render(view, params);
};

function retrieveRequestUrl() {
    var requestUrlRetriever = __.newBean('com.enonic.app.auth0.impl.RequestUrlRetriever');
    return __.toNativeObject(requestUrlRetriever.execute());
}

function generateServerUrl() {
    var serverUrlGenerator = __.newBean('com.enonic.app.auth0.impl.ServerUrlGenerator');
    return __.toNativeObject(serverUrlGenerator.execute());
}


