# Auth0 ID Provider App for Enonic XP

Authenticate your users using Auth0 services.
This ID Providers handles log-in, sign-up, reset password, single sign-on, validation rules, Gravatar profile retrieval.
The interface is based on Auth0 Lock widget, configurable through the ID Provider configuration.


## Usage

### Step 1: If you do not have one, [create an account on https://auth0.com/](docs/account/account.md)

### Step 2: [Create and configure an Auth0 client](docs/client/client.md)

### Step 3: Install the application
1. In the admin tool "Applications" of your Enonic XP installation, click on "Install". 
1. Select the tab "Enonic Market", find "Auth0 ID Provider", and click on the link "Install".

### Step 4: Create and configure the user store
1. In the admin tool "Users", click on "New".
1. Fill in the fields and, for the field "ID Provider", select the application "Auth0 ID Provider".
1. Configure the ID Provider:
    * Client
        * Domain: Copy the field "Domain" from your Auth0 client settings.
        * Client ID: Copy the field "Client ID" from your Auth0 client settings.
        * Client secret: Copy the field "Client Secret" from your Auth0 client settings.
    * (Optional) Groups
        * Groups: Groups automatically associated to new users
    * (Optional) Widget Options
        * Display
            * Allowed connection: Connections displayed by the widget. Defaults to all enabled connections. Examples: Username-Password-Authentication, github
            * Display avatar: Whether or not the user avatar and display name is fetched from Gravatar and display in the widget header.
            * Language: Language of the widget.
            * Title: Title of the widget.
        * Theme
            * Labelled submit button: Whether or not the submit button should have a label. When unchecked, an icon will be shown instead.
            * Logo URL: Image URL  that will be placed in the widget's header. Defaults to Auth0's logo
            * Primary color: Primary color of the widget. Defaults to #ea5323.
        * Social
            * Social button style: Size of the buttons for the social providers.
        * Database
            * Allow login: When unchecked, the widget won't display the login screen
            * Allow forgot password: When unchecked, the widget won't display the "Don't remember your password?" link
            * Allow signup: When unchecked, the widget won't display the signup screen
            * Initial screen: Name of the screen that will be shown when the widget is opened.
            * Login after sign up: Whether or not the user will be signed in after a successful sign up.
            
1. Apply and save
            
### Step 5: Configure the virtual host mapping
1. Edit the configuration file "com.enonic.xp.web.vhost.cfg", and set the new user store to your virtual host.
(See [Virtual Host Configuration](http://xp.readthedocs.io/en/stable/operations/configuration.html#configuration-vhost) for more information).

    ```ini
    enabled=true
      
    mapping.localhost.host = localhost
    mapping.localhost.source = /
    mapping.localhost.target = /
    mapping.localhost.userStore = system
    
    mapping.example.host = example.com
    mapping.example.source = /
    mapping.example.target = /portal/master/mysite
    mapping.example.userStore = myuserstore
    ```
                
### Step 6: Define the allowed callback URLs
1. Go back to your Auth0 Client settings
1. Define the "Allowed Origins (CORS)"
    * For the example above, the value would be: https://example.com
1. Define the ID provider callback URL in the field "Allowed Callback URLs"
    * The ID provider is listening for callbacks on "/portal/[branch]/_/idprovider/[userstore]"
    * If you have a virtual host mapping hiding "/portal/[branch]", like the example above, then use the virtual host mapping source + "_/idprovider/<userstore>". 
    * For the example above, the value would be: "https://example.com/_/idprovider/myuserstore"
1. Define the field "Allowed Logout URLs"
    * If you use the Javascript function "portalLib.logout()" with redirection, please list the redirection URLs in the field "Allowed Logout URLs"
    * For the example above, the value could be: "https://example.com/"


## Releases and Compatibility

| App version | Required XP version | Download |
| ----------- | ------------------- | -------- |
| 1.0.0 | 6.8.0 | [Download](http://repo.enonic.com/public/com/enonic/app/auth0idprovider/1.0.0/auth0idprovider-1.0.0.jar) |


## Building and deploying

Build this application from the command line. Go to the root of the project and enter:

    ./gradlew clean build

To deploy the app, set `$XP_HOME` environment variable and enter:

    ./gradlew deploy


## Releasing new version

To release a new version of this app, please follow the steps below:

1. Update `version` (and possibly `xpVersion`) in  `gradle.properties`.

2. Compile and deploy to our Maven repository:

    ./gradlew clean build uploadArchives

3. Update `README.md` file with new version information and compatibility.

4. Tag the source code using `git tag` command (where `X.Y.Z` is the released version):

    git tag vX.Y.Z

5. Push the updated code to GitHub.

    git push origin vX.Y.Z