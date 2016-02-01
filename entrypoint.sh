#!/usr/bin/env bash

# setup nginx
# TODO don't do this every time the container starts?
cd /dev-nginx
cp ssl/* /etc/nginx/
./setup-app.rb /code/nginx/nginx-mapping.yml
./restart-nginx.sh

# run app
cd /code
./start-login.sh
