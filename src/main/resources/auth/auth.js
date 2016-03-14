var mustacheLib = require('/lib/xp/mustache');
var portalLib = require('/lib/xp/portal');

exports.handle403 = function (req) {
    var authConfig = portalLib.getAuthConfig();
    var callbackUrl = portalLib.rewriteUrl({url: "/auth0", type: 'absolute'});
    var view = resolve('auth.html');
    var params = {
        authConfig: authConfig,
        currentPath: req.request.path,
        callbackUrl: callbackUrl
    };
    var body = mustacheLib.render(view, params);

    return {
        contentType: 'text/html',
        body: body
    };
}
