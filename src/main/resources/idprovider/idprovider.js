var authLib = require('/lib/xp/auth');
var mustacheLib = require('/lib/xp/mustache');
var portalLib = require('/lib/xp/portal');

exports.handle403 = function (req) {
    var body = generateLoginPage();

    return {
        status: 403,
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
    var redirectUrl = redirectUrl || retrieveRequestUrl();
    var authConfig = authLib.getIdProviderConfig();
    var userStoreKey = authLib.getUserStore().key;
    var callbackUrl = portalLib.url({path: "/auth0", type: 'absolute'});
    var params = {
        authConfig: authConfig,
        redirectUrl: redirectUrl,
        userStoreKey: userStoreKey,
        callbackUrl: callbackUrl
    };

    var view = resolve('idprovider.html');
    return mustacheLib.render(view, params);
};

function retrieveRequestUrl() {
    var requestUrlRetriever = __.newBean('com.enonic.app.auth0.impl.RequestUrlRetriever');
    return __.toNativeObject(requestUrlRetriever.execute());
}


