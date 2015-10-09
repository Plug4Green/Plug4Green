# Plug4Green demo

This module provides a demonstration of Plug4Green (P4G) connected to OpenStack (OS). The installation of OpenStack is performed in a virtualized environment on a single laptop.

The Openstack communicator module provides both the actuation of P4G energy and SLA aware policies (e.g., live migrate a VM from a server to another…) and the updating description of the data center configuration (real-time CPU/Ram usage, VMs position…).

## Requirements:

- VirtualBox with VirtualBox Extension Pack
- Git 
- At least 10Gb of free RAM 
- At least 1 physical core
- A *cable* Internet connection (no wifi)

Tested on Mac OS and Ubuntu.

## Openstack installation
This section details an Openstack installation under VirtualBox using Fuel. Such installation can be run on a single laptop. The idea is to use VirtualBox VMs to simulate physical nodes. On one node will be installed the OpenStack Cloud Controller, and on two others OpenStack Compute nodes. The Compute nodes will then be able to host OpenStack VMs, that Plug4Green will manage.

Additional infos about Fuel and OpenStack can be found here:
https://docs.mirantis.com/openstack/fuel/fuel-6.0/user-guide.html

### Fuel installation

```
get Fuel:
$ wget https://github.com/stackforge/fuel-main/archive/6.0.tar.gz
$ tar -xzvf 6.0.tar.gz

Download the fuel iso and move it to the right folder:
$ wget http://seed.fuel-infra.org/fuelweb-community-release/fuel-community-6.0.iso.torrent
$ xdg-open fuel-community-6.0.iso.torrent
$ mv fuel-community-6.0.iso fuel-main-6.0/virtualbox/iso

```

Warning: be sure to use Fuel version 6.0 exactly.

### Start node VMs

At this point we need to create the VirtualBox VMs that will host the Openstack installation, using the script "fuel-main/virtualbox/launch_8GB.sh".
This script will create 1 OS master and 3 slaves. Each VM uses about 1,5Gb of RAM. One is used for the Openstack controller and the other two to host the VMs (compute nodes).

```
cd fuel-main/virtualbox/
./launch_8GB.sh 
```

In the Fuel setup accept all default values, save and quit. This step may take a long time.

### Configure OpenStack environment

Once the fuel master VM and the two slaves displays their login invite, you can access the fuel master GUI through a browser [at this address](http://10.20.0.2:8000) (credential: admin both as user and password)

Click on "Create a new OpenStack environment" and select the following settings:

- Name and Release: assign name and choose a release
- Deployment Mode: multi-node
- Compute: QEMU
- Networking Setup: the default is sufficient (Neutron with VLAN)
- Storage Backends: choose Ceph for both options

In the Nodes tab:
using Add Nodes button, assign to each discovered node a role as follows:

- Assign one VM to the role "Controller"
- Assign *two roles* to the two remaining VMs: "Compute" & "Storage - Ceph OSD"

In the Networks tab: click on verify.

In the Settings tab:

- Copy/paste your public key (~/.ssh/id_rsa.pub) in the appropriate field
- Check option Ceph RBD for volumes (Cinder)
- Check option Ceph RBD for image (Glance)
- Check option Ceph RBD for ephemeral volumes (Nova)


After that you will be able to click on "Deploy Changes". It may require from some minutes to hours based on the system performances.

### Create OpenStack VMs

After the installation, you should be able to access the Openstack graphical interface through a browser at [this address](http://172.16.0.2).
In the Project/Compute/Intances tab, click on "Launch instances".
Create two instances (VM) with the configuration you prefer. I suggest to use the smallest flavor.

### View and migrate OpenStack VMs

The VMs position could be displayed through the nova command line as follows:

```
Connect to the controller: 
$ ssh root@172.16.0.2
Setup the OS variables: 
$ source ./openrc
```

The following commands are then available:
```
to show the VMs list:
$ nova list
to display the position of the VM:
$ nova show <VM identifier>
to migrate a VM to another node: 
$ nova live-migration <VM identifier> <destination node name>
```

##Plug4Green Demo
The environment is now ready to run Plug4Green demo. You should have two openStack VMs on two separate nodes, as displayed by `nova show`. 

```
download and install Plug4Green:
$ git clone git@github.com:Plug4Green/Plug4Green.git
compile:
$ cd Plug4Green
$ mvn clean install -DskipTests
```

Run the demo:

```
$ cd P4GDemo
$ java -jar target/P4GDemo-1.0-jar-with-dependencies.jar
```

After one minute, P4G should migrate one VM to the same node as the second VM, in order to save energy. You can check this using the nova command as shown in the previous section.


### REST APIs

#### trigger optimization

An optimization can be triggered manually using this rest API:

```
POST
http://<P4G host>:<P4G port>/<version>/plug4green/plug4green/startoptimization
```

For example it can be started with this command:

```
curl -i -X POST -H "Content-Type: text/plain" http://localhost:7777/v1/plug4green/plug4green/startoptimization
```

#### VM CPU Demand

You can change 'live' the value of the CPU demand for each VM using the REST API:

```
PUT
http://<P4G host>:<P4G port>/<version>/plug4green/<VM name>/VMCPUDemand
```

For example:

```
curl -i -X PUT -H "Content-Type: text/plain" -d 200 http://localhost:7777/v1/plug4green/a5939941-1963-4ef0-aa15-aa1ce326e6ce/VMCPUDemand
```

This command sets a CPU demand of 200 (i.e. two full cores) for the VM a5939941-1963-4ef0-aa15-aa1ce326e6ce.
The name of the VM should be retrieved from nova as described above.
After issuing this command, you should see one VM moving to another server, because it consumes too much to be colocated.


### P4G config

Plug4Green Demo uses the following configuration files:

- P4GDemo/src/main/resources/core/f4gmodel_OS.xml: description of the data center
- P4GDemo/src/main/resources/core/SLA_OS.xml: service-level agreement configuration
- P4GDemo/src/main/config/core/f4gconfig.properties: contains the reference to the SLA and Datacenter models.
- P4GDemo/src/main/config/ComOpenstack/config.yaml: provides the instructions to reach the Openstack infrastructure.

## Shutdown & restart

To shutdown the environment, just issue a "Close/ACPI Shutdown" on all VMs.
To stop P4G, just do Ctrl-C.

To restart: start VirtualBox, and restart all VMs, starting with the fuel-master. You can then connect to [OpenStack GUI](http://172.16.0.2) and restart the OpenStack VMs.

Enjoy!

