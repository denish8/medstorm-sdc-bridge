#!/bin/bash

apt-get update && apt-get install -y gpg
gpg --batch --import "$GPG_PRIVATE_KEY"
