# login.gutools

Small application to login a user via pan-domain-auth and redirect them.

## Usage in DEV
This app runs on port 9000, as do many other apps (unless explicitly specified).
Running it through Docker in DEV means you don't have to remember to use unique ports for each app and adjust your nginx config accordingly.

Set the `AWS_ACCESS_KEY_ID` and `AWS_SECRET_ACCESS_KEY` environment variables then run `docker-compose up`.

### NB for OSX:
You'll need to add a line to `/etc/hosts` to route requests to the Docker VM:

```sh
sudo sh -c "echo \"$(docker-machine ip default) login.local.dev-gutools.co.uk\" >> /etc/hosts"
```
