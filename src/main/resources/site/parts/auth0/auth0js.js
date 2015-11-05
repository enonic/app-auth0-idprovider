var lock = new Auth0Lock('XVe71J5BHZ0o8QPKQEYs7bx2Xa5FHkqL', 'auth0-test.eu.auth0.com');
function signin() {
    lock.show({
        connections: ['Username-Password-Authentication']
        , authParams: {
            scope: 'openid email'
        }
    });
}

function signout() {
    window.location.href = 'https://auth0-test.eu.auth0.com/v2/logout?returnTo={{currentUrl}}';
}

if (window.location.hash) {
    var hash = lock.parseHash(window.location.hash);

    if (hash) {
        if (hash.id_token) {
            fetch('/portal/draft/auth0', {
                headers: {
                    'Authorization': 'Bearer ' + hash.id_token
                },
                credentials: 'include',
                method: 'GET',
                cache: false
            });
            window.location.href = '{{currentUrl}}';
        }

        if (hash.error) {
            alert('There was an error: ' + hash.error + '\n' + hash.error_description);
        }
    }
}