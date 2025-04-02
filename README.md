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

There will be a limited number of users that can switch emergency access on and off. This will be required if Google Auth
is down.

Users that can change the switch will have their userId and a password hash stored in DynamoDB.

`userId` is the username of a Guardian email address e.g joe.bloggs if the email address is joe.bloggs@guardian.co.uk
`passwordHash` is generated using [bCrypt](https://github.com/t3hnar/scala-bcrypt). To generate a hash, checkout this repository and run:
```
sbt console
import com.github.t3hnar.bcrypt._
"[password-value]".boundedBcrypt
```
Add a new item to the Composer DynamoDB table `login.gutools-emergency-access-[STAGE]` containing the userId and password hash.

To turn a switch on or off run:
```
curl -X POST 'https://[login-domain]/switches/emergency/[on|off]' -k -H 'Authorization: Basic [firstname.lastname]@guardian.co.uk:[password]'
```

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
You will need `composer` Janus credentials, or potentially regular IAM crediations (aka break-glass credentials) if you cannot log in
to Janus. Once they have visited the `/emergency/request-cookie` endpoint:

```
./script/get-emergency-login-link firstname.lastname@guardian.co.uk
```

Be careful when sharing the link using WhatsApp etc. They will ping the link to build a preview unless it is sent in
quotes which will use up the token and require the user to request another one.
