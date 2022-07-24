# Getting Started

## Intro
This is an open source project that was created to provide strategies and platforms for business-focused cloud computing.
This project is based on 30 years of experience across multiple languages, platforms and architectures.

The strategy is based on experience and not theories.
It is not an all or nothing strategy.
You can use it for part or all of your applications.
It is build to interoperate with current code instead of trying to replace all existing code.

It is recommended, but not required, to use the [Architecture-First](https://tonymarius.substack.com/p/architecture-first-strategy?utm_source=url) approach to develop these items as well as items outside this project.
The project is part of a Business Oriented Architecture and called BOA.

![](docs/programming/images/boa-general-documentation-BOA-Icon.drawio.png)

It is important to understand that BOA is not an all or nothing architecture.
The BOA approach can and wants to coexist with current applications and solutions.
That is the real world and is welcomed.

The BOA approach takes advantage of the power of the Cloud-Centric environments.

![](docs/programming/images/Vision/boa-general-documentation-Vision-Support.drawio.png)

The main requirements for using the BOA platform are access to Redis for core communication and support for the messaging protocol.
As shown above, the platform is a non-opinionated layer on top of existing Cloud and On-Prem solutions.

The storing of data and messaging are also not limited to using Redis.
Redis is used for the base functionality, but the developers of custom Actor code are encouraged to use whatever storage locations that are appropriate.
For instance a Library, can be stored in an AWS S3 Bucket or a mounted file system depending on the project.

## Overview

It is recommended to read some of the documentation before installing.
An overview of BOA can be found [here](docs/programming/Overview.md).

The installation instructions are based on Linux whether as the core operating system or a structure such as Windows WSL 2.

## Installation

### Install Java 17

The application is based on Java version 17+.

https://docs.aws.amazon.com/corretto/latest/corretto-17-ug/downloads-list.html

Use the link above or an equivalent download link from a different vendor.

Add JAVA_HOME to the PATH.

```shell
$ export JAVA_HOME=/c/Users/boa/.jdks/corretto-17.0.3
$ echo $JAVA_HOME
/c/Users/boa/.jdks/corretto-17.0.3
```

### Install Git

Install Git from a trustworthy source, such as https://git-scm.com/book/en/v2/Getting-Started-Installing-Git.
There is specific version for Windows: https://git-scm.com/download/win

The successful download should include Git Bash.

![](docs/programming/images/Getting-Started/Getting-Started-Git-Bash.png)

### Clone the BOA showcase project

git clone https://github.com/architecture-first/boa-showcase.git

### Install Docker Desktop (recommended on Windows)

https://docs.docker.com/desktop/install/windows-install/

Make sure that you have the recommended RAM and disk space
After successful installation, launch Docker Desktop application and proceed to the Settings.

![](docs/programming/images/Getting-Started/Getting-Started-Docker-Desktop-Settings.png)

To get to the settings page, click the upper right corner of the windows

![](docs/programming/images/Getting-Started/Getting-Started-Docker-Desktop-K8s.png)

If you are planning to run in Kubernetes enable Kubernetes in the settings.

### Optional: Install Minikube (or equivalent)

Note: Use this option if you decide not to install Docker Desktop.

https://minikube.sigs.k8s.io/docs/start/

## Run the showcase retail application

The showcase retail application contains running code based on the platform.
It should only be used for reference.
The solution consists of the following components

- **MongoDB**
  - This NoSql database fits nicely with a BOA approach
    - Note: The BOA approach will work just as well with a relational database
- **Redis**
  - Redis is a comprehensive memory database for high performance access.
    - Many objects are stored in Redis, but most can be stored in any storage repository
      - The main items requiring Redis are the Bulletin board and the core messaging.
- **NodeJS**
  - The communication with the client occurs though a NodeJS application.
    - The microservice manages the UI static content as well as the websockets per client.
  - This component is not required for BOA applications, but shows a nice way to handle websocket communication with particular clients.
    - BOA solutions encourage asynchronous push communication rather than blocking synchronous communication.
- **Browser** (from the user's desktop)
  - index.html
    - Contains basic HTML5 and JavaScript to demonstrate code to interact with BOA Actors.
      - This logic can be replaced with any library, such as Angular, React, Vue, Svelte, etc.
    - It is intentionally not focused on look and feel
      - This is just a showcase application.
- Actors
  - **Customer**
    - Represents the User and interacts with the User's browser for communication
      - It is connected to both the Vicinity and the Client.
      - All users in this showcase application are **anonymous**
        - The temporary information is stored in the LocalStorage and SessionStorage.
  - **Merchant**
    - Responsible for the product and inventory portion of the application.
  - **Cashier**
    - Responsible for accepting payment and processing orders.
    - All payment and processing is faked for this showcase application
  - **Identity Provider**
    - Responsible for providing a valid Access Token (JwtToken).
  - **Security Guard**
    - Responsible for validating messages and reporting security incidents
  - **Vendor**
    - Delivers supplies for updating inventory
  - **Advertiser**
    - Produces advertisements based on data analysis
    - This Actor represents dynamic behavior without custom Java coding
      - It executes based on a developer defined script and interacts with a sidecar component for custom processing
- Messages
  - **business-retail**
    - The repository of ArchitectureFirst events for communication in the application.
      - There should be a custom business library per application type.
- Platform
  - **vicinity-platform**
    - The runtime that defines the Vicinity and Actors base functionality
    - This library will be kept in a Maven Repository in the near future.


You can take this application and modify it for your particular application.
Alternatively, you may choose to build your system from scratch by just using the vicinity-platform library.

### Docker Compose

This is the easiest option if you don't know Kubernetes yet.

![](docs/programming/images/Getting-Started/Getting-Started-Git-Bash-Start.png)

Open a Bash session, such as Git Bash.

![](docs/programming/images/Getting-Started/Getting-Started-Git-Bash-Deploy-Docker.png)

Change directory to the project root directory where the repository was cloned to.
Next Change the directory to the 'deploy/docker' directory

Run the following command (may need to run as sudo if there are privileges issues): 
```shell
$. ./run-retail.sh up
```
Note: if the file is not executable then run the following command.

```shell
chmod a+x run-retail.sh
```

![img.png](docs/programming/images/Getting-Started/Getting-Started-Browser-Docker-Compose-Open.png)

Go to the browser and location http://localhost:10010.

Follow "Using the showcase retail application" instructions later in this document.

### Kubernetes

This option requires Kubernetes knowledge, but is preferred.

![](docs/programming/images/Getting-Started/Getting-Started-Git-Bash-Start.png)

Open a Bash session, such as Git Bash.

![](docs/programming/images/Getting-Started/Getting-Started-Git-Bash-Deploy-K8s.png)

Change directory to the project root directory where the repository was cloned to.
Next change directory to 'deploy/k8s'.

Create a private Registry (may need to run as sudo if there are privileges issues)

```shell
docker run -d -p 5000:5000 --restart=always --name local-registry registry:2
```

Run the following command (may need to run as sudo if there are privileges issues).

```shell
$ bash run-retail-k8s.sh --build-and-install
```

Note: if the file is not executable then run the following command.

```shell
chmod a+x run-retail-k8s.sh
```
Note: At this time, it may take 1-2 minutes for the Actors to register the first time.
Wait 2 minutes then go to the browser and location http://localhost:30020.


## Using the showcase retail application

Note: If you are rebuilding the application after the initial deployment clear tokens as shown below.

Perform the following actions.
1. View products upon first load

![](docs/programming/images/Getting-Started/Getting-Started-View-Products.png)

2. Click on a product to view and then choose close

![](docs/programming/images/Getting-Started/Getting-Started-View-Product.png)

3. Register
   1. Click Login
   
 ![](docs/programming/images/Getting-Started/Getting-Started-Login.png)

   2. Click Register

 ![](docs/programming/images/Getting-Started/Getting-Started-Register.png)

Create any name.
An anonymous user is entered in the database for this user.

![](docs/programming/images/Getting-Started/Getting-Started-Post-Register.png)

4. Click on a product again

![](docs/programming/images/Getting-Started/Getting-Started-View-Product.png)

5. Add to Cart
   
![](docs/programming/images/Getting-Started/Getting-Started-Add-Product.png)

Close the Add to Cart window.

6. View shopping cart

![](docs/programming/images/Getting-Started/Getting-Started-View-Cart.png)

7. Checkout

![](docs/programming/images/Getting-Started/Getting-Started-Begin-Checkout.png)

8. View Order Confirmation

![](docs/programming/images/Getting-Started/Getting-Started-View-Order-Confirmation.png)

This is the end of a workflow through the retail showcase application.
You can close the Order Confirmation now.

## Monitoring

To see the Bulletin board, Task lists and Convos, you can install [Redis Commander](docs/programming/Tips-and-Tricks.md#install-redis-commander).

## Resetting the environment.

If you have already run through the tutorial and want to clear the environment perform the following steps.

### Remove Anonymous User ID

![](docs/programming/images/Getting-Started/Getting-Started-Clear-User-ID.png)

Delete the stored User ID.

### Clear User Token

![](docs/programming/images/Getting-Started/Getting-Started-Clear-User-Token.png)

Delete the two entries above.

## Tutorials
- [Tutorial: Hello Actor](docs/programming/Tutorials-Hello-Actor.md)
- [Tutorial: Merchant Convo](docs/programming/Tutorials-Merchant-Convo.md)

## Links

- [Overview](docs/programming/Overview.md 'Overview')
- [Concepts](docs/programming/Concepts.md)
- [Vicinity Features](docs/programming/Vicinity-Features.md 'Vicinity Features')
- [Actor Features](docs/programming/Actor-Features.md)
- [Messaging](docs/programming/Messaging.md)
- [Special Features](docs/programming/Special-Features.md)
- [Troubleshooting](docs/programming/Troubleshooting.md)
- [Tips and Tricks](docs/programming/Tips-and-Tricks.md)

## Key Contributors

- [Tony Marius](https://www.linkedin.com/in/tony-marius-05a1898/) - original author, Technical Architect
- [Paul Parrone](https://www.linkedin.com/in/paulparrone/) - author of Lush, Technical Architect
- [Bill Reeder](https://www.linkedin.com/in/bill-reeder-98869b12/?lipi=urn%3Ali%3Apage%3Ad_flagship3_people_connections%3B246w9rHIQxWeEL38zcmFMg%3D%3D) - Cloud-Computing Architect

