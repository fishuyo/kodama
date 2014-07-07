

import com.fishuyo.seer._
import graphics._
import dynamic._
import maths._
import spatial._
import io._
import util._
import particle._
// import trees._

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20

import scala.collection.mutable.ListBuffer
import scala.collection.mutable.HashMap
import concurrent.duration._
// import de.sciss.osc._

import kodama._
import openni._

import com.badlogic.gdx.graphics.{Texture => GdxTexture}
import com.badlogic.gdx.graphics.Pixmap

import org.openni._
import java.nio.ShortBuffer
import java.nio.ByteBuffer

import collection.immutable.Map

Scene.alpha = .5
SceneGraph.root.depth = false

Camera.nav.pos.set(0,1,0)
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

object SaveTheTrees {
  def save(name:String){
    var project = "tree-" + (new java.util.Date()).toLocaleString().replace(' ','-').replace(':','-') + ".json"
    if( name != "") project = name
    var path = "TreeData/" + project
    var file = Gdx.files.internal("TreeData/").file()
    file.mkdirs()
    file = Gdx.files.internal(path).file()

    var map = Map[String,Any]()
    Script.trees.zipWithIndex.foreach { case(t,i) =>
      map = map + (("t"+i) -> List(t.mx,t.my,t.mz,t.rx,t.ry,t.rz,t.visible,t.seed))
    }

    val p = new java.io.PrintWriter(file)
    p.write( scala.util.parsing.json.JSONObject(map).toString( (o) =>{
      o match {
        case o:List[Any] => s"""[${o.mkString(",")}]"""
        case s:String => s"""${'"'}${s}${'"'}"""
        case a:Any => a.toString()  
      }
    }))
    p.close
  }

  def load(name:String){
    val path = "TreeData/" + name
    val file = Gdx.files.internal(path).file()

    val sfile = scala.io.Source.fromFile(file)
    val json_string = sfile.getLines.mkString
    sfile.close

    val parsed = scala.util.parsing.json.JSON.parseFull(json_string)
    if( parsed.isEmpty ){
      println(s"failed to parse: $path")
      return
    }

    Script.trees.zipWithIndex.foreach { case(t,i) =>
      val map = parsed.get.asInstanceOf[Map[String,Any]]
      val l = map("t"+i).asInstanceOf[List[Double]]

      t.mx = l(0).toFloat
      t.my = l(1).toFloat
      t.mz = l(2).toFloat
      t.rx = l(3).toFloat
      t.ry = l(4).toFloat
      t.rz = l(5).toFloat
      t.visible = l(6).toInt
      t.seed = l(7).toLong
      t.root.pose.pos.set(t.mx,t.my,0)
      t.update(t.mz,t.rx,t.ry,t.rz)
    }
  }
}

object Script extends SeerScript {

	implicit def f2i(f:Float) = f.toInt

  DesktopApp.unsafeAddDir("/Users/fishuyo/kodama/lib")

  var dirty = true
  var update = false

  var moving_fabric = true
  var moving_trees = true

  val n = 30
  val mesh = Plane.generateMesh(10,10,n,n,Quat.up)
  mesh.primitive = Lines
  val model = Model(mesh)
  model.material = Material.specular
  model.material.color = RGB(0,0.5,0.7)

  mesh.vertices.foreach{ case v => v.set(v.x,v.y+Random.float(-1,1).apply()*0.05*(v.x).abs,v.z) }

  val fabric = new SpringMesh(mesh,1.f)
  fabric.pins += AbsoluteConstraint(fabric.particles(0), fabric.particles(0).position)
  fabric.pins += AbsoluteConstraint(fabric.particles(n), fabric.particles(n).position)
  // fabric.pins += AbsoluteConstraint(fabric.particles(0), fabric.particles(0).position)
  fabric.pins += AbsoluteConstraint(fabric.particles.last, fabric.particles.last.position)
  Gravity.set(0,0,0)

  val sun = Sphere().scale(0.1f)

  var blend = GL20.GL_ONE

  var time = 0.f

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

  val joints_pulse = Schedule.cycle(4 seconds){ case t =>
    try {
      Shader("joint")
      var sh = Shader.shader.get
      sh.uniforms("time") = t*2*Pi
    } catch { case e:Exception => ()}
  }
  // cycle.speed = 200.f
  // cycle2.speed = 200.f

  val colors = RGB(1,1,1) :: RGB(0.7,0.,0.5) :: RGB(0.,.7,.5) :: RGB(.5,.5,.7) :: RGB(0.5,.7,0) :: RGB(0,1,1) :: RGB(1,0,1) :: RGB(1,1,1) :: List()

	val cursors = HashMap[Int,(Model,Model)]()
  for( i <- 1 to 4){
    val m1 = Plane().scale(.1)
    val m2 = Plane().scale(.1)
    val m = Material.basic
    m.color = colors(i)
    m1.material = m
    m1.shader = "cursor"
    m2.material = m
    m2.shader = "cursor"
    cursors(i) = (m1,m2)
  }
	
  var lpos = Vec2()
	var vel = Vec2()

  println(com.badlogic.gdx.Gdx.graphics.getBufferFormat.samples)

	// val part = fabric.particles(fabric.particles.length/2+25)
	val tree = new ATree()
	val trees = List(new ATree(8), new ATree(8))


  OpenNI.connect()
	val context = OpenNI.context

	println(OpenNI.depthMD.getFullXRes)

  val dpix = new Pixmap(640,480, Pixmap.Format.RGBA8888)
  var tex1:GdxTexture = _


  var inited = false
  var feedback:RenderNode = null

  def loadShaders(){
    Shader.load("joint", File("/Users/fishuyo/kodama/shaders/basic.vert"), File("/Users/fishuyo/kodama/shaders/skel.frag")).monitor
    Shader.load("cursor", File("/Users/fishuyo/kodama/shaders/basic.vert"), File("/Users/fishuyo/kodama/shaders/cursor.frag")).monitor
  }

  override def init(){
    loadShaders()
    TreeNode.model.material = Material.specular //shader = "t"
  	TreeNode.model.material.color = RGB(0,0.5,0.7)

    for( i <- 1 to 4 ){ 
      OpenNI.skeletons(i) = new TriangleMan(i)
      OpenNI.skeletons(i).setColor(colors(i))
    }

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
		// model.material.texture = Some(tex1)
		// model.material.textureMix = 0.3f

  }

	override def draw(){
		FPS.print
		Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, blend)

		OpenNI.updateDepth()

    mesh.primitive = Triangles
		model.draw
		sun.draw
		
		trees.foreach(_.draw)

    OpenNI.skeletons.values.foreach(_.draw)
		OpenNI.skeletons.values.foreach(_.drawJoints)

    cursors.foreach{ case (id,(l,r)) => 
      if(OpenNI.tracking.isDefinedAt(id) && OpenNI.tracking(id)){
        Shader("cursor")
        var sh = Shader.shader.get
        sh.uniforms("color") = RGB(1,0,0) //l.material.color
        l.draw; r.draw
      }
    }

	}

  override def preUnload(){
    send.disconnect
    recv.disconnect
  }
  
  override def animate(dt:Float){
    if(!inited) init()

    time += dt
    // val bb = dpix.getPixels
		// bb.put(OpenNI.imgbytes)
		// bb.rewind
		// tex1.draw(dpix,0,0)

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
					// cursor.pose.pos.set(r(t.get))
				}
			})
		}
		lpos = Mouse.xy()

		OpenNI.tracking.foreach { 
			case (id,b) if b =>
    		OpenNI.getJoints(id)
    		OpenNI.skeletons(id).animate(dt)

        val s = OpenNI.skeletons(id)

        if(moving_fabric && id <= 4){
          val o1 = s.joints("lhand")
          val o2 = s.joints("rhand")
          val r1 = Ray(o1, s.bones(1).quat.toZ )
          val r2 = Ray(o2, s.bones(3).quat.toZ )
          fabric.particles.foreach( (p) => {
            val t1 = r1.intersectSphere(p.position, 0.25f)
            if(t1.isDefined){
              p.applyForce(s.vel("lhand")*1000.f)
              cursors(id)._1.pose.pos.lerpTo(r1(t1.get),0.01)
            }
            val t2 = r2.intersectSphere(p.position, 0.25f)
            if(t2.isDefined){
              p.applyForce(s.vel("rhand")*1000.f)
              cursors(id)._2.pose.pos.lerpTo(r2(t2.get),0.01)
            }
          })
        }

        if(moving_trees){

          val t = trees(0)
          t.visible = 1

          val dist = (s.joints("rhand") - s.joints("lhand")).magSq

          t.ry = dist   
          t.mz = (s.joints("rhand").y + s.joints("lhand").y) / 2.f
          if (t.mz < 0.08) t.mz = 0.08
          if (t.mz > 3.0) t.mz = 3.0 

          t.rz = s.joints("torso").z
          t.rx = -s.joints("torso").x


            t.update(t.mz,t.rx,t.ry,t.rz) 
        }


    	case _ => ()
  	}



		fabric.animate(speed.abs*1.f*dt)

		trees.zipWithIndex.foreach{ case (t,i) =>
			val v = (n*(n-1/*-i*/)/2).toInt //n*(n+1)/2
			t.root.pose.pos.set(mesh.vertices(v))

      val quat = Quat().getRotationTo(Vec3(0,0,1), mesh.normals(v) * math.pow(-1,i) )
			t.root.pose.quat.set(quat)
			t.root.restPose.quat.set(quat)
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
  Keyboard.bind("r", ()=>{video.ScreenCapture.framerate = 1.f; video.ScreenCapture.start})
  Keyboard.bind("t", ()=>{video.ScreenCapture.stop})

  Keyboard.bind("o", ()=>{SaveTheTrees.save("t.json")})
  Keyboard.bind("i", ()=>{SaveTheTrees.load("t.json")})

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
        t.scale += f(3)*0.01
        if (t.mz < 0.8) t.mz = 0.8
        else if (t.mz > 3.0) t.mz = 3.0 
        if (t.scale < 0.0) t.scale = 0.0
        else if (t.scale > 1.0) t.scale = 1.0 
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
  // recv.bindp {
  //   case Message("/gnarl/speed", s:Float) => 
  //     speed = s
  //     Script.cycle.speed = speed*20
  //     Script.cycle2.speed = speed*20
  //   case _ => ()
  // }

  // var imgwriter = new video.VideoWriter("", 640, 480, 1, 15)
  // var depthwriter = new video.VideoWriter("", 640, 480, 1, 15)
  // var screenwriter = new video.VideoWriter("", Window.width, Window.height, 1, 15)
  
  // var cap = Schedule.every(1 second){
  //   video.Video.writer ! video.Bytes(imgwriter,OpenNI.rgbbytes,640,480)
  //   video.Video.writer ! video.Bytes(depthwriter,OpenNI.imgbytes,640,480)
  // }
  // var scyc = Schedule.cycle(1 second){
  //   case t if t >= 1.f =>
  //     val bytes = com.badlogic.gdx.utils.ScreenUtils.getFrameBufferPixels(true)
  //     video.Video.writer ! video.Bytes(screenwriter,bytes,Window.width,Window.height)
  //   case _ => ()
  // }
  // val reset = Schedule.every(30 second){ // minute){
  //   cap.cancel
  //   scyc.cancel
  //   video.Video.writer ! video.Close(imgwriter)
  //   video.Video.writer ! video.Close(depthwriter)
  //   video.Video.writer ! video.Close(screenwriter)
  //   imgwriter = new video.VideoWriter("", 640, 480, 1, 15)
  //   depthwriter = new video.VideoWriter("", 640, 480, 1, 15)
  //   screenwriter = new video.VideoWriter("", Window.width, Window.height, 1, 15)

  //   cap = Schedule.every(1 second){
  //     video.Video.writer ! video.Bytes(imgwriter,OpenNI.rgbbytes,640,480)
  //     video.Video.writer ! video.Bytes(depthwriter,OpenNI.imgbytes,640,480)
  //   }
  //   scyc = Schedule.cycle(1 second){
  //     case t if t >= 1.f =>
  //       val bytes = com.badlogic.gdx.utils.ScreenUtils.getFrameBufferPixels(true)
  //       video.Video.writer ! video.Bytes(screenwriter,bytes,Window.width,Window.height)
  //     case _ => ()
  //   }
  // }

}


Script