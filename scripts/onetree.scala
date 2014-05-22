
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





// must return this from script
Script