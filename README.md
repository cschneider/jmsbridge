# Transparent Protocol bridge between HTTP and JMS

The bridge is packaged as a war file dand can be installed into java webservers like tomcat. Once installed you only need to configure your jms server.
An ideal use case for the bridge is if you have a SOAP over JMS based service and want to access it from a platform that can not talk
to jms like .NET. Of course you can also access normal queues that do not use SOAP at all. 

## License

Apache License 2.0

## Why to use this project?
I can also bridge from HTTP to JMS by using servicemix or camel. What is different here?

The jmsbridge needs only one simple setup for the jms server. Then you can access any queue on the server and any service behind them. Other solutions normally require a setup for each service you want to access.

## build

    mvn clean install
    
## Authentication

The bridge uses http basic authentication the http client provides a usename and password. The bridge then opens a
connection to the jms server with these credentials. So the bridge fully supports access control on queues and topics.

## Installation

* Load and start tomcat server. No setup necessary
* Load and start ActiveMQ server. No setup necessary
* Copy the jmsbridge.war into the webapps directory
* The war file will be unpacked to the directory jmsbridge
* If you want to use a different jms provider than activemq then you have to copy the jms jar files for your provider into
the jmsbridge/lib directory
* Edit the file jmsbridge/META-INF/context.xml. Set the correct parameters for your jms server. The default setup works for a local ActiveMQ server
* Restart your tomcat to active the configuration

## How to access the bridge

Access the bridge on http://localhost:8080/jmsbridge/<queuename>

* To send a message to the queue you send a POST request to the url that represents your queue
* The bridge will require a username / password for the page. Simply use your jms username/password. If you use a
default activemq installation that is not password protected then you can send any username/password
* The HTTP connection will then wait for a reply on a temporary queue. The contents of the reply will be returned
on the HTTP connection

## Complete test setup for the bridge

* Do the steps from installation
* Start a jms based webservice (like the cxfwsdfirstexample from cxf)
* Download and start SOAPUI
* Use SOAPUI to access the webservice on the bridge. With the correct url that represents the service queue

## Ideas for improvement

* Improved error handling
* Browsing of existing queues and topics
* Download of wsdls for the offered services
* Interactive mode. Access a queue with HTTP GET. A form could be offered where you could enter a request and submit the
request to the queue. The reponse could also be displayed
