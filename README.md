# login.gutools

Small application to login a user via pan-domain-auth and redirect them.

## Setting up locally

1. Clone this repo
2. Get nginx set up locally following the instructions below

### Nginx

To run correctly in standalone mode we run behind nginx, This can be installed as follows (you may have done
this already if you work with identity or similar):

1. Install nginx:
  * *Linux:*   ```sudo apt-get install nginx```
  * *Mac OSX:* ```brew install nginx```

2. Make sure you have a sites-enabled folder under your nginx home. This should be
  * *Linux:* ```/etc/nginx/sites-enabled```
  * *Mac OSX:* ```/usr/local/etc/nginx/sites-enabled```

3. Make sure your nginx.conf (found in your nginx home - the same location as the sites-enabled folder above) contains the following line in the http{} block:
`include sites-enabled/*;`
  * you may also want to disable the default server on 8080

4. Get the [dev-nginx](https://github.com/guardian/dev-nginx) repo checked out on your machine

5. Set up certs if you've not already done so (see [dev-nginx readme](https://github.com/guardian/dev-nginx))

6. Configure the composer route in nginx

```
    cd <path_of_dev-nginx>
    sudo ./setup-app.rb <path_of_login.gutools>/nginx/nginx-mapping.yml
```

## Running locally

You will need composer and workflow credentials.

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
./sbt console
import com.github.t3hnar.bcrypt._
"[password-value]".bcrypt
```

To turn a switch on or off run:
```
curl -X POST 'https://[login-domain]/switches/emergency/[on|off]' -k -H 'Authorization: Basic [firstname.lastname]@guardian.co.uk:[password]'
```
