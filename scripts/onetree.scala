
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

import com.badlogic.gdx.Gdx

import akka.actor._
import akka.event.Logging

import de.sciss.osc._
 
Shader.bg.set(0,0,0,1)

var idx = 0

class ATree(b:Int=8) extends Tree {
  var visible = 0
  var (mx,my,mz,rx,ry,rz) = (0.f,0.f,0.f,0.f,0.f,0.f)
  setAnimate(true)
  setReseed(true)
  setDepth(b)
  branch(b)

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



object Script extends SeerScript {

  var dirty = true
  var update = false

  var receiver:ActorRef = _

  var trees = ListBuffer[ATree]()
  trees += new ATree(9)
  trees += new ATree(7)
  trees += new ATree(7)
  trees += new ATree(7)
  val tree = trees(0)
  tree.visible = 1

  // tree.root.position.set(0,-2,-4)
  tree.root.pose.pos.set(0,-2,-4)

	override def onLoad(){
	}

	override def draw(){
    FPS.print
    trees.foreach(_.draw)
	}

	override def onUnload(){
	}

  
  override def animate(dt:Float){
    trees.foreach(_.animate(dt))
  }


  // input events
  Keyboard.clear()
  ScreenCaptureKey.use()
  Keyboard.use()
  Keyboard.bind("t", ()=>{SaveTheTrees.save("t.json")})
  Keyboard.bind("r", ()=>{SaveTheTrees.load("t.json")})
  Keyboard.bind("1", ()=>{idx=0;})
  Keyboard.bind("2", ()=>{idx=1;})
  Keyboard.bind("3", ()=>{idx=2;})
  Keyboard.bind("4", ()=>{idx=3;})
  Keyboard.bind("0", ()=>{idx= -1;})

  OSC.disconnect
  OSC.clear()
  OSC.listen(8008)
  OSC.bindp {
    case Message("/test",f:Float) => println(s"test: $f")
    case _ => ()
  }

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
}


Trackpad.clear
Trackpad.connect
Trackpad.bind( (i,f) => {

  // var t = new ATree
  // if( idx >= 0){ 
    val t = Script.trees(idx)
    t.visible = 1
  // }

  i match {
    case 1 =>
      val ur = Vec3(1,0,0) //Camera.nav.ur()
      val uf = Vec3(0,0,1) //Camera.nav.uf()

      Script.trees.foreach{ case t =>
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

  t.root.pose.pos.set(t.mx,t.my,0)

  if(i > 2){
    t.update(t.mz,t.rx,t.ry,t.rz) 

  }
})









// must return this from script
Script