

import com.fishuyo.seer._
import kodama._
import graphics._
import spatial._
import maths._
import dynamic._
import io._
import util._

import concurrent.duration._

import io._
import openni._

import com.badlogic.gdx.graphics.{Texture => GdxTexture}
import com.badlogic.gdx.graphics.Pixmap

import org.openni._
import java.nio.ShortBuffer
import java.nio.ByteBuffer


Scene.alpha = 1.
SceneGraph.root.depth = true


object Script extends SeerScript {

	Mouse.clear
	Mouse.use

  var rdNode:RDNode = null
  var inited = false

  val s = Plane().scale(1,-1,1).translate(1.5,0,0)
  s.material = Material.basic
  s.material.textureMix = 1.f
  s.shader = "colorize"

  OpenNI.connect()
	val context = OpenNI.context

	println(OpenNI.depthMD.getFullXRes)


	val quad1 = Plane().scale(1,-480.f/640.f,1).translate(-1,0,0)
	val quad2 = Plane().scale(1,-480.f/640.f,1).translate(1,0,0)
  val dpix = new Pixmap(640,480, Pixmap.Format.RGB888)
  val vpix = new Pixmap(640,480, Pixmap.Format.RGB888)
  var tex1:GdxTexture = _
  var tex2:GdxTexture = _



  override def init(){
  	loadShaders()
    rdNode = new RDNode
  	SceneGraph.roots += rdNode

		tex1 = new GdxTexture(dpix)
		tex2 = new GdxTexture(vpix)
		quad1.material = Material.basic
		quad1.material.texture = Some(tex1)
		quad1.material.textureMix = 1.f
		quad2.material = Material.basic
		quad2.material.texture = Some(tex2)
		quad2.material.textureMix = 1.f
  	inited = true
  }

	override def draw(){
		FPS.print

		OpenNI.updateDepth()

		quad1.draw
		// quad2.draw

		rdNode.bindBuffer(0)
		tex1.bind(1)
		s.draw
		for(i <- 0 until 10) rdNode.render
	}
	override def animate(dt:Float){
		if(!inited) init()

		Shader("rd")
		var s = Shader.shader.get
		s.uniforms("brush") = Mouse.xy()
		s.uniforms("width") = Window.width.toFloat
		s.uniforms("height") = Window.height.toFloat
		s.uniforms("feed") = 0.037 //62
		s.uniforms("kill") = 0.06 //093
		s.uniforms("dt") = dt
		s.uniforms("u_texture1") = 1

		Shader("colorize")
		s = Shader.shader.get
		s.uniforms("color1") = RGBA(0,0,0,0)
    s.uniforms("color2") = RGBA(1,0,0,.3f)
    s.uniforms("color3") = RGBA(0,0,1,.4f)
    s.uniforms("color4") = RGBA(0,1,1,.5f)
    s.uniforms("color5") = RGBA(0,0,0,.6f)

  	val bb = dpix.getPixels
		bb.put(OpenNI.imgbytes)
		bb.rewind
		tex1.draw(dpix,0,0)
	}

	def loadShaders(){
    Shader.load("rd", File("shaders/basic.vert"), File("shaders/rd_img.frag")).monitor
  	Shader.load("colorize", File("shaders/basic.vert"), File("shaders/colorize.frag")).monitor
  }

  override def onUnload(){
  	SceneGraph.roots -= rdNode
  	// OpenNI.disconnect
  }

}




Script