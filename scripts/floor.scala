
import com.fishuyo.seer._
import graphics._
import dynamic._
import spatial._
import io._
import cv._
import video._
import util._
// import kinect._
import kodama.actor.ActorManager.{system_floor => system}
// import actor._

import scala.collection.mutable.ListBuffer
import scala.collection.JavaConversions._

import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.glutils._
import com.badlogic.gdx.graphics.{Texture => GdxTexture}

import org.opencv.core._
import org.opencv.highgui._
import org.opencv.imgproc._

import akka.actor._
import akka.event.Logging

import concurrent.duration._
import de.sciss.osc.Message


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
    nav.pos.wrap(Vec3(-1,-1,-.05),Vec3(1,1,.05))
    if(math.abs(nav.pos.z) < 0.01){
      Shader("rd")
      var s = Shader.shader.get
      s.uniforms("brush") = (nav.pos.xy+Vec2(1,1))*0.5
    }
  }
}
 

object Script extends SeerScript {

  var remote:ActorRef = _
  val local:ActorRef = system.actorOf(Props(new LocalActor ), name = "resize")

  OpenCV.loadLibrary()

  var loop = new VideoLoop
  var dirty = true

	var bytes:Array[Byte] = null
	var (w,ww,h,hh) = (0.0,0.0,0.0,0.0)


  val capture = new VideoCapture(1)
  Thread.sleep(250)

  w = capture.get(Highgui.CV_CAP_PROP_FRAME_WIDTH)
  h = capture.get(Highgui.CV_CAP_PROP_FRAME_HEIGHT)
  
  ww = w
  hh = h
  println( s"starting capture w: $w $h")

  var subRect = new Rect(0,0,w.toInt,h.toInt)

  var pix:Pixmap = null
  var texture:GdxTexture = null
  var loopNode:TextureNode = null

  val quads = for(i <- 0 until 9) yield Plane()
  quads.foreach(_.material = Material.basic)

  var scale = 0.5

  // RD
  var rddt = 0.4f
  var (feed,kill) = (0.025f,0.06f)
  var resizeRD = false
  var rdNode:RDNode = null
  var inited = false

  val s = Plane()
  s.material = Material.basic
  s.material.textureMix = 1.f
  s.shader = "colorize"

  val agents = for(i <- 0 until 0) yield {
    val a = new Agent
    a.nav.vel = Vec3(0,0,.1)
    a.nav.angVel = Random.vec3()
    a
  }

  var blue = 0.f
  val color2 = Vec3(1,0,0)
  val color3 = Vec3(0,0,1)
  val color4 = Vec3(0,1,1)
  val color2d = Vec3(1,0,0)
  val color3d = Vec3(0,0,1)
  val color4d = Vec3(0,1,1)
  var (blend0,blend1) = (0.95,0.6)
  var ncompNode:RenderNode = null
  var colorizeNode:RenderNode = null



  def loadShaders(){
    Shader.load("rd", File("shaders/basic.vert"), File("shaders/rd_floor.frag")).monitor
    Shader.load("colorize", File("shaders/basic.vert"), File("shaders/colorize.frag")).monitor
    Shader.load("ncomposite", File("shaders/basic.vert"), File("shaders/ncomp_floor.frag")).monitor
  }

	override def onLoad(){
	}

  override def init(){
    loadShaders()
    rdNode = new RDNode
    // rdNode.camera = Camera //weirdness
    SceneGraph.roots += rdNode

    // loopNode = new TextureNode( texture )
    // loopNode.outputTo(ScreenNode)
    // SceneGraph.addNode(loopNode)
    // ScreenNode.scene.clear
    // ScreenNode.scene.push(Plane().scale(-1,-1,1))
    colorizeNode = new RenderNode
    colorizeNode.shader = "colorize"
    colorizeNode.scene.push(Plane())
    rdNode.outputTo(colorizeNode)

    ncompNode = new RenderNode
    ncompNode.shader = "ncomposite"
    ncompNode.clear = false
    ncompNode.scene.push(Plane())
    SceneGraph.root.outputTo(ncompNode)
    colorizeNode.outputTo(ncompNode)
    ncompNode.outputTo(ScreenNode)

    // resize(560,410,710,510) // ceilingcam

    // resize(110,10,1780,1070) // wide angle
    resize(640, 170, 750, 680)


    SceneGraph.root.camera = new OrthographicCamera(2,2)

    inited = true

  }
	override def draw(){
    FPS.print

    quads.foreach(_.draw)

    // rdNode.bindBuffer(0)
    // s.draw
    // g.draw
    agents.foreach(_.draw)
    for(i <- 0 until 10) rdNode.render
    colorizeNode.render
    ncompNode.render
	}

	override def preUnload(){
    local ! akka.actor.PoisonPill
		send.disconnect
		recv.disconnect
	}
	override def onUnload(){
    // if( texture != null) texture.dispose
    loop.clear
		capture.release
    ScreenNode.inputs.clear
    SceneGraph.removeNode(loopNode)

    SceneGraph.roots -= rdNode

	}

  def resizeC(x1:Float,y1:Float, x2:Float, y2:Float){
    implicit def f2i(f:Float) = f.toInt
    val c = clamper(0.f,1.f)_
    val (l,r) = (if(x1>x2) (c(x2),c(x1)) else (c(x1),c(x2)) )
    val (t,b) = (if(y1>y2) (c(y2),c(y1)) else (c(y1),c(y2)) )
    // println(s"resizeC: ${l*w} ${t*h} ${(r-l)*w} ${(b-t)*h}")
    resize( l*w, t*h, (r-l)*ww, (b-t)*hh )
  }
  
  def resize(x:Int, y:Int, width:Int, height:Int){
    var wid = width
    var hit = height
    if(x+wid > ww) wid = ww.toInt-x
    if( wid % 2 == 1) wid -= 1

    if(y+hit > hh) hit = hh.toInt-y
    w = wid.toDouble
    h = hit.toDouble
    println(s"resize: ${x} ${y} ${wid} ${hit}")
    loop.clear()
    dirty = true
    subRect = new Rect(x,y,wid,hit)
  }

  def resizeFull(){
    resize(0,0,ww.toInt,hh.toInt)
  }

  override def animate(dt:Float){


    if( dirty ){  // resize everything if using sub image
      pix = new Pixmap((w*scale).toInt,(h*scale).toInt, Pixmap.Format.RGB888)
      bytes = new Array[Byte]((h.toInt*scale*w.toInt*scale*3).toInt)
      if(texture != null) texture.dispose
      texture = new GdxTexture(pix) 

      val size = Vec3(.45f, .5f, 1.f)
      val offset = Vec3(0.015,0,0)
      quads.zipWithIndex.foreach { case(quad,i) =>
        val (x,y) = (i % 3,i / 3)
        val flip = Vec3(1.f + -2.f*(x%2), 1.f + -2.f*(y%2), 1.f)
        val move = Vec3(x-1,y-1,0.f)*size + offset
        quad.scale.set(size * flip)
        quad.translate(move)

        quad.material.texture = Some(texture)
        quad.material.textureMix = 1.f
      }
      // loopNode.texture = texture 
      dirty = false
    }

    if(!inited) init()

    if(resizeRD){
      rdNode.resize(Viewport(0,0,Window.width,Window.height))
      resizeRD = false
    }

    if( remote == null){
      remote = system.actorFor("akka.tcp://seer@localhost:2553/user/puddle")
      if(remote != null) remote ! ((w*scale).toInt,(h*scale).toInt)
    }

    // RD animate

    Shader("rd")
    var s = Shader.shader.get
    var bx = map(B.pos.x,-.8,.9,.3,.7)
    // bx = map(B.pos.x,-1.8,1.8,0,1)
    var by = map(B.pos.z,.4,-.7,.35,.65)
    // by = map(B.pos.z,1.5,-1.5,0,1)
    s.uniforms("brush") = Vec2(bx,by) //Mouse.xy()
    s.uniforms("brushSize") = map(B.pos.y,0,2,2000,200) //Mouse.xy()
    s.uniforms("width") = Window.width.toFloat //*1.5//*2.
    s.uniforms("height") = Window.height.toFloat //*1.5//*2.
    s.uniforms("F") = feed //0.037 //62
    s.uniforms("K") = kill //0.06 //093
    s.uniforms("dt") = rddt

    color2.lerpTo(color2d,0.001)
    color3.lerpTo(color3d,0.001)
    color4.lerpTo(color4d,0.001)

    Shader("colorize")
    s = Shader.shader.get
    s.uniforms("color1") = RGBA(0,0,0,0)
    s.uniforms("color2") = RGBA(color2, .3f)
    s.uniforms("color3") = RGBA(color3, .4f)
    s.uniforms("color4") = RGBA(color4, .5f)
    s.uniforms("color5") = RGBA(0,0,0,.6f)

    Shader("ncomposite")
    s = Shader.shader.get
    s.uniforms("u_blend0") = blend0
    s.uniforms("u_blend1") = blend1


    agents.foreach( _.animate(dt))

    // camera animate

    try{

  	val cam = new Mat()
  	val read = capture.read(cam)  // read from camera

    if( !read ){ return }

    var img = cam
    // img = Kinect.videoMat

    val subImg = new Mat(img, subRect )   // take sub image

    val rsmall = new Mat()
  	val small = new Mat()

  	Imgproc.resize(subImg,small, new Size(), scale,scale,0)   // scale down
    // Core.flip(small,rsmall,0)   // flip so mirrored
    Imgproc.cvtColor(small,rsmall, Imgproc.COLOR_BGR2RGB)   // convert to rgb

    var sub = rsmall

  	var out = new Mat()
  	loop.videoIO( sub, out)  // pass frame to loop get next output
    // if( out.empty()) return

    out = rsmall

    // copy MAT to pixmap
  	out.get(0,0,bytes)
		val bb = pix.getPixels()
		bb.put(bytes)
		bb.rewind()

    if(remote != null){
      remote ! bytes
      println("sent bytes")
    }

    // update texture from pixmap
		texture.draw(pix,0,0)

    cam.release
    subImg.release
    small.release
    rsmall.release
    out.release
    } catch { case e:Exception => ()}

  }

  // IO
	Keyboard.clear()
	Keyboard.use()
	Keyboard.bind("r",()=>{ loop.toggleRecord() })
	Keyboard.bind("c",()=>{ loop.stop(); loop.clear() })
	Keyboard.bind("x",()=>{ loop.stack() })
	Keyboard.bind("t",()=>{ loop.togglePlay() })
	Keyboard.bind("v",()=>{ loop.reverse() })
	Keyboard.bind("z",()=>{ loop.rewind() })
	Keyboard.bind("1",()=>{ resizeFull();})

	var rx = 0
	var ry = 0
	var rw = w.toInt
	var rh = h.toInt
	Keyboard.bind("y",()=>{ rx -= 10; resize(rx,ry,rw,rh);})
	Keyboard.bind("u",()=>{ rx += 10;  resize(rx,ry,rw,rh);})
	Keyboard.bind("h",()=>{ rw -= 10; resize(rx,ry,rw,rh);})
	Keyboard.bind("j",()=>{ rw += 10;  resize(rx,ry,rw,rh);})
	Keyboard.bind("i",()=>{ ry -= 10; resize(rx,ry,rw,rh);})
	Keyboard.bind("k",()=>{ ry += 10;  resize(rx,ry,rw,rh);})
	Keyboard.bind("o",()=>{ rh -= 10; resize(rx,ry,rw,rh);})
	Keyboard.bind("l",()=>{ rh += 10;  resize(rx,ry,rw,rh);})

	Mouse.clear()
	Mouse.use()
	// Mouse.bind("drag", (i)=>{
	//   val x = i(0) / (1.0*Window.width)
	//   val y = i(1) / (1.0*Window.height)
	//   val speed = (400 - i(1)) / 100.0
	//   val decay = (i(0) - 400) / 100.0

	//   // # Main.loop.setSpeed(1.0) #speed)
	//   // # Main.loop.setAlphaBeta(decay, speed)
	//   loop.setAlpha(decay)
	// })

  var B = Pose()
  var Blast = Pose()
  var Bvel = Vec3()
  VRPN.clear
  VRPN.bind("gnarl", (p)=>{
    Blast = B
    B = B.lerp(p,0.1f)
    Bvel = p.pos - Blast.pos
  })


	// OSC
  val send = new OSCSend
  send.connect("localhost", 8010)
  val recv = new OSCRecv
  recv.listen(8011)
  recv.bindp {
    case Message("/rd/fk",f:Float,k:Float) => println("update fk"); feed = f; kill = k;
    case Message("/rd/clear") => println("clear rd buffer"); resizeRD = true
    case Message("/rd/dt", f:Float) => rddt = f
    case Message("/ncomp/blend0",f:Float) => blend0 = f
    case Message("/ncomp/blend1",f:Float) => blend1 = f
    case Message("/rd/blue",f:Float) => blue = f
    case Message("/rd/color2",r:Float,g:Float,b:Float) => println("color2"); color2d.set(Vec3(r,g,b))
    case Message("/rd/color3",r:Float,g:Float,b:Float) => color3d.set(Vec3(r,g,b))
    case Message("/rd/color4",r:Float,g:Float,b:Float) => color4d.set(Vec3(r,g,b))

  }


  class LocalActor extends Actor with akka.actor.ActorLogging {
    override def preStart() = {
      log.debug("Starting")
    }
    override def preRestart(reason: Throwable, message: Option[Any]) {
      log.error(reason, "Restarting due to [{}] when processing [{}]",
        reason.getMessage, message.getOrElse(""))
    }
    def receive = {
      case "request" => 
        println("resize requested")
        if( remote == null){
        val remote = system.actorFor("akka.tcp://seer@localhost:2553/user/puddle")
        }
        if(remote != null) remote ! ((w*scale).toInt,(h*scale).toInt)

      case _ => ()
    }
  }
}



Script