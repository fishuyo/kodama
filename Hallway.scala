
package com.fishuyo.seer
package kodama
package hallway

import graphics._
import io._
import dynamic._ 

object Main extends SeerApp {

  val live = new SeerScriptLoader("scripts/hallway.scala")


  override def init(){
  	loadShaders()

  }

  override def draw(){}
  override def animate(dt:Float){}


  def loadShaders(){
  	var s = Shader.load("test", File("shaders/basic.vert"), File("shaders/basic.frag"))
  	s.monitor
  	s = Shader.load("rd", File("shaders/basic.vert"), File("shaders/rd.frag"))
  	s.monitor
  }
}