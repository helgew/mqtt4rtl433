# mqtt4rtl433
A simple MQTT java application for rtl433 data.

This application implements a thin wrapper around the asynchronous MQTT client from the 
[Eclipse Paho MQTT Java API](https://github.com/eclipse/paho.mqtt.java).

It takes JSON output from the [rtl_433](https://github.com/merbanan/rtl_433) device decoder 
and publishes it to an MQTT broker.

### Notes
* The publisher generates subtopics automatically based on the device model.

* The client strives to deliver messages reliably even across network and client restarts by using a file-based 
persistence mechanism. The default connection mode disables 'clean sessions'.

* Currently, only the `mqtt://` protocol is supported (no TLS support).

## Building
Building the executable jar file should be as simple as `mvn clean install`

## Running
The executable jar file can be run with as a simple pipe in conjunction with 
[rtl_433](https://github.com/merbanan/rtl_433):

`rtl_433 -F json | java -jar mqtt4rtl433-app.jar`

## Command Line Options
`java -jar mqtt4rtl433-app.jar --help`

```
Option                Description                                                                                
------                -----------                                                                                
-D, --dir             directory for inflight storage (default: /tmp)
-L                    specify connection URL in the form of tcp://[username[:password]@]host[:port]/topic        
-P, --password        provide a password                                                                         
-c, --clean           connect using a 'clean session'.                                                           
-d, --debug           enable debug messages.                                                                     
-f, --file            read messages from file instead of STDIN                                                   
-h, --host            mqtt host to connect to. (default: m2m.eclipse.org)                                        
--help                print this help text and quit                                                              
-i, --id              id to use for this client. (default: mqtt4rtl433_client-<UUID>)                            
-l, --logfile         log to this file. (default: application.log)                
-p, --port <Integer>  network port to connect to. (default: 1883)                                                
-q <Integer>          quality of service level to use for all messages. (default: 2)                             
--quiet               do not print any messages to the console except for errors.                                
-t, --topic           mqtt topic to publish below (default: rtl433)                                              
-u, --user            provide a username                                                                         
```
