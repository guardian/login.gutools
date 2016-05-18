# login.gutools

Small application to login a user via pan-domain-auth and redirect them.

## Running locally

To run as to behave like production (uses `application.conf`) `./sbt run`

To run in developer mode (pulls in `application.local.conf`) `./sbt devrun`

To run in debug mode `./sbt --debug`

### Emergency access when Google auth is down

There will be a limited number of users that can switch emergency access on and off. This will be required if Google Auth 
is down. 

Users that can change the switch will have their userId and a password hash stored in DynanoDB.

`userId` is the username of a Guardian email address e.g joe.bloggs if the email address is joe.bloggs@guardian.co.uk
`passwordHash` is generated using [bCrypt](https://github.com/t3hnar/scala-bcrypt). To generate a hash:
```
./sbt
import com.github.t3hnar.bcrypt._
"[password-value]".bcrypt
```

