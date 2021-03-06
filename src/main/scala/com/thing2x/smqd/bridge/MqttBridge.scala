// Copyright 2018 UANGEL
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.thing2x.smqd.bridge

import akka.actor.ActorSystem
import akka.stream.{Materializer, QueueOfferResult}
import akka.stream.alpakka.mqtt.scaladsl.MqttSink
import akka.stream.alpakka.mqtt.{MqttConnectionSettings, MqttMessage, MqttQoS}
import akka.stream.scaladsl._
import akka.util.ByteString
import com.thing2x.smqd._
import com.thing2x.smqd.plugin._
import com.thing2x.smqd.util.ConfigUtil._
import com.typesafe.config.Config
import com.typesafe.scalalogging.StrictLogging
import io.netty.buffer.ByteBuf
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.{Failure, Success}

// 2018. 6. 22. - Created by Kwon, Yeong Eon

class MqttBridgeDriver(name: String, smqdInstance: Smqd, config: Config) extends BridgeDriver(name, smqdInstance, config) with StrictLogging {

  private var source: Option[SourceQueueWithComplete[MqttMessage]] = None

  override protected def createBridge(bridgeConf: Config, index: Long): Bridge = {
    val topic = bridgeConf.getString("topic")
    val filterPath = FilterPath(topic)
    val prefix = bridgeConf.getOptionString("prefix")
    val suffix = bridgeConf.getOptionString("suffix")
    val qos = if (bridgeConf.hasPath("qos")){
      bridgeConf.getInt("qos") match {
        case 1 => MqttQoS.atLeastOnce
        case 2 => MqttQoS.exactlyOnce
        case _ => MqttQoS.atMostOnce
      }
    }
    else {
      MqttQoS.atMostOnce
    }

    new MqttBridge(this, index, filterPath, qos,
      if (prefix.isDefined && prefix.get.length > 0) prefix else None,
      if (suffix.isDefined && suffix.get.length > 0) suffix else None)
  }

  override protected def connect(): Unit = {

    implicit val ec: ExecutionContext = smqdInstance.Implicit.gloablDispatcher
    implicit val actorSystem: ActorSystem = smqdInstance.Implicit.system
    implicit val materializer: Materializer = smqdInstance.Implicit.materializer
    
    val queueSize: Int = config.getOptionInt("queue").getOrElse(20)
    val destination = config.getString("destination")
    val clientId = config.getOptionString("client-id").getOrElse(s"MqttBridge-$name")

    val user = config.getOptionString("user")
    val password = config.getOptionString("password")
    val auth = if (user.isDefined || password.isDefined) {
      Some((if(user.isDefined) user.get else null, if(password.isDefined) password.get else null))
    }
    else {
      None
    }

    val overflowStrategy = config.getOverflowStrategy("overflow-strategy")
    val keepAliveInterval = config.getOptionDuration("keep-alive-interval").getOrElse(60.seconds)
    val connectionSettings = MqttConnectionSettings("tcp://"+destination, clientId, new MemoryPersistence,
      auth = auth,
      keepAliveInterval = keepAliveInterval)
      .withAutomaticReconnect(true)
      .withCleanSession(true)

    logger.debug(s"MqttBridgeDriver($name) overflow-strategy: $overflowStrategy")
    logger.debug(s"MqttBridgeDriver($name) keep-alive-interval: ${connectionSettings.keepAliveInterval.toSeconds} seconds")
    // Materialization with SourceQueue
    //   refer = https://stackoverflow.com/questions/30964824/how-to-create-a-source-that-can-receive-elements-later-via-a-method-call
    val queue = Source.queue[MqttMessage](queueSize, overflowStrategy)
//      .map{ m =>
//        logger.trace(s"MqttBridgeDriver($name) publish qos: ${m.qos.getOrElse(MqttQoS.AtMostOnce).byteValue}, topic: ${m.topic}, payload: ${m.payload.length} bytes")
//        m
//      }
      .to(MqttSink(connectionSettings, MqttQoS.atMostOnce))
      .run()

    // when Source complete or remote server close connection
    queue.watchCompletion.onComplete {
      case Success(d) =>
        // for  reconnect test
        //source = None
        logger.debug(s"MqttBridgeDriver($name) connection lost, isClosed = $isClosed")
      case Failure(ex) =>
        // for  reconnect test
        //source = None
        logger.debug(s"MqttBridgeDriver($name) connection lost, , isClosed = $isClosed :", ex)
    }

    source = Some(queue)
  }

  def publish(bridge: MqttBridge, topicPath: TopicPath, msg: Any): Unit = {
    source match {
      case Some(queue) if !isClosed =>
        val byteString = msg match {
          case bb: ByteBuf =>
            val buf = new Array[Byte](bb.readableBytes)
            bb.getBytes(0, buf)
            ByteString(buf)
          case str: String =>
            ByteString(str)
          case buf: Array[Byte] =>
            ByteString(buf)
          case bs: ByteString =>
            bs
          case _ =>
            val err = s"MqttBridgeDriver($name) can not handle a meesage type of ${msg.getClass.getName}"
            logger.error(err)
            throw new RuntimeException(err)
        }

        val topic = bridge.prefix.getOrElse("") + topicPath.toString + bridge.suffix.getOrElse("")
        val qos = bridge.qos

        implicit val ec: ExecutionContext = smqdInstance.Implicit.gloablDispatcher

        queue.offer(new MqttMessage(topic, byteString, Some(qos))).map {
          case QueueOfferResult.Enqueued =>
            logger.trace(s"MqttBridgeDriver($name)   offer qos: ${qos.byteValue}, topic: $topic, payload: ${byteString.length} bytes")
          case QueueOfferResult.Dropped =>
            logger.trace(s"MqttBridgeDriver($name)    drop qos: ${qos.byteValue}, topic: $topic, payload: ${byteString.length} bytes")
          case QueueOfferResult.QueueClosed =>
            logger.trace(s"MqttBridgeDriver($name)  closed qos: ${qos.byteValue}, topic: $topic, payload: ${byteString.length} bytes")
          case QueueOfferResult.Failure(ex) =>
            logger.trace(s"MqttBridgeDriver($name)  failed qos: ${qos.byteValue}, topic: $topic, payload: ${byteString.length} bytes", ex)
        }

      case _ =>
        logger.warn(s"MqttBridgeDriver($name) is not conntected, messages for '${topicPath.toString}' will be discarded")
    }
  }

  override def disconnect(): Unit = {
    logger.info(s"MqttBridgeDriver($name) disconnect")
    if (source.isDefined)
      source.get.complete()
    source = None
  }
}

class MqttBridge(driver: MqttBridgeDriver, index: Long, filterPath: FilterPath,
                 val qos: MqttQoS, val prefix: Option[String], val suffix: Option[String])
  extends AbstractBridge(driver, index, filterPath) with StrictLogging {

  override def bridge(topic: TopicPath, msg: Any): Unit = driver.publish( this, topic, msg )
}
