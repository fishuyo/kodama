

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

Scene.alpha = .4
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

  DesktopApp.unsafeAddDir(Gallery.path + "lib")

  var dirty = true
  var update = false

  var moving_fabric = false
  var moving_trees = true

  var daytime = true

  val n = 30
  val mesh = Plane.generateMesh(10,10,n,n,Quat.up)
  mesh.primitive = Lines
  val model = Model(mesh) //.translate(0,0,-5)
  model.material = Material.specular
  model.material.color = RGB(0,0.5,0.7)
  mesh.vertices.foreach{ case v => v.set(v.x,v.y+Random.float(-1,1).apply()*0.05*(v.x).abs,v.z) }
  val fabricVertices0 = mesh.vertices.clone

  val fabric = new SpringMesh(mesh,1.f)
  fabric.pins += AbsoluteConstraint(fabric.particles(0), fabric.particles(0).position)
  fabric.pins += AbsoluteConstraint(fabric.particles(n), fabric.particles(n).position)
  // fabric.pins += AbsoluteConstraint(fabric.particles(0), fabric.particles(0).position)
  fabric.pins += AbsoluteConstraint(fabric.particles.last, fabric.particles.last.position)
  Gravity.set(0,0,0)

  // var animating_fabric = true

  val sun = Plane().scale(1.f)
  sun.material = Material.basic
  sun.shader = "sun"

  val linemesh = new Mesh()
  linemesh.primitive = Lines
  linemesh.maxVertices = 100
  val linemodel = Model(linemesh)
  linemodel.shader = "bone"

  var blend = GL20.GL_ONE

  var time = 0.f

  val tree = new ATree()
  val trees = List(new ATree(8), new ATree(8))
  var treeMinHeight = 0.1f

  Schedule.clear
  val cycle = Schedule.cycle(5 minutes){ case t =>
  	val y = 10.f*math.cos(2*Pi*t)
  	val z = 10.f*math.sin(2*Pi*t)
  	Shader.lightPosition.set(Shader.lightPosition.x,y,z)
  	sun.pose.pos.set(Shader.lightPosition)

    // println(s"time: $t")
    var color = Vec3(1,1,1)
    
    if( t < .25f){ //noon to sunset
      val c = map(t,0,.25,0,1)
      color = Vec3(1,1,0).lerp(Vec3(1,0,1),c)
    }else if(t <.5f){ // sunset to midnight
      daytime = false
      moving_fabric = true
      moving_trees = true
      val c = map(t,.25,.5,0,1)
      color = Vec3(1,0,1).lerp(Vec3(1,1,1),c)
    }else if( t < .75f){ // midnight to sunrise
      val c = map(t,.5,.75,0,1)
      color = Vec3(1,1,1).lerp(Vec3(1,0.1,0),c)
      if( t > .65){
        moving_fabric = false
        moving_trees = true
        fabric.particles.foreach( (p) => {p.applyForce((p.initialPosition - p.position)*0.75) })
      }
    }else if(t < 1.f){ //sunrise to noon
      daytime = true
      moving_fabric = false
      moving_trees = true
      val c = map(t,.75,1,0,1)
      color = Vec3(1,.1,0).lerp(Vec3(1,1,0),c)
    }

    Shader.lightSpecular.set( RGBA(color*0.4,1.f) )

    try{
      Shader("sun")
      val sh = Shader.shader.get
      sh.uniforms("color") = color
      // Shader.lightDiffuse.set(RGB(t,t,1-t))
    } catch { case e:Exception => () }

  }
  val cycle2 = Schedule.cycle(15 minutes){ case t =>
  	val x = 2.f*math.cos(2*Pi*t)
  	Shader.lightPosition.x = x
  	sun.pose.pos.set(Shader.lightPosition)

    if( t < .03f ){
      val s = map (t,0,.03,0.05,0.2)
      val h = map (t,0,.03,0.2,0.9)
      trees.foreach( _.scale = s)
      treeMinHeight = h
    } else if( t < .1f ){
      val s = map (t,.03,.1,0.2,0.5)
      val h = map (t,.03,.1,0.9,1.5)
      trees.foreach( _.scale = s)
      treeMinHeight = h
    } else if( t < 0.25f){
      // val s = map (t,0,.1,0.05,0.5)
      // val h = map (t,0,.1,0.2,1.5)
      // trees.foreach( _.scale = s)
      // treeMinHeight = h
    } else if( t < 0.5f){

    } else if( t < 0.75f){

    } else if( t < 1.f){
      if( t > 0.95){
        val s = map(t,.95,1,0.5,0.05)
        val h = map(t,.95,1,1.5,0.2)
        trees.foreach( _.scale = s)
        treeMinHeight = h
      }
    }
  }

  val joints_pulse = Schedule.cycle(4 seconds){ case t =>
    try {
      Shader("joint")
      var sh = Shader.shader.get
      sh.uniforms("time") = t*2*Pi
    } catch { case e:Exception => ()}
  }

  val sun_pulse = Schedule.cycle(8 seconds){ case t =>
    try {
      Shader("sun")
      val sh = Shader.shader.get
      sh.uniforms("time") = t*2*Pi
    } catch { case e:Exception => ()}
  }
  // cycle.speed = 200.f
  // cycle2.speed = 200.f

  val colors = Array(RGB(0.7,0.,0.5), RGB(0.,.7,.5), RGB(.5,.5,.8), RGB(0.8,.5,0.1), RGB(1,1,1) )

	val cursors = new Array[(Model,Model)](4)
  for( i <- 0 until 4){
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

  var camDest = Vec3(0,1,0)

  println(com.badlogic.gdx.Gdx.graphics.getBufferFormat.samples)

	// val part = fabric.particles(fabric.particles.length/2+25)

  OpenNI.connect()
	val context = OpenNI.context

	println(OpenNI.depthMD.getFullXRes)

  val dpix = new Pixmap(640,480, Pixmap.Format.RGBA8888)
  var tex1:GdxTexture = _

  var drawconnections = false

  var inited = false
  var feedback:RenderNode = null

  var wind:com.badlogic.gdx.audio.Music = _
  var volume = Vec3(0)

  def loadShaders(){
    Shader.load("joint", File(Gallery.path + "shaders/basic.vert"), File(Gallery.path + "shaders/skel.frag")).monitor
    Shader.load("bone", File(Gallery.path + "shaders/basic.vert"), File(Gallery.path + "shaders/bone.frag")).monitor
    Shader.load("cursor", File(Gallery.path + "shaders/basic.vert"), File(Gallery.path + "shaders/cursor.frag")).monitor
    Shader.load("sun", File(Gallery.path + "shaders/basic.vert"), File(Gallery.path + "shaders/sun.frag")).monitor
  }

  override def init(){
    loadShaders()
    TreeNode.model.material = Material.specular //shader = "t"
  	TreeNode.model.material.color = RGB(0,0.5,0.7)

    for( i <- 1 to 4 ){ 
      OpenNI.skeletons(i) = new TriangleMan(i)
      OpenNI.skeletons(i).setColor(colors(i-1))
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

    val f = Gdx.files.absolute(Gallery.path+"res/wind.mp3")
    wind = Gdx.audio.newMusic(f)
    wind.setVolume(0.0f)
    wind.setLooping(true)
    wind.play

  }


	override def draw(){
		FPS.print
		Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, blend)

		OpenNI.updateDepth()

    mesh.primitive = Triangles
		model.draw
		sun.draw
		
		trees.foreach(_.draw)

    // Gdx.gl.glLineWidth(1)
    // if(drawconnections) linemodel.draw

    // sensors.foreach( (s) => {
    //   Sphere().scale(0.1).translate(s.position).draw()
    // })

    OpenNI.skeletons.values.foreach(_.draw)
		OpenNI.skeletons.values.foreach(_.drawJoints)

    if(moving_fabric){
      val len = OpenNI.tracking.count{ case (id,b) => b }
      for( i <- 0 until len){
        Shader("cursor")
        var sh = Shader.shader.get
        val (l,r) = cursors(i)
        sh.uniforms("color") = colors(i)
        sh.uniforms("time") = time
        l.draw; r.draw
      }
      // cursors.zipWithIndex.foreach{ case (l,r) => 
      //   if(OpenNI.tracking.isDefinedAt(id) && OpenNI.tracking(id)){
      //     Shader("cursor")
      //     var sh = Shader.shader.get
      //     sh.uniforms("color") = colors(id) //l.material.color
      //     sh.uniforms("time") = time
      //     l.draw; r.draw
      //   }
      // }
    }

	}

  override def preUnload(){
    wind.stop
    send.disconnect
    recv.disconnect
  }
  
  var lastdt = 0.01f
  override def animate(dt:Float){
    if(!inited) init()

    lastdt = dt
    time += dt
    // val bb = dpix.getPixels
		// bb.put(OpenNI.imgbytes)
		// bb.rewind
		// tex1.draw(dpix,0,0)

    Shader("composite")
    val fb = Shader.shader.get
    fb.uniforms("u_blend0") = 0.35
    fb.uniforms("u_blend1") = 0.75
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

		val tracked = OpenNI.tracking.collect{ case (id,b) if b => id }.toList
    tracked.foreach { 
			case id =>
    		OpenNI.getJoints(id)
    		OpenNI.skeletons(id).animate(dt)

        val s = OpenNI.skeletons(id)

        // if(moving_fabric && id >= 1 && id <= 4){
        //   val o1 = s.joints("lhand")
        //   val o2 = s.joints("rhand")
        //   val r1 = Ray(o1, s.bones(1).quat.toZ )
        //   val r2 = Ray(o2, s.bones(3).quat.toZ )
        //   fabric.particles.foreach( (p) => {
        //     val t1 = r1.intersectSphere(p.position, 0.25f)
        //     if(t1.isDefined){
        //       val v = s.vel("lhand")
        //       p.applyForce(v*400.f)
        //       cursors(id)._1.pose.pos.lerpTo(r1(t1.get),0.01)
        //       trees.foreach( _.root.applyForce(v*20))
        //     }
        //     val t2 = r2.intersectSphere(p.position, 0.25f)
        //     if(t2.isDefined){
        //       val v = s.vel("rhand")
        //       p.applyForce(v*400.f)
        //       cursors(id)._2.pose.pos.lerpTo(r2(t2.get),0.01)
        //       trees.foreach( _.root.applyForce(v*20))
        //     }
        //   })
        // } 
  	}

    tracked.take(4).zipWithIndex.foreach { 
      case (id,idx) =>
        val s = OpenNI.skeletons(id)
        s.setColor(colors(idx))

        if(moving_fabric){
          val o1 = s.joints("lhand")
          val o2 = s.joints("rhand")
          val r1 = Ray(o1, s.bones(1).quat.toZ )
          val r2 = Ray(o2, s.bones(3).quat.toZ )
          fabric.particles.foreach( (p) => {
            val t1 = r1.intersectSphere(p.position, 0.25f)
            if(t1.isDefined){
              val v = s.vel("lhand")
              p.applyForce(v*400.f)
              cursors(idx)._1.pose.pos.lerpTo(r1(t1.get),0.01)
              // cursors(idx)._1.material.color = colors(idx)
              trees.foreach( _.root.applyForce(v*20))
            }
            val t2 = r2.intersectSphere(p.position, 0.25f)
            if(t2.isDefined){
              val v = s.vel("rhand")
              p.applyForce(v*400.f)
              cursors(idx)._2.pose.pos.lerpTo(r2(t2.get),0.01)
              // cursors(idx)._2.material.color = colors(idx)
              trees.foreach( _.root.applyForce(v*20))
            }
          })
        } 
    }

    if(moving_trees){
      val amt = cycle2.percent + 1.f

      tracked.length match {
        case 0 => 
          drawconnections = false
          trees.foreach( (t) => {
            t.visible = 1
            if (t.mz < treeMinHeight) t.mz = treeMinHeight
            if (t.mz > 2.5) t.mz = 2.5 

            t.rz += 0.0001

            t.update(t.mz,t.rx,t.ry,t.rz)
          })

        case 1 =>
          drawconnections = false
          val s = OpenNI.skeletons(tracked(0))

          animateTreesSoloS(s,trees,false)

        case 2 =>
          val s1 = OpenNI.skeletons(tracked(0))
          val s2 = OpenNI.skeletons(tracked(1))
          // linemesh.clear
          // s1.joints.values.zip(s1.joints.values).foreach {
          //   case (j1,j2) => 
          //     linemesh.vertices += j1
          //     linemesh.vertices += j2
          // }
          // // val r1 = Random.oneOf(s1.joints.values.toArray :_*)
          // // val r2 = Random.oneOf(s2.joints.values.toArray :_*)
          // // for(i <- 0 until 4) { 
          // //   linemesh.vertices += r1()
          // //   linemesh.vertices += r2()
          // // }
          // linemesh.update
          // drawconnections = true

          animateTreesDuetS(s1,s2,trees,false)

        case 3 =>
          val s1 = OpenNI.skeletons(tracked(0))
          val s2 = OpenNI.skeletons(tracked(1))
          val s3 = OpenNI.skeletons(tracked(2))
          // linemesh.clear
          // val r1 = Random.oneOf(s1.joints.values.toArray :_*)
          // val r2 = Random.oneOf(s2.joints.values.toArray :_*)
          // val r3 = Random.oneOf(s3.joints.values.toArray :_*)
          // for(i <- 0 until 4) { 
          //   linemesh.vertices += r1()
          //   linemesh.vertices += r2()
          //   linemesh.vertices += r2()
          //   linemesh.vertices += r3()
          // }
          // linemesh.update
          // drawconnections = true

          animateTreesDuetS(s1,s2,List(trees(1)),false)
          animateTreesSoloS(s3,List(trees(0)),true)

        case count if count >= 4 =>
          val s1 = OpenNI.skeletons(tracked(0))
          val s2 = OpenNI.skeletons(tracked(1))
          val s3 = OpenNI.skeletons(tracked(2))
          val s4 = OpenNI.skeletons(tracked(3))
          // linemesh.clear
          // val r1 = Random.oneOf(s1.joints.values.toArray :_*)
          // val r2 = Random.oneOf(s2.joints.values.toArray :_*)
          // val r3 = Random.oneOf(s3.joints.values.toArray :_*)
          // val r4 = Random.oneOf(s4.joints.values.toArray :_*)
          // for(i <- 0 until 4) { 
          //   linemesh.vertices += r1()
          //   linemesh.vertices += r2()
          //   linemesh.vertices += r2()
          //   linemesh.vertices += r3()
          //   linemesh.vertices += r3()
          //   linemesh.vertices += r4()
          // }
          // linemesh.update
          // drawconnections = true

          animateTreesDuetS(s1,s2,List(trees(1)),false)
          animateTreesDuetS(s3,s4,List(trees(0)),true)

        case _ => drawconnections = false
      }
    }

    fabric.animate(speed.abs*1.f*dt)

    volume.lerpTo(Vec3(fabric.averageVelocity*100.f),0.01)
    wind.setVolume(volume.x)

    var height = 0.f

		trees.zipWithIndex.foreach{ case (t,i) =>
			val v = (n*(n-1/*-i*/)/2).toInt //n*(n+1)/2
			t.root.pose.pos.set(mesh.vertices(v))

      val quat = Quat().getRotationTo(Vec3(0,0,1), mesh.normals(v) * math.pow(-1,i) )
      t.root.pose.quat.slerpTo(quat, 0.01)
			// t.root.pose.quat.set(quat)
			t.root.restPose.quat.set(quat)
			t.animate(dt)

      if(t.visible == 1) height += t.root.getMaxAbsHeight()
		}

    // println(height)
    camDest = Camera.nav.uf()* -(height+1) + Vec3(0,1,0)
    Camera.nav.pos.lerpTo( camDest, 0.005 )

  }

  def animateTreesSolo(s:TriangleMan, ts:List[ATree]){
    val amt = cycle2.percent + 1.f

    val wide = s.joints("rhand") - s.joints("lhand")
    val dist = wide.magSq / 2.f * amt
    val height = (s.joints("rhand").y + s.joints("lhand").y) / 2.f * amt
    val up = s.joints("head") - s.joints("neck")

    ts.foreach( (t) => {
      t.visible = 1
      t.ry = -.5 + dist  
      t.mz = height
      if (t.mz < treeMinHeight) t.mz = treeMinHeight
      if (t.mz > 2.5) t.mz = 2.5 

      t.rz = 0.3 + wide.z.abs * 4.0 * amt
      t.rx = up.x * 4.0 * amt

      t.update(t.mz,t.rx,t.ry,t.rz)
    })
  }

  def animateTreesSoloS(s:TriangleMan, ts:List[ATree], right:Boolean=false){

    var off = 0
    if(right) off = 4
    anchors(0+off).position.lerpTo(s.joints("rhand"),0.01)
    anchors(1+off).position.lerpTo(s.joints("lhand"),0.01)
    anchors(2+off).position.lerpTo(s.joints("head"),0.01)
    anchors(3+off).position.lerpTo(s.joints("neck"),0.01)

    updateSensors(right)


    val amt = cycle2.percent + 1.f

    val wide = sensors(0+off).position - sensors(1+off).position //s.joints("rhand") - s.joints("lhand")
    val dist = wide.magSq / 2.f * amt
    val height = (sensors(0+off).position.y + sensors(1+off).position.y) / 2.f * amt
    val up = sensors(2+off).position - sensors(3+off).position

    ts.foreach( (t) => {
      t.visible = 1
      t.ry = wide.x * amt + amt*0.5 //+ dist
      t.mz = height
      if (t.mz < treeMinHeight) t.mz = treeMinHeight
      if (t.mz > 2.5) t.mz = 2.5 

      t.rz = 0.6 + wide.z.abs * 4.0 * amt
      t.rx = up.x * 2.0 * amt

      t.update(t.mz,t.rx,t.ry,t.rz)
    })
  }

  def animateTreesDuet(s1:TriangleMan,s2:TriangleMan, ts:List[ATree]){
    val amt = cycle2.percent + 1.f

    val wide = s1.joints("rhand") - s2.joints("lhand")
    val dist = wide.magSq / 2.f * amt
    val height = (s1.joints("rhand").y + s2.joints("lhand").y) / 2.f * amt
    val up = s1.joints("head") - s2.joints("head")

    ts.foreach( (t) => {
      t.visible = 1
      t.ry = -.6 + dist  
      t.mz = height
      if (t.mz < treeMinHeight) t.mz = treeMinHeight
      if (t.mz > 2.5) t.mz = 2.5 

      t.rz = 0.3 + wide.z.abs * 4.0 * amt
      t.rx = up.y * 4.0 * amt

      t.update(t.mz,t.rx,t.ry,t.rz)
    })
  }

  def animateTreesDuetS(s1:TriangleMan,s2:TriangleMan, ts:List[ATree], right:Boolean = false){

    var off = 0
    if(right) off = 4
    anchors(0+off).position.lerpTo(s1.joints("rhand"),0.01)
    anchors(1+off).position.lerpTo(s2.joints("lhand"),0.01)
    anchors(2+off).position.lerpTo(s1.joints("head"),0.01)
    anchors(3+off).position.lerpTo(s2.joints("head"),0.01)

    updateSensors(right)

    val amt = cycle2.percent + 1.f

    val wide = sensors(0+off).position - sensors(1+off).position //s.joints("rhand") - s.joints("lhand")
    val dist = wide.magSq / 2.f * amt
    val height = (sensors(0+off).position.y + sensors(1+off).position.y) / 2.f * amt
    val up = sensors(2+off).position - sensors(3+off).position

    ts.foreach( (t) => {
      t.visible = 1
      t.ry = wide.x * amt  
      t.mz = height
      if (t.mz < treeMinHeight) t.mz = treeMinHeight
      if (t.mz > 2.5) t.mz = 2.5 

      t.rz = 0.3 + wide.z.abs * 4.0 * amt
      t.rx = up.y * 4.0 * amt

      t.update(t.mz,t.rx,t.ry,t.rz)
    })
  }

  val sensors = Array[Particle](new Particle(),new Particle(),new Particle(), new Particle(),new Particle(),new Particle(),new Particle(), new Particle())
  val anchors = Array[Particle](new Particle(),new Particle(),new Particle(), new Particle(),new Particle(),new Particle(),new Particle(), new Particle())
  anchors.foreach( _.mass = 10.f)

  val constraints = for(i <- 0 until 8) yield LinearSpringConstraint(sensors(i), anchors(i), 0.25, 0.7)
  
  var xt = 0.f

  def updateSensors(right:Boolean){
    val timeStep = .015f
    val damping = 20.f
    Integrators.setTimeStep(timeStep)

    val steps = ( (lastdt+xt) / timeStep ).toInt
    xt += lastdt - steps * timeStep

    for( t <- (0 until steps)){
      for( s <- (0 until 3) ){ 
        if(!right) constraints.take(4).foreach( _.solve() )
        else constraints.takeRight(4).foreach( _.solve() )
      }

      if(!right) sensors.take(4).foreach( (p) => {
        p.applyGravity()
        p.applyDamping(damping)
        p.step() // timeStep
      })
      else sensors.takeRight(4).foreach( (p) => {
        p.applyGravity()
        p.applyDamping(damping)
        p.step() // timeStep
      })
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
  // Keyboard.bind("r", ()=>{video.ScreenCapture.framerate = 1.f; video.ScreenCapture.start})
  // Keyboard.bind("t", ()=>{video.ScreenCapture.stop})

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
        else if (t.mz > 2.5) t.mz = 2.5 
        if (t.scale <= 0.01) t.scale = 0.01
        else if (t.scale > 0.5) t.scale = 0.5 
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

  var imgwriter = new video.VideoWriter("", 640, 480, 1, 15)
  // var depthwriter = new video.VideoWriter("", 640, 480, 1, 15)
  var screenwriter:video.VideoWriter = _
  
  var cap:akka.actor.Cancellable = _
  var scap:akka.actor.Cancellable = _
  var capreset:akka.actor.Cancellable = _

  Keyboard.bind("r", ()=>{
    println("recording.")
  
    imgwriter = new video.VideoWriter("", 640, 480, 1, 15)
    screenwriter = new video.VideoWriter("", Window.width, Window.height, 1, 15)

    cap = Schedule.every(1 second){
      video.Video.writer ! video.Bytes(imgwriter,OpenNI.rgbbytes,640,480)
      // video.Video.writer ! video.Bytes(depthwriter,OpenNI.imgbytes,640,480)
    }
    scap = Schedule.cycle(1 second){
      case t if t >= 1.f =>
        val bytes = com.badlogic.gdx.utils.ScreenUtils.getFrameBufferPixels(true)
        video.Video.writer ! video.Bytes(screenwriter,bytes,Window.width,Window.height)
      case _ => ()
    }
    capreset = Schedule.every(30 minute){
      cap.cancel
      scap.cancel
      video.Video.writer ! video.Close(imgwriter)
      // video.Video.writer ! video.Close(depthwriter)
      video.Video.writer ! video.Close(screenwriter)
      imgwriter = new video.VideoWriter("", 640, 480, 1, 15)
      // depthwriter = new video.VideoWriter("", 640, 480, 1, 15)
      screenwriter = new video.VideoWriter("", Window.width, Window.height, 1, 15)

      cap = Schedule.every(1 second){
        video.Video.writer ! video.Bytes(imgwriter,OpenNI.rgbbytes,640,480)
        // video.Video.writer ! video.Bytes(depthwriter,OpenNI.imgbytes,640,480)
      }
      scap = Schedule.cycle(1 second){
        case t if t >= 1.f =>
          val bytes = com.badlogic.gdx.utils.ScreenUtils.getFrameBufferPixels(true)
          video.Video.writer ! video.Bytes(screenwriter,bytes,Window.width,Window.height)
        case _ => ()
      }
    }
  })
  Keyboard.bind("y", ()=>{
    println("recording.")
    imgwriter = new video.VideoWriter("", 640, 480, 1, 15)
    screenwriter = new video.VideoWriter("", Window.width, Window.height, 1, 15)

    cap = Schedule.every(1 second){
      video.Video.writer ! video.Bytes(imgwriter,OpenNI.rgbbytes,640,480)
      // video.Video.writer ! video.Bytes(depthwriter,OpenNI.imgbytes,640,480)
    }
    scap = Schedule.cycle(1 second){
      case t if t >= 1.f =>
        val bytes = com.badlogic.gdx.utils.ScreenUtils.getFrameBufferPixels(true)
        video.Video.writer ! video.Bytes(screenwriter,bytes,Window.width,Window.height)
      case _ => ()
    }
    capreset = Schedule.after(30 minute){
      cap.cancel
      scap.cancel
      video.Video.writer ! video.Close(imgwriter)
      // video.Video.writer ! video.Close(depthwriter)
      video.Video.writer ! video.Close(screenwriter)
      imgwriter = new video.VideoWriter("", 640, 480, 1, 15)
      // depthwriter = new video.VideoWriter("", 640, 480, 1, 15)
      screenwriter = new video.VideoWriter("", Window.width, Window.height, 1, 15)
    }
  })
  Keyboard.bind("t", ()=>{
    println("recording cancelled.")
    cap.cancel
    scap.cancel
    capreset.cancel
  })
  

}


Script