#!/usr/bin/env bash

# setup nginx
# TODO don't do this every time the container starts?
cd /dev-nginx
cp ssl/* /etc/nginx/
./setup-app.rb /code/nginx/nginx-mapping.yml
./restart-nginx.sh

# run app
cd /code
./activator "~ run -Dconfig.resource=application.local.conf -Dpandomain.aws.keyId=$AWS_ACCESS_KEY_ID -Dpandomain.aws.secret=$AWS_SECRET_ACCESS_KEY"
