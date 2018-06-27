# SMQD MQTT Bridge

## Usage

```scala
    libraryDependencies += "t2x.smqd" %% "smqd-bridge-mqtt" % "0.1.0"
```

## Configuration

```
smqd {
  bridge {
    drivers = [
      {
        name = mqtt_br
        class = t2x.smqd.bridge.MqttBridgeDriver
        destination = "127.0.0.1:1883"
        client-id = bridge_client
        user = userx
        password = userpassword
        queue = 20
        overflow-strategy = drop-buffer
        keep-alive-interval = 10s
      }
    ]

    bridges = [
      {
        topic = "sensor/+/temperature"
        driver = mqtt_br
        qos = 0
      },
      {
        topic = "sensor/+/humidity"
        driver = mqtt_br
        prefix = "bridged/data/"
        suffix = "/json"
        qos = 1
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

- _driver_

    should be name of http bridge driver

- _prefix_ & _suffix_

    if those settings are not defined, received messages will be published to destination mqtt server with same topic name.

    if exists, _prefix_ and _suffix_ will be appended at the head and tail of original topic path before published

- _qos_

    0, 1, 2  (default: 0)

