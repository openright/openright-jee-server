# Openright Java Server

Contains reusable libraries for creating java jetty based servers
and optional modules for packaging a server or any java application into a native container or package manager

Modules
- master-pom:  Contains shared master pom used across modules
- server:  Contains reusable server and webapp libraries for building jetty based java servers
- deployment: Contains optional packaging and deployment modules for building release and deployment artifacts and containers
	Supported:  rpm
	Planned:  docker, vagrant, apt-get,...

