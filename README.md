# SMQD MQTT Bridge

[![License](http://img.shields.io/:license-apache-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0.html)
[![Build Status](https://travis-ci.org/smqd/smqd-bridge-mqtt.svg?branch=develop)](https://travis-ci.org/smqd/smqd-bridge-mqtt)
[![Sonatype Nexus (Releases)](https://img.shields.io/nexus/r/https/oss.sonatype.org/com.thing2x/smqd-bridge-mqtt_2.12.svg)](https://oss.sonatype.org/content/groups/public/com/thing2x/smqd-bridge-mqtt_2.12/)
[![Sonatype Nexus (Snapshots)](https://img.shields.io/nexus/s/https/oss.sonatype.org/com.thing2x/smqd-bridge-mqtt_2.12.svg)](https://oss.sonatype.org/content/groups/public/com/thing2x/smqd-bridge-mqtt_2.12/)

## Usage

```scala
    libraryDependencies += "com.thing2x.smqd" %% "smqd-bridge-mqtt" % "x.y.z"
```

## Configuration

```
smqd {
  bridge {
    drivers = [
      {
        name = mqtt_br
        entry.plugin = com.thing2x.smqd.bridge.MqttBridgeDriver
        config {
            destination = "127.0.0.1:1883"
            client-id = bridge_client
            user = userid
            password = userpassword
            queue = 20
            overflow-strategy = drop-buffer
            keep-alive-interval = 10s
            bridges = [
                {
                  topic = "sensor/+/temperature"
                  qos = 0
                },
                {
                  topic = "sensor/+/humidity"
                  prefix = "bridged/data/"
                  suffix = "/json"
                  qos = 1
                }
              ] 
        }
      }
    ]
  }
}

```

### driver settings

- _destination_

    desitnation mqtt server's address and port

- _client-id_

    client-id used to connect for destination mqtt server (default: MqttBridge-${driver's name})

- _user_ and _password_

    login user name and password. one or both of them can be omitted from config as Mqtt Specification

- _keep-alive-interval_

    mqtt keepAliveInterval setting. default is 60 seconds

    smqd sends PINGREQ to destination mqtt server for every interval of this value

- _queue_

    size of driver's queue, default is 10

- _overflow-strategy_

    queue overflow strategy

    - drop-head
    - drop-tail
    - drop-buffer (default)
    - drop-new
    - backpressure
    - fail


### bridge settings

- _topic_

    topic filter that the bridge will subscribe

- _prefix_ & _suffix_

    if those settings are not defined, received messages will be published to destination mqtt server with same topic name.

    if exists, _prefix_ and _suffix_ will be appended at the head and tail of original topic path before published

- _qos_

    0, 1, 2  (default: 0)

