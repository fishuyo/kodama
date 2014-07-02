

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

import kodama._
import openni._

import com.badlogic.gdx.graphics.{Texture => GdxTexture}
import com.badlogic.gdx.graphics.Pixmap

import org.openni._
import java.nio.ShortBuffer
import java.nio.ByteBuffer


Scene.alpha = .3
SceneGraph.root.depth = false

Camera.nav.pos.set(0,1,4)
Camera.nav.quat.set(1,0,0,0)

class ATree(b:Int=8) extends Tree {
  var visible = 0
  var (mx,my,mz,rx,ry,rz) = (0.f,0.f,0.f,0.f,0.f,0.f)
  setAnimate(true)
  setReseed(true)
  setDepth(b)
  branch(b)

  def update(){ update(mz,rx,ry,rz)}

  override def draw(){ if(visible==1) super.draw() }
  override def animate(dt:Float){ if(visible==1) super.animate(dt) }
}

object Script extends SeerScript {

	implicit def f2i(f:Float) = f.toInt

  var dirty = true
  var update = false

  val n = 30
  val mesh = Plane.generateMesh(10,10,n,n,Quat.up)
  mesh.primitive = Lines
  val model = Model(mesh)
  model.material = Material.specular
  model.material.color = RGB(0,0.5,0.7)

  val fabric = new SpringMesh(mesh,1.f)
  fabric.pins += AbsoluteConstraint(fabric.particles(0), fabric.particles(0).position)
  fabric.pins += AbsoluteConstraint(fabric.particles(n), fabric.particles(n).position)
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
  // cycle.speed = 200.f
  // cycle2.speed = 200.f

	val cursor = Sphere().scale(0.05)
	var lpos = Vec2()
	var vel = Vec2()

  println(com.badlogic.gdx.Gdx.graphics.getBufferFormat.samples)

	// val part = fabric.particles(fabric.particles.length/2+25)
	val tree = new ATree()
	val trees = List(new ATree(8), new ATree(8))


  OpenNI.connect()
	val context = OpenNI.context

	println(OpenNI.depthMD.getFullXRes)

  val dpix = new Pixmap(640,480, Pixmap.Format.RGB888)
  var tex1:GdxTexture = _


  var inited = false
  var feedback:RenderNode = null
  override def init(){
    // loadShaders()
    TreeNode.model.material = Material.specular //shader = "t"
  	TreeNode.model.material.color = RGB(0,0.5,0.7)

    inited = true

    SceneGraph.root.outputs.clear
    ScreenNode.inputs.clear

    feedback = new RenderNode
    feedback.shader = "composite"
    feedback.clear = false
    feedback.scene.push(Plane())
    SceneGraph.root.outputTo(feedback)
    feedback.outputTo(feedback)
    feedback.outputTo(ScreenNode)

		tex1 = new GdxTexture(dpix)
		model.material.texture = Some(tex1)
		model.material.textureMix = 0.f

  }

	override def draw(){
		FPS.print
		Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, blend)

		OpenNI.updateDepth()

		model.draw
		sun.draw
		
    // cursor.draw

		trees.foreach(_.draw)

		OpenNI.skeletons.values.foreach(_.draw)
	}

  override def preUnload(){
    send.disconnect
    recv.disconnect
  }
  
  override def animate(dt:Float){
    if(!inited) init()

    val bb = dpix.getPixels
		bb.put(OpenNI.imgbytes)
		bb.rewind
		tex1.draw(dpix,0,0)

    Shader("composite")
    val fb = Shader.shader.get
    fb.uniforms("u_blend0") = 0.35
    fb.uniforms("u_blend1") = 0.65
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

		OpenNI.tracking.foreach { 
			case (id,b) if b =>
    		OpenNI.getJoints(id)
    		OpenNI.skeletons(id).animate(dt)
    	case _ => ()
  	}

    val r = Ray(B.pos+Vec3(0,100,0), Vec3(0,-1,0) )
    fabric.particles.foreach( (p) => {
      val t = r.intersectSphere(p.position, 0.25f)
      if(t.isDefined){
        p.applyForce(Bvel*150.f)
        cursor.pose.pos.set(r(t.get))
      }
    })

		fabric.animate(speed.abs*1.f*dt)

		trees.zipWithIndex.foreach{ case (t,i) =>
			val v = (n*(n-i)/2).toInt //n*(n+1)/2
			t.root.pose.pos.set(mesh.vertices(v))
			t.root.pose.quat.set(Quat().getRotationTo(Vec3(0,0,1), mesh.normals(v)) )
			t.root.restPose.quat.set(Quat().getRotationTo(Vec3(0,0,1), mesh.normals(v)) )
			t.animate(dt)
		}
  }


  // input events
  Keyboard.clear()
  Keyboard.use()
  Keyboard.bind("g", ()=>{
  		// Script.mesh.clear()
			// Plane.generateMesh(Script.mesh,100,100,100,100,Quat.up())
			mesh.vertices.foreach{ case v => v.set(v.x,v.y+Random.float(-1,1).apply()*0.02*(v.x).abs,v.z) }
			mesh.recalculateNormals()
			mesh.update()
  })
  Keyboard.bind("1", ()=>{mesh.primitive = Triangles})
  Keyboard.bind("2", ()=>{mesh.primitive = Lines})
  Keyboard.bind("3", ()=>{blend = GL20.GL_ONE_MINUS_SRC_ALPHA})
  Keyboard.bind("4", ()=>{blend = GL20.GL_ONE})

  Mouse.clear
  Mouse.use

  var idx = 0
  Trackpad.clear
  Trackpad.connect
  Trackpad.bind( (i,f) => {

    // val t = trees(idx)
    trees.foreach { case t =>
    t.visible = 1

    i match {
      case 1 =>
        val ur = Vec3(1,0,0) //Camera.nav.ur()
        val uf = Vec3(0,0,1) //Camera.nav.uf()

        trees.foreach{ case t =>
          t.root.applyForce( ur*(f(0)-0.5) * 2.0*f(4) )
          t.root.applyForce( uf*(f(1)-0.5) * -2.0*f(4) )
        }
      case 2 =>
        t.mx += f(2)*0.05  
        t.my += f(3)*0.05
      case 3 =>
        t.ry += f(2)*0.05  
        t.mz += f(3)*0.01
        if (t.mz < 0.08) t.mz = 0.08
        if (t.mz > 3.0) t.mz = 3.0 
      case 4 =>
        t.rz += f(3)*0.05
        t.rx += f(2)*0.05
      case 5 =>
      	Gravity.set(Vec3(f(0)-.5,f(1)-.5,0)*1.f)
      case _ => ()
    }

    // t.root.pose.pos.set(t.mx,t.my,0)

    if(i > 2){
      t.update(t.mz,t.rx,t.ry,t.rz) 

    }
  	}
  })

  var B = Pose()
  var Blast = Pose()
  var Bvel = Vec3()
  var speed = 1.f
  // VRPN.clear
  // VRPN.bind("gnarl", (p)=>{
  //   Blast = B
  //   B = B.lerp(p,0.1f)
  //   Bvel = p.pos - Blast.pos
  //   //speed = B.quat.toEulerVec.y.toDegrees / 90.f
  //   // speed = (B.quat.toZ dot Vec3(-1,0,0))
  //   // send.send("/gnarl/speed",speed)
  //   // Script.cycle.speed = speed*20
  //   // Script.cycle2.speed = speed*20

  // })

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


Script