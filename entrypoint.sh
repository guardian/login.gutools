#!/usr/bin/env bash

# setup nginx
# TODO don't do this every time the container starts?
cd /dev-nginx
cp ssl/* /etc/nginx/
./setup-app.rb /code/nginx/nginx-mapping.yml

# start nginx
nginx

# run app
cd /code
./start-login.sh
