
package com.fishuyo.seer
package kodama

import graphics._
import io._
import dynamic._ 

object Main extends SeerApp {

  val live = new SeerScriptLoader("live.scala")
  var rdNode:RDNode = null

  override def init(){
  	loadShaders()

    rdNode = new RDNode
    // rdNode.camera = Camera //weirdness
  	SceneGraph.roots += rdNode
  }

  override def draw(){}
  override def animate(dt:Float){}


  def loadShaders(){
  	var s = Shader.load("test", File("shaders/basic.vert"), File("shaders/basic.frag"))
  	s.monitor
    s = Shader.load("rd", File("shaders/basic.vert"), File("shaders/rd.frag"))
    s.monitor
    s = Shader.load("terrain", File("shaders/terrain.vert"), File("shaders/terrain.frag"))
    s.monitor
  	s = Shader.load("colorize", File("shaders/basic.vert"), File("shaders/colorize.frag"))
    s.uniforms("color1") = RGBA(0,0,0,0)
    s.uniforms("color2") = RGBA(1,0,0,.3f)
    s.uniforms("color3") = RGBA(0,0,1,.5f)
    s.uniforms("color4") = RGBA(0,1,0,.7f)
    s.uniforms("color5") = RGBA(1,1,0,1)
  	s.monitor
  }
}