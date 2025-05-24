FROM python:3.10-alpine AS build

# Copied for local install inorder to get the right architecture

ARG VERSION=2.2.2

RUN addgroup -S sendria && adduser -S sendria -G sendria
WORKDIR /home/sendria
USER sendria
RUN python3 -m pip install --user sendria==$VERSION
ENV PATH="/home/sendria/.local/bin:$PATH"

EXPOSE 1025 1080

ENTRYPOINT [ "sendria", "--foreground", "--db=./mails.sqlite", "--smtp-ip=0.0.0.0", "--http-ip=0.0.0.0" ]
