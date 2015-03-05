# Summary

re-usable set of libaries for building Jetty based applications.

# Usage (Development)
To start from IDE, simply run class StartServerInDevelopment
This uses properties defined in app-test.properties (including http port) and log configuration from logback-test-config.xml

# Usage (Prod)
TBD

# Features
- Automatic JMX configuration (on http-port + 1)
- Automatic status and monitoring url (on http-port + 5, under http://<host>/status
  - Includes Health check, metrics, thread dump and ping 
  
- TBD:  See plugins

# Dependencies
- OpenRight Master
- Jetty
- metrics.dropwizard.io
