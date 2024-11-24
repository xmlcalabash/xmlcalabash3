# p:send-mail

This project implements `p:send-mail`.

In order to pass the tests, you must be running Sendria. For example, in Docker like so:

```
    sendriasmtp:
      image: msztolcman/sendria:v2.2.2.0
      container_name: sendriasmtp
      ports:
        - 1025:1025
        - 1080:1080
      stdin_open: true
      tty: true
      environment:
        - MAIL_DRIVER=smtp
        - MAIL_HOST=127.0.0.1
        - MAIL_PORT=1025
        - MAIL_USERNAME=username
        - MAIL_PASSWORD=password
        - MAIL_ENCRYPTION=tcp
        - MAIL_FROM_ADDRESS=nobody@example.com
      networks:
        - external_net
```
