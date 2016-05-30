var authLib = require('/lib/xp/auth');
var mustacheLib = require('/lib/xp/mustache');
var portalLib = require('/lib/xp/portal');

exports.login = function (req) {
    var requestUrlRetriever = __.newBean('com.enonic.app.auth0.impl.RequestUrlRetriever');
    var currentUrl = __.toNativeObject(requestUrlRetriever.execute());

    var authConfig = authLib.getIdProviderConfig();
    var userStoreKey = authLib.getUserStore().key;
    var callbackUrl = portalLib.url({path: "/auth0", type: 'absolute'});
    var view = resolve('idprovider.html');
    var params = {
        authConfig: authConfig,
        currentUrl: currentUrl,
        userStoreKey: userStoreKey,
        callbackUrl: callbackUrl
    };
    var body = mustacheLib.render(view, params);

    return {
        contentType: 'text/html',
        body: body
    };
};

//exports.authFilter = function (req) {
//    // Invoked only when user is missing
//    // Probably only implemented if SSO in front of XP. Ala getRemoteUser
//    //req.headers["Basic"];
//    log.info("authFilter:" + JSON.stringify(req, null, 2));
//};
//
//exports.synch = function (req) {
//    log.info("synch:" + JSON.stringify(req, null, 2));
//}


exports.logout = function (req) {
    authLib.logout();

    if (req.params.redirect) {
        return {
            redirect: req.params.redirect
        }
    }

    //var authConfig = authLib.getIdProviderConfig();
    //if (req.params.redirect) {
    //    return {
    //        redirect: "https://" + authConfig.appDomain + "/v2/logout" +
    //                  (req.params.redirect ? ("?returnTo=" + encodeURIComponent(req.params.redirect)) : "")
    //    }
    //}
}
