# Dynamo-Like-DB

Dynamo-Like-DB is a distributed object based storage service inspired from [Amazon DynamoDB](https://www.allthingsdistributed.com/files/amazon-dynamo-sosp2007.pdf). 

DLD's features include:
* Gossip protocol, for node disovery and failure detection.
* Vector clock based versioning system for objects.
* Consistent Hashing 

DLD was tested on a system of 4 VMs. The implementation, however is not bug-free.

### Compilation
To compile the project, pull the project using and compile the maven project. Compiling the maven project will produce a jar file which can be exceuted as a Dynamo Node.

### Testing DLD
To test DLD on VMs, download a latest release of Ubuntu Server ISO, and install it on a Virtual Machine Manager (for example, VirtualBox). Clone it to make four copies of the Ubuntu Server. Create a shared folder on the host machine and link it with all the 4 VMs. Put the jar file inside the shared folder and issue the following command in the terminal from each of the VMs to launch the Dynamo nodes:
``
