var authLib = require('/lib/xp/auth');
var mustacheLib = require('/lib/xp/mustache');
var portalLib = require('/lib/xp/portal');

exports.login = function (req) {
    var authConfig = authLib.getIdProviderConfig();
    var callbackUrl = portalLib.url({path: "/auth0", type: 'absolute'});
    var view = resolve('identity.html');
    var params = {
        authConfig: authConfig,
        currentPath: req.path,
        callbackUrl: callbackUrl
    };
    var body = mustacheLib.render(view, params);

    return {
        contentType: 'text/html',
        body: body
    };
};

exports.logout = function (req) {
    authLib.logout();

    if (req.params.redirect) {
        return {
            redirect: req.params.redirect
        }
    }
}
