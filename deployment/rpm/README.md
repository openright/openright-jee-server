Maven module for generating RPM package from an openright java server.

It uses the maven tiles plugin for minimal configuration of the target server.

The generated package has been tested and should be compatible with both redhat based distros (RHEL, centos, fedora) as well as systems compatible with LSB (Linux Standard Base) such as Ubuntu.

# Features

- simple, single command to install or upgrade
- proper file system layout for application (see structure below)
- shared config hierarchy when installing multiple services (micro-services) on same host
- automatically generates proper jmx config
- start/stop/restart as a standard linux service
- automatic restart of application on host reboot
- logging to standard linux location (/var/log/) for easy management
- managed upgrades and downgrades (preserving user edits to configuration files)
- can be integrated with a linux repository such as YUM for easy version management


# Usage
See rpm example under examples project for usage

Once an application is installed it can be managed like a regular linux service
`sudo service <appname> [start|stop|restart|status]`

# Installation
The generated RPM can be installed using rpm or yum on redhat/centos/fedora.
`sudo rpm -i <rpm file name>`

On ubuntu it can be installed using `sudo alien -i --scripts <rpm file name>`

Note:  If you have a Nexus repository for maven artifacts, this can be used as a YUM repository to simplify deployment.

If this is registered as a YUM repo on a linux machine, you can use `sudo yum install <base app name>` instead of downloading and installing locally.  This is recommended for projects and environments where it is frequently updated.

# Requirements
Install java jdk 1.8 or newer

# Structure
The package by default generates the following file system layout (in accordance with the Linux FHS layout)

Location                      | Description
----------------------------  | -----------
/opt/\<username>/conf          | contains shared config files and jmx config
/opt/\<appname>                | home directory of app
/opt/\<appname>/bin            | startup scripts referenced by the service
/opt/\<appname>/conf           | application configuration (most notable app.properties which contains http port)
/opt/\<appname>/lib            | all application libraries and wars
/opt/\<appname>/webapps        | location of the unpacked war for jetty to run
/opt/\<appname>/logs           | symlink to /var/log/<username>/<appname>/  where log files are stored
/etc/init.d/\<appname>         | startup service name for automatic restart on reboot etc
/var/run/\<username>           | Location of pid files for the process (if running)
/var/log/\<username>/\<appname> | Location of all log files
