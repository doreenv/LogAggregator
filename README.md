# LogAggregator
#### A system comprised of two components: a log aggregator service and log forwarding agents. <BR/>
#### The agents tail log files and sending to the service to recreate the files on the server side. <BR />
To run the service: <BR/>
    mvn package && java -jar target/service-0.0.1-SNAPSHOT.jar

