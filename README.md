# Summary

re-usable set of libaries for building basic jetty based application containers.

# Usage
This is a set of libraries.  In order to use them a couple of additional classes must be created defining the structure and location of your application.  See the associated example (openright-jee-server-example) for starting points.

# Features
- Automatic JMX configuration (on http-port + 1)
- Automatic status and monitoring url (on http-port + 5, under http://<host>/status
  - Includes Health check, metrics, thread dump and ping 
- Structure for logging
- Gzip filtering is always on
- TBD:  See plugins

# Limitations and possibilities
- This is only libraries for a basic container.  For serving JSP's, REST services, WAR files, integrating with databases, messaging middleware or other stuff this must be extended and the appropriate libraries added. (see Usage). Only static html works out of the box.  Do not even try to add anything that isn't BASIC here. 

- There are few or no restrictions on the type content that can be served.  You may deploy your own custom code as well as be use it to host thirdparty WAR applications.

# Security
- Some care is taken to turn off unnecessary features (such as directory browsing in Jetty), and to turn on OWASP vulnerability checking.  However, the basic container does not include authentication, access control, cross origin checks, secure cookie handling, ddos mitigation or any kind of security feature you'd expect from a fully developed system.  You should take care to follow an appropriate SSDLC (Secure Software Development Life-cycle) to ensure you're understand what the above words mean and can take appropriate measures.

# Dependencies
- Jetty
- metrics.dropwizard.io
