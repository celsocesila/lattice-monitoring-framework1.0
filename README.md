# Lattice Monitoring Framework
This project contains both the source code and some pre-compiled binaries of the Lattice Monitoring Framework.

The main components of the framework are:
- The Controller
- Data Sources
- Data Consumers


In general, we can assume that a single Controller instance is up and running in a given resource technological domain. However, alternative monitoring topologies might be used.

### Build
The provided `build.xml` (under `source/`) can be used both for compiling the source code and generating the deployable jar files.
```sh
$ cd source/
$ ant dist
```
The above command generates two different jar binary files in the jars directory:
- `monitoring-bin-controller.jar` containing all the classes and dependencies related to the controller.
- `monitoring-bin-core.jar` containing a subset of classes and dependencies that can be used for instantiating Data Sources and Data Consumers.

and also a jar containing the source code
- `monitoring-src.jar`

### Installation
As soon as the Build process is completed, the controller can be started as follows:
```sh
$ cd jars/
$ java -cp monitoring-bin-controller.jar eu.fivegex.monitoring.control.controller.Controller controller.properties
```
The `controller.properties` file contains the configuration settings for the controller (an example is reported under `conf/`)

### Configuration
```
info.localport = 6699
``` 
is the local port used by the Controller when connecting to the Information Plane. Other Lattice entities (e.g., Data Sources) will remotely connect to this port once started.

```
restconsole.localport = 6666
```
is the port where the controller will listen for HTTP control requests coming from the orchestration layer (i.e., IMoS).
```
probes.package = eu.fivegex.appl.probes
probes.suffix = Probe
```
When generating the probe catalogue, the Controller will look for all the class files whose package name matches `eu.fivegex.appl.probes` and whose name terminates with the `Probe` suffix (e.g., `eu.fivegex.appl.probes.docker.DockerProbe.class`).

```
deployment.enabled = true
```
Can be set either to `true` or `false` and enables/disables respectively the automated Data Sources deployment functionality to a remote host (current implementation is based on SSH with public key authentication)
```
deployment.localJarPath = /Users/lattice
deployment.jarFileName = monitoring-bin-core.jar
deployment.remoteJarPath = /tmp
deployment.ds.className = eu.fivegex.monitoring.appl.datasources.ZMQDataSourceDaemon
deployment.dc.className = eu.fivegex.monitoring.appl.dataconsumers.ZMQControllableDataConsumerDaemon
```
The above settings allow to specify (in order):
- the path where the jar (to be used for the Data Sources / Consumers automated remote deployment) is located
- the file name of the above jar file
- the path where the jar will be copied on the remote machine where the Data Source is being deployed
- the class name of the Data Source to be started (it must exist in the specified jar)
- the class name of the Data Consumer to be started (it must exist in the specified jar)


### Testing
In order to verify that the Controller is up and running, a quick check consists in performing a `GET` request to `/probe/catalogue/`. The expected result is a response code `200` and `Content-Type: application/json`.


### Usage for the vCDN experiment
Lattice is the monitoring backend that will enable collecting measurements from the VNFs deployed via the 5GEx Orchestration System. It can be used e.g., to retrieve measurements from Docker containers and / or Openstack VMs. When Lattice is used with Docker, the system running the Docker Engine has to be configured to expose the REST API on port 4243 (see [Docker documentation](https://success.docker.com/article/how-do-i-enable-the-remote-api-for-dockerd)).
