var mustacheLib = require('/lib/xp/mustache');
var portalLib = require('/lib/xp/portal');

exports.handle403 = function (req) {
    var authConfig = portalLib.getAuthConfig();
    var view = resolve('auth.html');
    var params = {
        authConfig: authConfig,
        currentPath: req.request.path
    };
    var body = mustacheLib.render(view, params);

    return {
        contentType: 'text/html',
        body: body
    };
}
