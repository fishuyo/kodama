
import com.fishuyo.seer._
import graphics._
import dynamic._
import maths._
import spatial._
import io._
import util._
import particle._
import trees._

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20

import scala.collection.mutable.ListBuffer

import concurrent.duration._
import de.sciss.osc._


Scene.alpha = .3
SceneGraph.root.depth = false

Camera.nav.pos.set(0,1,4)
Camera.nav.quat.set(1,0,0,0)

object Script extends SeerScript {

	implicit def f2i(f:Float) = f.toInt

  var dirty = true
  var update = false

  val mesh = Plane.generateMesh(10,10,50,50,Quat.up)
  mesh.primitive = Lines
  val model = Model(mesh)
  model.material = Material.specular
  model.material.color = RGB(0,0.5,0.7)

  val fabric = new SpringMesh(mesh,1.f)
  fabric.pins += AbsoluteConstraint(fabric.particles(0), fabric.particles(0).position)
  fabric.pins += AbsoluteConstraint(fabric.particles(50), fabric.particles(50).position)
  // fabric.pins += AbsoluteConstraint(fabric.particles(0), fabric.particles(0).position)
  fabric.pins += AbsoluteConstraint(fabric.particles.last, fabric.particles.last.position)
  Gravity.set(0,0,0)

  val sun = Sphere().scale(0.1f)

  var blend = GL20.GL_ONE_MINUS_SRC_ALPHA

  Schedule.clear
  val cycle = Schedule.cycle(200 seconds){ case t =>
  	val y = 10.f*math.cos(2*Pi*t)
  	val z = 10.f*math.sin(2*Pi*t)
  	Shader.lightPosition.set(Shader.lightPosition.x,y,z)
  	sun.pose.pos.set(Shader.lightPosition)
  }
  val cycle2 = Schedule.cycle(1 hour){ case t =>
  	val x = 2.f*math.cos(2*Pi*t)
  	Shader.lightPosition.x = x
  	sun.pose.pos.set(Shader.lightPosition)
  }

	val cursor = Sphere().scale(0.05)
	var lpos = Vec2()
	var vel = Vec2()

	// val part = fabric.particles(fabric.particles.length/2+25)
	// val tree = new Tree()
	// tree.setAnimate(true)
	// tree.setReseed(true)
	// tree.setDepth(8)
	// tree.branch(8)

	override def onLoad(){
	}

	override def draw(){
		FPS.print
		Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, blend)

		model.draw
		sun.draw
		cursor.draw
		// tree.draw
	}

  override def preUnload(){
    send.disconnect
    recv.disconnect
  }
	override def onUnload(){
	}

  
  override def animate(dt:Float){
  	val x = Mouse.xy().x
  	// cycle.speed = x*100
  	// cycle2.speed = x*100

  	if( Mouse.status() == "drag"){
			vel = (Mouse.xy() - lpos)/dt
			// println(vel)
			// s.applyForce( Vec3(vel.x,vel.y,0)*10.f)
			val r = Camera.ray(Mouse.x()*Window.width, (1.f-Mouse.y()) * Window.height)
			fabric.particles.foreach( (p) => {
				val t = r.intersectSphere(p.position, 0.25f)
				if(t.isDefined){
					// val p = r(t.get)
					p.applyForce(Vec3(vel.x,vel.y,0)*150.f)
					cursor.pose.pos.set(r(t.get))
				}
			})
		}
		lpos = Mouse.xy()

    val r = Ray(B.pos+Vec3(0,100,0), Vec3(0,-1,0) )
    fabric.particles.foreach( (p) => {
      val t = r.intersectSphere(p.position, 0.25f)
      if(t.isDefined){
        p.applyForce(Bvel*150.f)
        cursor.pose.pos.set(r(t.get))
      }
    })


		fabric.animate(speed.abs*2.f*dt)
		// tree.root.pose.pos.set(part.position)
		// tree.animate(dt)
  }


  // input events
  Keyboard.clear()
  Keyboard.use()
  Keyboard.bind("g", ()=>{
  		// Script.mesh.clear()
			// Plane.generateMesh(Script.mesh,100,100,100,100,Quat.up())
			Script.mesh.vertices.foreach{ case v => v.set(v.x,v.y+Random.float(-1,1).apply()*0.02*(v.x).abs,v.z) }
			Script.mesh.recalculateNormals()
			Script.mesh.update()
  })
  Keyboard.bind("1", ()=>{Script.mesh.primitive = Triangles})
  Keyboard.bind("2", ()=>{Script.mesh.primitive = Lines})
  Keyboard.bind("3", ()=>{Script.blend = GL20.GL_ONE_MINUS_SRC_ALPHA})
  Keyboard.bind("4", ()=>{Script.blend = GL20.GL_ONE})

  Mouse.clear
  Mouse.use

  var B = Pose()
  var Blast = Pose()
  var Bvel = Vec3()
  var speed = 1.f
  VRPN.clear
  VRPN.bind("gnarl", (p)=>{
    Blast = B
    B = B.lerp(p,0.1f)
    Bvel = p.pos - Blast.pos
    //speed = B.quat.toEulerVec.y.toDegrees / 90.f
    // speed = (B.quat.toZ dot Vec3(-1,0,0))
    // send.send("/gnarl/speed",speed)
    // Script.cycle.speed = speed*20
    // Script.cycle2.speed = speed*20

  })

  val send = new OSCSend
  send.connect("localhost", 8010)
  val recv = new OSCRecv
  recv.listen(8011)
  recv.bindp {
    case Message("/gnarl/speed", s:Float) => 
      speed = s
      Script.cycle.speed = speed*20
      Script.cycle2.speed = speed*20
    case _ => ()
  }

}


// must return this from script
Script