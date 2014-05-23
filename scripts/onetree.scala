
import com.fishuyo.seer._
import graphics._
import dynamic._
import maths._
import spatial._
import io._
import cv._
import video._
import util._
import kinect._
import actor._

import trees._
// import particle._
// import structures._

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

import de.sciss.osc._
 
Shader.bg.set(0,0,0,1)

object Script extends SeerScript {

  var dirty = true
  var update = false

  var receiver:ActorRef = _

  val tree = Tree()
  // tree.root.position.set(0,-2,-4)
  tree.root.pose.pos.set(0,-2,-4)

  val depth = 9
  tree.setAnimate(true)
  tree.setReseed(true)
  tree.setDepth(depth)
  tree.branch(depth)

	override def onLoad(){
	}

	override def draw(){
    tree.draw
	}

	override def onUnload(){
	}

  
  override def animate(dt:Float){
    tree.animate(dt)
  }


}

// input events
ScreenCaptureKey.use()
Keyboard.use()

var B = Pose()
// VRPN.clear
// VRPN.bind("b", (p)=>{
//   B = B.lerp(p,0.1f)
//   val q = -B.quat.toZ() //toEulerVec()
//   // println(q)
//   val t = Script.tree
//   t.bAngle.y.setMinMax( 0.05, q.x,false )
//   // t.sAngle.x.setMinMax( 0.15, B.pos.x, false )
//   // t.bAngle.x.setMinMax( 0.15, B.pos.x, false )
//   // t.sAngle.z.setMinMax( 0.05, q.z, false )
//   // t.bAngle.z.setMinMax( 0.05, q.z, false )

//   t.sRatio.setMinMax( 0.15, B.pos.y, false )
//   t.bRatio.setMinMax( 0.15, B.pos.y, false )

//   t.refresh()
// })


OSC.disconnect
OSC.clear()
OSC.listen(8008)
OSC.bindp {
  case Message("/test",f:Float) => println(s"test: $f")
  case _ => ()
}

var (mx,my,mz,rx,ry,rz) = (0.f,0.f,0.f,0.f,0.f,0.f)
Trackpad.clear
Trackpad.connect
Trackpad.bind( (i,f) => {
  val t = Script.tree

  i match {
    case 1 =>
      val ur = Vec3(1,0,0) //Camera.nav.ur()
      val uf = Vec3(0,0,1) //Camera.nav.uf()

      t.root.applyForce( ur*(f(0)-0.5) * 2.0*f(4) )
      t.root.applyForce( uf*(f(1)-0.5) * -2.0*f(4) )
    case 2 =>
      mx += f(2)*0.05  
      my += f(3)*0.05
    case 3 =>
      ry = ry + f(2)*0.05  
      mz = mz + f(3)*0.01
      if (mz < 0.08) mz = 0.08
      if (mz > 3.0) mz = 3.0 
    case 4 =>
      rz = rz + f(3)*0.05
      rx = rx + f(2)*0.05
    case _ => ()
  }

  t.root.pose.pos.set(mx,my,0)

  if(i > 2){
    t.bAngle.y.setMinMax( 0.05, ry,false )
    // ##t.bAngle.y.set(mx)
      t.sRatio.setMinMax( 0.05, mz, false )
      // ## t.sRatio.set( mz )
      t.bRatio.setMinMax( 0.05, mz, false )
      // ##t.bRatio.set( my )
      t.sAngle.x.setMinMax( 0.05, rx, false )
      t.bAngle.x.setMinMax( 0.05, rx, false )
      // ##t.sAngle.x.set( rx )
      t.sAngle.z.setMinMax( 0.05, rz, false )
      t.bAngle.z.setMinMax( 0.05, rz, false )
      // ##t.sAngle.z.set( rz )
      // ##t.branch(depth)
      t.refresh()

      // # t.root.accel.zero
      // # t.root.euler.zero
  }
})


// object SaveTheTrees {
// 	def save(name:String){
// 		var project = "tree-" + (new java.util.Date()).toLocaleString().replace(' ','-').replace(':','-')
//     if( name != "") project = name
//     var path = "TreeData/" + project
//     var file = Gdx.files.internal(path).file()
//     file.mkdirs()

//   	var map = Map[String,Any]()
//     map = map + (("loop"+i) -> List(b.curSize,b.rPos,b.wPos,b.rMin,b.rMax,b.speed,l.gain,l.pan,l.decay))

//     file = Gdx.files.external(path+"/loops.json").file()
//   	val p = new java.io.PrintWriter(file)
// 	  p.write( scala.util.parsing.json.JSONObject(map).toString( (o) =>{
// 	  	o match {
// 	  		case o:List[Any] => s"""[${o.mkString(",")}]"""
// 	  		case s:String => s"""${'"'}${s}${'"'}"""
// 	  		case a:Any => a.toString()  
// 	  	}
// 	  }))
// 	  p.close

// 	}

// 	def load(name:String){
//     val path = "LoopData/" + name
//     val file = Gdx.files.external(path+"/loops.json").file()

//     val sfile = scala.io.Source.fromFile(file)
//   	val json_string = sfile.getLines.mkString
//   	sfile.close

//   	val parsed = scala.util.parsing.json.JSON.parseFull(json_string)
//   	if( parsed.isEmpty ){
//   		println(s"failed to parse: $path")
//   		return
//   	}

//   	val map = parsed.get.asInstanceOf[Map[String,Any]]

//     for( i <- (0 until loops.size)){
//     	loops(i).load(path+"/"+i+".wav")

// 	  	val l = map("loop"+i).asInstanceOf[List[Double]]
// 	  	// println(l)
// 	  	val loop = loops(i)
// 	  	val b = loop.b

// 	  	b.curSize = l(0).toInt
// 	  	b.maxSize = b.curSize
// 	  	b.rPos = l(1).toFloat
// 	  	b.wPos = l(2).toInt
// 	  	b.rMin = l(3).toInt
// 	  	b.rMax = l(4).toInt
// 	  	b.speed = l(5).toFloat
// 	  	loop.gain = l(6).toFloat
// 	  	loop.pan = l(7).toFloat
// 	  	loop.decay = l(8).toFloat

//     }
// 	}
// }





// must return this from script
Script