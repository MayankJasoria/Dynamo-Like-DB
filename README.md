# Dynamo-Like-DB

Dynamo-Like-DB is a distributed object based storage service inspired from [Amazon DynamoDB](https://www.allthingsdistributed.com/files/amazon-dynamo-sosp2007.pdf). 

DLD's features include:
* Gossip protocol, for node disovery and failure detection.
* Vector clock based versioning system for objects.
* Consistent Hashing 

DLD was tested on a system of 4 VMs.

### Compilation
To compile the project, pull the project using and compile the maven project. Compiling the maven project will produce a jar file which can be exceuted as a Dynamo Node.

### Testing DLD
To test DLD on VMs, download a latest release of Ubuntu Server ISO, and install it on a Virtual Machine Manager (for example, VirtualBox). Clone it to make four copies of the Ubuntu Server. Create a shared folder on the host machine and link it with all the 4 VMs. Put the jar file inside the shared folder. The application (JAR) must be started on each system (connected to each other via a network) by passing the following command-line arguments:
 * (String) Node name, must be one word
 * (String) Node IP address:port (example 172.17.73.158:9350)
 * (Integer) Gossip rate (example 2000)
 * (Integer) TTL (example 20000)
 * (Boolean) is this node an API Gateway? true/false
 * (String) comma-separated list of IP:ports to attempt establishing communication with at the start for entering the network (example 172.17.23.51:9350,172.17.23.56:9350,172.17.23.60:9350). {This parameter is optional}

Example: `java <name-of-jar>.jar node1 172.17.73.158:9350 2000 20000 false 172.17.23.51:9350,172.17.23.56:9350,172.17.23.60:9350`
 The API gateway can be started by deploying the WAR file on a server and making an apprioriate GET request to http://<IP:PORT>/dynamoServer/db/start. Details explained ahead

 ### APIs

 - base URL: http://<IP:PORT>/dynamoProject/db

 - /start: GET request, starts the server
 - /shutdown: GET request, stops the server
 - /bucket:
    * POST (JSON, parameters - bucketName:String) - Creates a bucket in the database
    * DELETE (JSON, parameters - bucketName:String) - Deletes an existing bucket along with all its records from the database

 - /{bucketName}:
    * POST (JSON, parameters - key:String, value:String) - adds a record to bucket {bucketName} in the database
    * PUT (JSON, parameters - key:String, value:String) - updates a record in bucket {bucketName} in the database

 - /{bucketName}/{objectName}:
    * GET - returns JSON containing values and vector clocks of all relevant nodes (based on hashing) from which read of object {objectName} in bucket {bucketName} was successful
    * DELETE - deletes the object {objectName} from bucket {bucketName} from all relavant nodes, based on hashing
