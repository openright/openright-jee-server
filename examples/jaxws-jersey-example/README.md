# JaxWS Jersey Server Example

Example hello world services using JAX-RS and Jersey

# Usage (Development)

Start server from IDE through the StartServerInDevelopment class.

Browse to url http://localhost:10080/example/api/hello/world

You should then see a small json object "{hello: world}"

Browse to url http://localhost:10085/status to see following
- metrics (may be used by monitoring frameworks to track load on system, etc.)
- healthcheck (may be used by monitoring frameworks to check if application is healthy and sound)
- ping (may be used by load balancers, monitoring frameworks such as Nagios etc to check if alive)
- threaddump (shows currently running threads)

Use VisualVM and connect to localhost:10081 to see JMX stats

# REST and json services
This example relies on JAX-RS and Jersey.
See the JaxRsApplication class, and HelloWorldService for pointers on how to create additional services.
