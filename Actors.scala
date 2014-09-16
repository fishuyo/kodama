
package com.fishuyo.seer
package kodama.actor

import akka.actor._
import akka.actor.Props
import com.typesafe.config.ConfigFactory
// import akka.event.Logging
// import akka.actor.ActorSystem

import collection.mutable.ListBuffer


object ActorManager {

  val config_floor = ConfigFactory.parseString("""
    akka {
      actor {
        provider = "akka.remote.RemoteActorRefProvider"
      }
      remote {
        enabled-transports = ["akka.remote.netty.udp"]
        netty.udp {
          send-buffer-size = 20MiB
          receive-buffer-size = 20MiB
          maximum-frame-size = 10MiB

          hostname = "192.168.0.109"
          port = 2552
        }
        compression-scheme = "zlib"
        zlib-compression-level = 1
     }
    }
  """)

  val config_wall = ConfigFactory.parseString("""
    akka {
      actor {
        provider = "akka.remote.RemoteActorRefProvider"
      }
      remote {
        enabled-transports = ["akka.remote.netty.udp"]
        netty.udp {
          send-buffer-size = 20MiB
          receive-buffer-size = 20MiB
          maximum-frame-size = 10MiB
          
          hostname = "192.168.0.101"
          port = 2552
        }
        compression-scheme = "zlib"
        zlib-compression-level = 1
     }
    }
  """)

  // load the normal config stack (system props, then application.conf, then reference.conf)
  val regularConfig = ConfigFactory.load();

  lazy val system_floor = ActorSystem("seer", ConfigFactory.load(ActorManager.config_floor))
  lazy val system_wall = ActorSystem("seer", ConfigFactory.load(ActorManager.config_wall))
}



