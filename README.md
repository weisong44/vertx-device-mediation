vertx-device-mediation
======================

This is a project to build a device mediation using vert.x

 - Websocket is used as transport
 - Connection is always initiated by devices
 - Upon connection, a simple auth exchange using challenge is used
 - Once authenticated, both sides are able to send data
 - Data transfer initiated by devices is modeled as notification, devices currently send statistics every second
 - Data transfer initiated by the server is modeled as request/response pairs, the server queries for device health every second
 
TODO
 - Cluster
 - H/A

