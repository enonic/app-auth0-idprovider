exports.addOrReplaceToState = function (key, value) {
    var bean = __.newBean('com.enonic.app.auth0.impl.StateScriptBean');
    return bean.addOrReplaceToState(key, value);
};

exports.addNonceToState = function () {
    var bean = __.newBean('com.enonic.app.auth0.impl.StateScriptBean');
    bean.addNonceToState();
};