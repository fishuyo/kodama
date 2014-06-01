
import com.fishuyo.seer._
import graphics._
import dynamic._
import maths._
import spatial._
import io._
import util._
import actor._

import trees._
import particle._

import scala.collection.mutable.ListBuffer

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.{Texture => GdxTexture}

import akka.actor._
import akka.event.Logging

import de.sciss.osc.Message
 
import concurrent.duration._


Scene.alpha = .3
SceneGraph.root.depth = false

Camera.nav.pos.set(0,1,4)
Camera.nav.quat.set(1,0,0,0)

Shader.bg.set(0,0,0,1)


///
/// Trees
///
var idx = 0

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
      map = map + (("t"+i) -> List(t.mx,t.my,t.mz,t.rx,t.ry,t.rz,t.visible))
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
      t.root.pose.pos.set(t.mx,t.my,0)
      t.update(t.mz,t.rx,t.ry,t.rz)
    }
  }
}



////
//// Agents
///
class Flock {

}

class Agent extends Animatable {
  var nav = Nav()
  nav.pos = Random.vec3()
  val b = Sphere().scale(.01,.01,.025)
  b.material = Material.specular
  b.material.color = RGB(0,1,1) 
  // for(i <- 0 until 7){ 
  //  val w = Circle().translate(0,0,1-i/3.5f)
  //  w.material = Material.specular
  //  w.material.color = RGB(0,1,1)
  //  w.scale(1.5f - math.abs(w.pose.pos.z))
  //  b.addChild(w)
  // }

  // Schedule.cycle(1 second){ case t =>
  //  b.children.zipWithIndex.foreach{
  //    case (c,i) => c.scale.set(1+t+i/7.f)
  //  }
  // }

  override def draw(){ 
    b.pose = nav
    b.draw
  }
  override def animate(dt:Float){
    nav.step(dt)
    nav.pos.wrap(Vec3(-2,0,-2),Vec3(2,4,2))
    if(math.abs(nav.pos.z) < 0.01){
      Shader("rd")
      var s = Shader.shader.get
      val p = Camera.project( new com.badlogic.gdx.math.Vector3(nav.pos.x,nav.pos.y,nav.pos.z))
      s.uniforms("brush") = Vec2(p.x/Window.width,p.y/Window.height)  
    }
  }
}

///
/// Script
///
///

object Script extends SeerScript {

  implicit def f2i(f:Float) = f.toInt

  var dirty = true
  var update = true

  var receiver:ActorRef = _

  var trees = ListBuffer[ATree]()
  trees += new ATree(9)
  trees += new ATree(8)
  trees += new ATree(8)
  trees += new ATree(8)
  val tree = trees(0)
  // TreeNode.model.material = Material.specular
  tree.visible = 1

  // tree.root.position.set(0,-2,-4)
  tree.root.pose.pos.set(0,-2,-4)

  var bytes:Array[Byte] = null
  var (w,h) = (660,490)

  var pix:Pixmap = null
  var texture:GdxTexture = null

  val agents = for(i <- 0 until 0) yield {
    val a = new Agent
    a.nav.vel = Vec3(0,0,.1)
    a.nav.angVel = Random.vec3()
    a
  }

  // fabric
  val np = 35
  val mesh = Plane.generateMesh(10,10,np,np,Quat.up)
  mesh.primitive = Lines
  val model = Model(mesh)
  model.material = Material.specular
  model.material.color = RGB(0,0.5,0.7)

  val fabric = new SpringMesh(mesh,1.f)
  fabric.pins += AbsoluteConstraint(fabric.particles(0), fabric.particles(0).position)
  fabric.pins += AbsoluteConstraint(fabric.particles(np), fabric.particles(np).position)
  // fabric.pins += AbsoluteConstraint(fabric.particles(0), fabric.particles(0).position)
  fabric.pins += AbsoluteConstraint(fabric.particles.last, fabric.particles.last.position)
  Gravity.set(0,0,0)

  val sun = Sphere().scale(0.1f)
  var blend = GL20.GL_ONE_MINUS_SRC_ALPHA
  val cursor = Sphere().scale(0.05)
  var lpos = Vec2()
  var vel = Vec2()

  // render nodes
  var inited = false
  var floorTexNode:TextureNode = _
  var floorNode:RenderNode = _
  var ncompNode:RenderNode = _
  var rdNode:RDNode = _
  var colorizeNode:RenderNode = _

  var resizeRD = true
  var (feed,kill) = (0.082,0.061)
  var (blend0,blend1,blend2) = (1.f,0.5f,0.5f)
  var speed = 1.f

  // liquid nav
  var nav = Nav()
  nav.pos.set(0,1,1)

  def loadShaders(){
    Shader.load("rd", File("shaders/basic.vert"), File("shaders/rd_wall.frag")).monitor
    Shader.load("colorize", File("shaders/basic.vert"), File("shaders/colorize.frag")).monitor
    Shader.load("ncomposite", File("shaders/basic.vert"), File("shaders/ncomp_wall.frag")).monitor
  }

  override def onLoad(){
  }

  override def init(){
    loadShaders()

    val material = Material.specular
    material.color.set(0,1,1)
    material.loadTexture("res/scales.png")
    material.textureMix = 0.5f
    agents.foreach(_.b.material = material)

    rdNode = new RDNode
    colorizeNode = new RenderNode
    colorizeNode.shader = "colorize"
    colorizeNode.scene.push(Plane())
    rdNode.outputTo(colorizeNode)

    floorTexNode = new TextureNode(texture)
    floorNode = new RenderNode
    floorNode.shader = "texture"
    // floorNode.scene.push( Plane())
    floorNode.scene.push( Plane().translate(0.5,0,0).scale(.5,-1,1))
    floorNode.scene.push( Plane().translate(-0.5,0,0).scale(-.5,-1,1))
    floorTexNode.outputTo(floorNode)

    ncompNode = new RenderNode
    ncompNode.shader = "ncomposite"
    ncompNode.clear = false
    ncompNode.scene.push(Plane())
    SceneGraph.root.outputTo(ncompNode)
    floorNode.outputTo(ncompNode)
    colorizeNode.outputTo(ncompNode)
    // ncompNode.outputTo(ScreenNode)

    val request = system.actorFor("akka://seer@192.168.0.109:2552/user/resize")
    if( request != null ){
      println("requesting resize")
      request ! "request"
    }
    inited = true

  }

  override def draw(){
    FPS.print
    trees.foreach(_.draw)
    agents.foreach(_.draw)

    //fabric
    Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, blend)
    model.draw
    sun.draw
    cursor.draw

    floorNode.render
    for(i <- 0 until 10) rdNode.render
    colorizeNode.render
    ncompNode.render
  }

  override def preUnload(){
    receiver ! akka.actor.PoisonPill
    ScreenNode.inputs.clear
    SceneGraph.root.outputs.clear
    send.disconnect
    recv.disconnect
  }

  
  override def animate(dt:Float){
    // nav.step(dt)
    // nav.pos.wrap(Vec3(-2,0,-2),Vec3(2,4,2))
    // Camera.nav.set(nav)

    if( dirty ){  // resize everything if using sub image
      pix = new Pixmap(w,h, Pixmap.Format.RGB888)
      bytes = Array.fill(w*h*3)(255.toByte)
      if(texture != null) texture.dispose
      texture = new GdxTexture(pix)
      if(floorTexNode != null) floorTexNode.texture = texture 
      dirty = false
    }

    if(!inited) init()

    trees.foreach(_.animate(dt))

    if( receiver == null) receiver = system.actorOf(Props(new RecvActor ), name = "puddle")

    if(update){
      try{
        val bb = pix.getPixels()
        bb.put(bytes)
        bb.rewind()
      } catch { case e:Exception => "Error updating: probably size mismatch!"}

      // update texture from pixmap
      texture.draw(pix,0,0)
      update = false
      receiver ! "free"
    }

    if(resizeRD){
      rdNode.resize(Viewport(0,0,1920,512))
      resizeRD = false
    }

    Shader("rd")
    var s = Shader.shader.get
    s.uniforms("brush") = Mouse.xy()
    s.uniforms("width") = Window.width.toFloat
    s.uniforms("height") = Window.height.toFloat
    s.uniforms("F") = feed //0.037 //62
    s.uniforms("K") = kill //0.06 //093
    s.uniforms("dt") = dt

    Shader("colorize")
    s = Shader.shader.get
    s.uniforms("color1") = RGBA(0,0,0,0)
    s.uniforms("color2") = RGBA(1,0,0,.3f)
    s.uniforms("color3") = RGBA(0,0,1,.4f)
    s.uniforms("color4") = RGBA(0,1,1,.5f)
    s.uniforms("color5") = RGBA(0,0,0,.6f)

    Shader("ncomposite")
    s = Shader.shader.get
    s.uniforms("u_blend0") = blend0
    s.uniforms("u_blend1") = blend1
    s.uniforms("u_blend2") = blend2

    agents.foreach(_.animate(dt))

    // fabric

    val x = Mouse.xy().x
    if( Mouse.status() == "drag"){
      vel = (Mouse.xy() - lpos)/dt
      val r = Camera.ray(Mouse.x()*Window.width, (1.f-Mouse.y()) * Window.height)
      fabric.particles.foreach( (p) => {
        val t = r.intersectSphere(p.position, 0.25f)
        if(t.isDefined){
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

    fabric.animate(speed.abs*1.f*dt)

  }

    // schedule
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


  Mouse.clear
  Mouse.use

  // input events
  Keyboard.clear()
  Keyboard.use()
  Keyboard.bind("t", ()=>{SaveTheTrees.save("t.json")})
  Keyboard.bind("r", ()=>{SaveTheTrees.load("t.json")})
  Keyboard.bind("1", ()=>{idx=0;})
  Keyboard.bind("2", ()=>{idx=1;})
  Keyboard.bind("3", ()=>{idx=2;})
  Keyboard.bind("4", ()=>{idx=3;})
  Keyboard.bind("0", ()=>{idx= -1;})
  Keyboard.bind("g", ()=>{
    mesh.vertices.foreach{ case v => v.set(v.x,v.y+Random.float(-1,1).apply()*0.02*(v.x).abs,v.z) }
    mesh.recalculateNormals()
    mesh.update()
  })
  Keyboard.bind("5", ()=>{mesh.primitive = Triangles})
  Keyboard.bind("6", ()=>{mesh.primitive = Lines})
  Keyboard.bind("7", ()=>{blend = GL20.GL_ONE_MINUS_SRC_ALPHA})
  Keyboard.bind("8", ()=>{blend = GL20.GL_ONE})

  var G = Pose()
  VRPN.clear
  VRPN.bind("gnarl", (p)=>{
    G = G.lerp(p,0.1f)
    val e = G.quat.toEulerVec()
    val t = trees(idx)
    t.ry = -G.pos.x + math.sin(G.pos.y*15)*0.04 //map(G.pos.y,0,2,0,0.2)
    t.mz = map(G.pos.mag,0,5,0,1) + map(G.pos.y,0,2,0.5,1.3)
    t.rx = e.y / 2.f 
    t.rz = map(G.pos.z,-4,4,0,16)
    t.update()
  })

  var A = Pose()
  VRPN.bind("a", (p)=>{
    A = A.lerp(p,0.1f)
    nav.vel.set(0,0,A.pos.z* -0.3)
    nav.angVel.set((A.pos.y-1)*0.3,A.pos.x* -0.3,0)
  })

  var B = Pose()
  var Blast = Pose()
  var Bvel = Vec3()
  VRPN.bind("b", (p)=>{
    Blast = B
    B = B.lerp(p,0.1f)
    Bvel = p.pos - Blast.pos
  })


  Trackpad.clear
  Trackpad.connect
  Trackpad.bind( (i,f) => {

    // var t = new ATree
    // if( idx >= 0){ 
      val t = trees(idx)
      t.visible = 1
    // }

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
      case _ => ()
    }

    t.root.pose.pos.set(t.mx,0,-t.my)

    if(i > 2){
      t.update(t.mz,t.rx,t.ry,t.rz) 

    }
  })


  // OSC
  val send = new OSCSend
  send.connect("localhost", 8010)
  val recv = new OSCRecv
  recv.listen(8011)
  recv.bindp {
    case Message("/rd/fk",f:Float,k:Float) => println("update fk"); feed = f; kill = k;
    case Message("/rd/clear") => println("clear rd"); resizeRD = true
    case Message("/ncomp/blend0",f:Float) => blend0 = f
    case Message("/ncomp/blend1",f:Float) => blend1 = f
    case Message("/ncomp/blend2",f:Float) => blend2 = f
    case Message("/gnarl/speed", f:Float) => speed = f
      cycle.speed = speed*20
      cycle2.speed = speed*20

    case m => println(m)
  }
}


// recv byte array from floor machine
class RecvActor extends Actor with akka.actor.ActorLogging {
  var busy = false

  override def preStart() = {
    log.debug("Starting")
  }
  override def preRestart(reason: Throwable, message: Option[Any]) {
    log.error(reason, "Restarting due to [{}] when processing [{}]",
      reason.getMessage, message.getOrElse(""))
  }

  def receive = {
    case (w:Int,h:Int) =>
      log.error("resize")
      Script.w = w
      Script.h = h
      Script.dirty = true
    case "free" => busy = false
    case msg if !busy =>
      msg match{
        case b:Array[Byte] =>
          busy = true
          Script.bytes = b
          Script.update = true
      }

    case _ => ()
  }
}




// must return this from script
Script