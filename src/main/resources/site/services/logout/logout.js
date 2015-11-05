var authLib = require('/lib/xp/auth');

function handleGet(req) {
    authLib.logout();
    return {
        redirect: req.headers.Referer
    }
}
exports.get = handleGet;