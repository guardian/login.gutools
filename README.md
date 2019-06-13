# login.gutools

Small application to login a user via pan-domain-auth and redirect them.

## Running locally
1. Get credentials for `composer` and `workflow` from [Janus](https://janus.gutools.co.uk/multi-credentials?&permissionIds=composer-dev,workflow-dev&tzOffset=1).
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
`passwordHash` is generated using [bCrypt](https://github.com/t3hnar/scala-bcrypt). To generate a hash:
```
sbt console
import com.github.t3hnar.bcrypt._
"[password-value]".bcrypt
```

To turn a switch on or off run:
```
curl -X POST 'https://[login-domain]/switches/emergency/[on|off]' -k -H 'Authorization: Basic [firstname.lastname]@guardian.co.uk:[password]'
```

