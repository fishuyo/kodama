
import com.fishuyo.seer._
import graphics._
import dynamic._
import spatial._
import io._
import cv._
import video._
import util._
// import kinect._
// import kodama.actor.ActorManager.{system_floor => system}
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


object Script extends SeerScript {

  var remote:ActorRef = _
  // val local:ActorRef = system.actorOf(Props(new LocalActor ), name = "resize")

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

  var scale = 1.
  var inited = false

  override def init(){
    // resize(640, 170, 750, 680)
    SceneGraph.root.camera = new OrthographicCamera(2,2)
    inited = true
  }

	override def draw(){
    FPS.print

    // quads.foreach(_.draw)
    quads(0).draw
	}

	override def preUnload(){
    // local ! akka.actor.PoisonPill
		// send.disconnect
		// recv.disconnect
	}
	override def onUnload(){
    // if( texture != null) texture.dispose
    loop.clear
		capture.release
    // ScreenNode.inputs.clear
    // SceneGraph.removeNode(loopNode)

    // SceneGraph.roots -= rdNode

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
  	// loop.videoIO( sub, out)  // pass frame to loop get next output
    // if( out.empty()) return

    out = rsmall

    // copy MAT to pixmap
  	out.get(0,0,bytes)
		val bb = pix.getPixels()
		bb.put(bytes)
		bb.rewind()

    // if(remote != null) remote ! bytes

    // update texture from pixmap
		texture.draw(pix,0,0)

    cam.release
    subImg.release
    small.release
    rsmall.release
    out.release
    } catch { case e:Exception => ()}

  }

}



Script