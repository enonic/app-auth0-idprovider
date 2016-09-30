var authLib = require('/lib/xp/auth');
var mustacheLib = require('/lib/xp/mustache');
var portalLib = require('/lib/xp/portal');
var stateLib = require('/lib/state');

exports.handle401 = function (req) {
    var redirectUrl = retrieveRequestUrl()
    var body = generateLoginPage(redirectUrl);

    return {
        status: 401,
        contentType: 'text/html',
        body: body
    };
};

exports.login = function (req) {
    var body = generateLoginPage(req.params.redirect);

    return {
        contentType: 'text/html',
        body: body
    };
};

exports.logout = function (req) {
    authLib.logout();

    var authConfig = authLib.getIdProviderConfig();

    return {
        redirect: "https://" + authConfig.appDomain + "/v2/logout" +
                  (req.params.redirect ? ("?returnTo=" + encodeURIComponent(req.params.redirect)) : "")

    }
};

function generateLoginPage(redirectUrl) {
    var userStoreKey = portalLib.getUserStoreKey();
    stateLib.addNonceToState();

    stateLib.addOrReplaceToState('userstore', userStoreKey);
    var state = stateLib.addOrReplaceToState('redirect', redirectUrl);
    var callbackUrl = portalLib.url({path: "/auth0", type: 'absolute'});
    var authConfig = authLib.getIdProviderConfig();

    var params = {
        authConfig: authConfig,
        callbackUrl: callbackUrl,
        state: state
    };

    var view = resolve('idprovider.html');
    return mustacheLib.render(view, params);
};

function retrieveRequestUrl() {
    var requestUrlRetriever = __.newBean('com.enonic.app.auth0.impl.RequestUrlRetriever');
    return __.toNativeObject(requestUrlRetriever.execute());
}


