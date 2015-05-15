# Plug4Green demo

This module provides a demonstration of Plug4Green (P4G) connected to OpenStack (OS).
P4G is an energy-aware VM placement algorithm able to compute the placement of the VMs and state of the servers. In particular P4G achieves the VMs consolidation and server state management based on configured SLAs and description of the current configuration of the data center. 
The Openstack communicator module provides both the actuation of P4G energy and SLA aware policies (e.g., live migrate a VM from a server to another…) and the updating description of the data center configuration (real-time CPU/Ram usage, VMs position…).
In the following sections we describe how to install Openstack in virtual infrastruture on a single laptop, in order to test P4G energy management.

## Requirements:

- VirtualBox with VirtualBox Extension Pack
- Git 
- At least 10Gb of free RAM 
- At least 1 physical core
- Internet connection

Tested on Mac OS and Ubuntu.

## Openstack installation
This section details an Openstack installation under VirtualBox using Fuel. Such installation can be run on a single laptop. 
It is possible to change the configuration while the fuel master is booting. This guide considers the default configuration, in particular for network. 

```
Clone the repo:
$ git clone https://github.com/stackforge/fuel-main.git

Download the fuel iso:
$ wget http://seed.fuel-infra.org/fuelweb-community-release/fuel-community-6.0.iso.torrent
$ xdg-open fuel-community-6.0.iso.torrent

Move the iso in the iso folder of the repository:
$ mv fuel-community-6.0.iso fuel-main/virtualbox/iso

```
At this point we need to create the VMs for the Openstack installation using the script "fuel-main/virtualbox/launch_8GB.sh".
This script will create 1 OS master and 3 slaves. Each VM uses about 1,5Gb of RAM. One is used for the Openstack controller and the other two to host the VMs (compute nodes).
Accept default values in the Fuel setup, save and quit. This step may take a long time.

```
cd fuel-main/virtualbox/
./launch_8GB.sh 
```

Once the fuel master VM and the two slaves displays their login invite, you can access the fuel master GUI through a browser:

```
xdg-open http://10.20.0.2:8000 (credential: admin both as user and password)
```

1. Create a new environment
2. Name and Release: assign name and choose a release
3. Deployment Mode: multi-node
4. Compute: QEMU
5. Networking Setup: the default is sufficient (Neutron with VLAN)
6. Storage Backends: choose Ceph for both options

It is suggested to verify the network settings: inside the Networks tab click on verify.
In the tab settings:

- Add your public key or keys that will be authorized to access the nodes
- Check options:
 
	- Ceph RBD for volumes (Cinder)
	- Ceph RBD for image (Glance)
	- Ceph RBD for ephemeral volumes (Nova)

In the Nodes tab, using Add Nodes button, assign to each discovered node a role as follows:

1. Assign "Controller" to a VM
2. Assign "Compute" & "Storage - Ceph OSD" to the two other VMs

After that you will be able to click on "Deploy Changes". It may require from some minutes to hours based on the system performances.

### Environment setup

After the installation, you would be able to access the Openstack graphical interface through a browser at the address 172.16.0.2.
In the Project tab you can create two instances (VM) with the configuration you prefer. I suggest to use the smallest flavor.
The VMs position could be displayed through the nova command line as follows:

1. Connect to the controller: ssh root@172.16.0.2
2. Type the command “nova list” to show the VMs list
“nova show <VM identifier>” displays also the position of the VM.
3. It possible to test to move a VM to another node to test the consolidation with the command: nova live-migrate <VM identifier> <destination node name>

## Plug4Green configuration
Plug4Green uses two configuration files:

- f4gmodel_OS.xml: description of the data center
- SLA_OS.xml: service-level agreement configuration

##Plug4Green Demo

The Plug4Green should be compiled with mvn clean install and can be started directly from Eclipse or from the jar created in the folder P4GDemo/target.
P4G demo uses two configuration files:

- P4GDemo/src/main/config/core/f4gconfig.properties: contains the reference to the SLA and Datacenter models.
- P4GDemo/src/main/config/ComOpenstack/config.yaml: provides the instructions to reach the Openstack infrastructure.

All this configuration files should be putted at the same lavel of the jar or inside the suggested path.



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

