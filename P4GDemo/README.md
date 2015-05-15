# Plug4Green demo

This module provides a demonstration of Plug4Green (P4G) connected to OpenStack (OS). The installation of OpenStack is performed in a virtualized environment on a single laptop.\\
P4G is an energy-aware VM placement algorithm able to compute the placement of the VMs and state of the servers. In particular P4G achieves the VMs consolidation and server state management based on configured SLAs and description of the current configuration of the data center. 
The Openstack communicator module provides both the actuation of P4G energy and SLA aware policies (e.g., live migrate a VM from a server to another…) and the updating description of the data center configuration (real-time CPU/Ram usage, VMs position…).

## Requirements:

- VirtualBox with VirtualBox Extension Pack
- Git 
- At least 10Gb of free RAM 
- At least 1 physical core
- Internet connection

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

- Assign "Controller" to a VM
- Assign "Compute" & "Storage - Ceph OSD" to the two other VMs

In the Networks tab: click on verify.

In the Settings tab:

- copy/paste your public key (~/.ssh/id_rsa.pub) in the appropriate field
- Check option Ceph RBD for volumes (Cinder)
- Check option Ceph RBD for image (Glance)
- Check option Ceph RBD for ephemeral volumes (Nova)


After that you will be able to click on "Deploy Changes". It may require from some minutes to hours based on the system performances.

### Create OpenStack VMs

After the installation, you should be able to access the Openstack graphical interface through a browser at [this address](172.16.0.2).
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
java -jar P4GDemo/target/P4GDemo-1.0-jar-with-dependencies.jar
```

After one minute, P4G should migrate one VM to the same node as the second VM, in order to save energy. You can check this using the nova command as shown in the previous section.

Plug4Green Demo uses the following configuration files:

- P4GDemo/src/main/resource/core/f4gmodel_OS.xml: description of the data center
- P4GDemo/src/main/resource/core/SLA_OS.xml: service-level agreement configuration
- P4GDemo/src/main/config/core/f4gconfig.properties: contains the reference to the SLA and Datacenter models.
- P4GDemo/src/main/config/ComOpenstack/config.yaml: provides the instructions to reach the Openstack infrastructure.

## Shutting down

To shutdown the environment, just issue a "Close/ACPI Shutdown" on all VMs.
To stop P4G, just do Ctrl-C.

To restart: start VirtualBox, and restart all VMs. You can then connect to OpenStack GUI and restart the OpenStack VMs.

Enjoy!

# Troubleshooting #

## Problems with VLAN network on VirtualBox ##

I had to manually configure several host-only networks on vbox to get
fuel to properly deploy openstack infrastructure. If
you are experincing error messages during the openstack installation
phase on fuel interface (like no route to host, for instance) you may
try this as well. 

Greate part of content come from this tutorial that
explains how to install fuel manually.

https://docs.mirantis.com/openstack/fuel/fuel-6.0/virtualbox.html

OpenStack needs at least 5 networks to work properly:

### Step-by-step

Openstack needs to separte traffic over 5 networks to work properly.

These networks are:

1. Fuel network (used to boot PXE) and to deploy the environment
2. Storage (used by ceph or cinder)
3. Management (used by nova)
4. Public (used to access from outside)
5. VM (network for new created VMs)


### Setup virtualbox

Our goal will be to use vbox network adapters instead of the VLAN
feature to work around the problem.
Vbox limits to 4 the number of virtual network interfaces per
VM so we will still have to put one of the networks as a VLAN.

First on virtual box, go to

File -> Preferences -> Network

Choose the Host-only Network tab, assure the following configuration
to each vboxnet# interface (note that # is actually a number):

vboxnet0: Admin (PXE)
10.20.0.1

vboxnet1: Public
172.16.0.1

vboxnet2: Management
192.168.0.1

vboxnet3: Storage
192.168.1.1

For all vboxnet interfaces set net mask to 255.255.255.0 and no DHCP.

Now you have to shutdown all your VMs. With the right mouse button
click on Settings and fin the Network pane. Configure 4 network
adapters each one for a different vboxnet assure the following order

Adapter 1 - vboxnet0
Adapter 2 - vboxnet 1
Adapter 3 - vboxnet 2
Adapter 4 - vboxnet 3

At each adapter choose:

Host-only Adapter
in Advanced
choose the PCnet-FAST III adapter instead of Intel (default)
Set promicuous Mode to : Allow All
check the Cable Connected option


Now reboot all your nodes. 

### Setup on fuel

Access the fuel-master node create a new deployment. Use Nova as network.

On the nodes tab, configure for each node the network settings as follows:

eth0 : Admin (PXE) and VM (Fixed)
eth1: Public
eth2: Management
eth3: Storage

On the network tab, assure that only the Use VLAN tagging for
Nova-Network is checked. Put a number as the VLAN tag.
You can test the network now.

