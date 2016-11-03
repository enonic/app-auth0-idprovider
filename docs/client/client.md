# Create and configure an Auth0 client

### Step 1: Create a client application (or reuse the default app) 

1. In "Clients", click on the button "CREATE CLIENT" 
![](img/create-client.png)

1. Define your client name and select "Regular Web Applications" as client type.
![](img/create-client-2.png)


### Step 2: Configure the client settings

1. Your client should have the type "Regular Web Application" and the Token Endpoint Authentication Method "POST".
![](img/client-settings.png)

1. Below, enable "Use Auth0 instead of the IdP to do Single Sign On"
![](img/sso-setting.png)

1. (Recommended) Add the rule "Force email verification"
    
    1. Create a new rule
    ![](img/rule.png)
    
    1. Select the rule "Force email verification"
    ![](img/rule-2.png)
    
    1. Save
    ![](img/rule-3.png)

