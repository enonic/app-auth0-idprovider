# Auth0 ID Provider App for Enonic XP

This app contains an ID Provider using Auth0 single sign-on services.

## Usage

### Step 1: Create an Auth0 account
TODO

### Step 2: Create an Auth0 client
TODO (+ Add rule "Force email verification")

### Step 3: Install the application
1. In the admin tool "Applications" of your Enonic XP installation, click on "Install". 
2. Select the tab "Enonic Market", find "Auth0 ID Provider", and click on the link "Install".

### Step 4: Create and configure the user store
1. In the admin tool "Users", click on "New".
2. Fill in the fields and, for the field "ID Provider", select the application "Auth0 ID Provider".
3. Configure the ID Provider:
    * Application Domain: Copy the field "Domain" from your client settings in Auth0 clients dashboard
    * Application Client ID: Copy the field "Client ID" from your client settings in Auth0 clients dashboard
    * Application secret: Copy the field "Client Secret" from your client settings in Auth0 clients dashboard
    * (Optional) Groups: Groups to associate to new users   
            
### Step 5: Create and configure the user store
1. Edit the configuration file "com.enonic.xp.web.vhost.cfg", and set the new user store to your virtual host.
(See [Virtual Host Configuration](http://xp.readthedocs.io/en/stable/operations/configuration.html#configuration-vhost) for more information).

    ```ini
    enabled=true
      
    mapping.admin.host = localhost
    mapping.admin.source = /admin
    mapping.admin.target = /admin
    mapping.admin.userStore = system
    
    mapping.mysite.host = localhost
    mapping.mysite.source = /
    mapping.mysite.target = /portal/master/mysite
    mapping.mysite.userStore = myuserstore
    ```


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