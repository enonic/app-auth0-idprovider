# Auth0 ID Provider App for Enonic XP

This app contains an ID Provider using Auth0 single sign-on services.

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
    * Application Domain: Copy the field "Domain" from your Auth0 client settings.
    * Application Client ID: Copy the field "Client ID" from your Auth0 client settings.
    * Application secret: Copy the field "Client Secret" from your Auth0 client settings.
    * (Optional) Groups: Groups to associate to new users   
            
### Step 5: Create and configure the user store
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
1. Define the ID provider callback in the "Allowed Callback URLs"
    * The ID provider is listening on "/portal/[branch]/_/idprovider/[userstore]"
    * If you have a virtual host mapping hiding "/portal/[branch]", then use the virtual host mapping source + "_/idprovider/<userstore>". 
    * For the example above, the full callback URL will be: "https://example.com/_/idprovider/myuserstore"


## Releases and Compatibility

| App version | Required XP version | Download |
| ----------- | ------------------- | -------- |


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