# login.gutools

Small application to login a user via pan-domain-auth and redirect them.

## Running locally
1. Get credentials for `composer` from [Janus](https://janus.gutools.co.uk/multi-credentials?&permissionIds=composer-dev&tzOffset=1).
1. Run `./script/setup`.
1. Run `./script/start`.
1. Open `https://login.local.dev-gutools.co.uk`.

### Interactive debugging
1. Setup your IDE to attach to port 5005. [Here are instructions for IntelliJ](https://www.jetbrains.com/help/idea/run-debug-configuration-remote-debug.html#1).
2. Run `./script/start --debug`.
3. Start debugging in your IDE.

### Emergency access when Google auth is down

If the Google Auth service goes down it is possible to use an emergency feature that will extend the cookie lifetime by 1 day for users already signed in. When the switch is on users will be required to access the `/emergency/reissue` endpoint to extend the cookie lifetime.

If users do not have a cookie issued, they can request an email with a link for obtaining a new cookie through the `/emergency/request-cookie` endpoint.

If a lot users are requesting new cookies, we might have to increase the read/write capacity of the dynamo table where cookie tokens are stored.

#### Managing emergency access

Emergency access is configured using an S3-based configuration file, which is updated via the `emergency-access` script. This replaces the previous approach for emergency access management and provides a simple command-line interface to enable or disable emergency access.

There are three ways to authorise enabling the switch:
- [Composer janus credentials](#composer-janus-credentials)
- [Emergency login switch access key](#emergency-login-switch-access-key)
- [Using break-glass credentials](#using-break-glass-credentials)

The switch takes time to update after changes are made - **up to 60 seconds** for the application to pick up the new state. You can check the current switch state at: https://login.gutools.co.uk/switches.

#### Composer janus credentials

If you can access Janus or already have access to Composer credentials with login config bucket write permissions, you can run the script using those credentials.

```bash
# Enable emergency access for a stage (with composer aws credentials)
./script/emergency-access enable PROD --profile composer

# Disable emergency access for a stage (with composer aws credentials)
./script/emergency-access disable PROD --profile composer
```

#### Emergency login switch access key

If you have been [issued an emergency login access key](#provisioning-emergency-access-keys) you can run the script using those credentials.

```bash
# Enable emergency access for a stage (with emergency-login aws credentials)
./script/emergency-access enable PROD --profile emergency-login

# Disable emergency access for a stage (with emergency-login aws credentials)
./script/emergency-access disable PROD --profile emergency-login
```

#### Using break-glass credentials

If access to AWS via Janus is an issue and no access keys are available, break-glass credentials may need to be used. To get an access key:

1. Access the [AWS console](https://console.aws.amazon.com/) using break glass credentials
2. Go to your AWS IAM user page
3. Navigate to "Security credentials"
4. Go to "Access keys"
5. Create a new access key
6. Remove the key once access is no longer needed

Set the following environment variables with your break-glass credentials and omit the `--profile` option when running the script:

```bash
export AWS_ACCESS_KEY_ID=your_access_key_id
export AWS_SECRET_ACCESS_KEY=your_secret_access_key

./script/emergency-access enable PROD
```

#### Emergency comms

Central Production will send comms to all users letting them know what to do:

> We are aware of issues with Google authentication which may affect your access to editorial tools, including Composer, Workflow and The Grid. 
> The developers are investigating as a matter of urgency.
> 
> If you are having authentication issues, please click on this link to extend your session and then navigate to one of the editorial tools:
>   https://login.gutools.co.uk/emergency/reissue
>   
> If the extend link doesn't work please click below to get a link send to your email inbox:
>   https://login.gutools.co.uk/emergency/request-cookie

In the event that Gmail is also down and users can't receive emails, you can fish out a login token to send to them by other means.
You will need `composer` Janus credentials, or potentially regular IAM credentials (aka break-glass credentials) if you cannot log in
to Janus. Once they have visited the `/emergency/request-cookie` endpoint:

```
./script/get-emergency-login-link firstname.lastname@guardian.co.uk
```

Be careful when sharing the link using WhatsApp etc. They will ping the link to build a preview unless it is sent in
quotes which will use up the token and require the user to request another one.

### Provisioning emergency access keys

An Engineering Manager of the Journalism Stream can provide an emergency access key to users as required.

The manager can create a new user in AWS IAM as part of the `emergency-login-switch-users-PROD` group. It is recommended 
to affix `-emergency-login` to the end of the name. The `GoogleUsername` tag should be used on any created account for 
ease of identifying ownership. An access key can then be created and shared with the user. 

It is recommended to store the credentials using the AWS CLI for ease of access in an emergency:

```bash 
aws configure set aws_access_key_id [access_key] --profile emergency-login   
aws configure set aws_secret_access_key [secret_access_key] --profile emergency-login
```