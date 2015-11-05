var thymeleaf = require('/lib/xp/thymeleaf');
var mustache = require('/lib/xp/mustache');
var portal = require('/lib/xp/portal');
var auth = require('/lib/xp/auth');

exports.get = function (req) {

    log.info("req %s", JSON.stringify(req, null, 4));

    var user = auth.getUser();
    var logoutServiceUrl = portal.serviceUrl({
        service: "logout"
    });

    var view = resolve('auth0.html');
    var body = thymeleaf.render(view, {
        user: user,
        logoutServiceUrl: logoutServiceUrl
    });

    return {
        contentType: 'text/html',
        body: body,
        pageContributions: {
            bodyEnd: [
                '<script src="https://cdn.auth0.com/js/lock-7.9.min.js"></script>',
                '<script type="text/javascript">' + mustache.render(resolve('auth0js.js'), {currentUrl: req.url}) + '</script>'
            ]
        }
    };
};