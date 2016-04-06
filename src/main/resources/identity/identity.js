var mustacheLib = require('/lib/xp/mustache');
var portalLib = require('/lib/xp/portal');
var authLib = require('/lib/xp/auth');

exports.handle403 = function (req) {
    log.info("test1");
    var authConfig = authLib.getIdProviderConfig();
    log.info("test2");
    var callbackUrl = portalLib.url({path: "/auth0", type: 'absolute'});
    log.info("test3");
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
