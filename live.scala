

import com.fishuyo.seer._
import kodama._
import graphics._
import spatial._
import maths._
import dynamic._
import io._
import util._

import concurrent.duration._

Scene.alpha = 1.
SceneGraph.root.depth = true
Schedule.clear

class Flock {

}

class Agent extends Animatable {
	var nav = Nav()
	val b = Sphere().scale(.01,.01,.025)
	b.material = Material.specular
	b.material.color = RGB(0,1,1)	
	// for(i <- 0 until 7){ 
	// 	val w = Circle().translate(0,0,1-i/3.5f)
	// 	w.material = Material.specular
	// 	w.material.color = RGB(0,1,1)
	// 	w.scale(1.5f - math.abs(w.pose.pos.z))
	// 	b.addChild(w)
	// }

	// Schedule.cycle(1 second){ case t =>
	// 	b.children.zipWithIndex.foreach{
	// 		case (c,i) => c.scale.set(1+t+i/7.f)
	// 	}
	// }

	override def draw(){ 
		b.pose = nav
		b.draw
	}
	override def animate(dt:Float){
		nav.step(dt)
		nav.pos.wrap(Vec3(-1,-1,-1),Vec3(1,1,1))
		if(math.abs(nav.pos.z) < 0.01){
			Shader("rd")
			var s = Shader.shader.get
			s.uniforms("brush") = (nav.pos.xy+Vec2(1,1))*0.5
		}
	}
}

object Script extends SeerScript {

	Mouse.clear
	Mouse.use

  val s = Plane()
  s.material = Material.basic
  s.material.textureMix = 1.f
  s.shader = "colorize"

  val gmesh = Plane.generateMesh(10,10,250,250,Quat.up)
  val g = Model(gmesh).translate(0,-5,0)
  g.material = Material.basic
  g.material.textureMix = 0.f
  g.shader = "terrain"

  val agents = for(i <- 0 until 100) yield {
  	val a = new Agent
  	a.nav.vel = Vec3(0,0,.1)
  	a.nav.angVel = Random.vec3()
  	a
  }

	override def draw(){
		FPS.print
		// SceneGraph.roots.last.bindBuffer(0)
		s.draw
		g.draw
		agents.foreach(_.draw)
		for(i <- 0 until 10) SceneGraph.roots.last.render

	}
	override def animate(dt:Float){

		Shader("rd")
		var s = Shader.shader.get
		s.uniforms("brush") = Mouse.xy()
		s.uniforms("width") = Window.width.toFloat
		s.uniforms("height") = Window.height.toFloat
		s.uniforms("feed") = 0.037 //62
		s.uniforms("kill") = 0.06 //093
		s.uniforms("dt") = dt

		Shader("colorize")
		s = Shader.shader.get
		s.uniforms("color1") = RGBA(0,0,0,0)
    s.uniforms("color2") = RGBA(1,0,0,.3f)
    s.uniforms("color3") = RGBA(0,0,1,.4f)
    s.uniforms("color4") = RGBA(0,1,1,.5f)
    s.uniforms("color5") = RGBA(0,0,0,.6f)

    Shader("terrain")
		s = Shader.shader.get
		s.uniforms("u_texture0") = 0
		s.uniforms("color1") = RGBA(0,0,0,0)
    s.uniforms("color2") = RGBA(1,0,0,.3f)
    s.uniforms("color3") = RGBA(0,0,1,.4f)
    s.uniforms("color4") = RGBA(0,1,1,.5f)
    s.uniforms("color5") = RGBA(0,1,1,1.f)


		agents.foreach( _.animate(dt))


	}

}

Script