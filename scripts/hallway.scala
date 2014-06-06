
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
import scala.collection.mutable.ArrayBuffer
import scala.collection.JavaConversions._

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20

import akka.actor._
import akka.event.Logging

import de.sciss.osc.Message

import openni._
 
Shader.bg.set(0,0,0,1)
Camera.nav.pos.set(0,2,6)

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

  var dirty = true
  var update = false

  var receiver:ActorRef = _

  var trees = ListBuffer[ATree]()
  trees += new ATree(9)
  trees += new ATree(8)
  trees += new ATree(7)
  trees += new ATree(7)
  val tree = trees(0)
  tree.visible = 1

  // tree.root.position.set(0,-2,-4)
  tree.root.pose.pos.set(0,-2,-4)

 //  OpenNI.connect()
	// val context = OpenNI.context

	// println(OpenNI.depthMD.getFullXRes)

  val skeletons = ArrayBuffer[Skeleton]()
  for( i <- 0 until 10) skeletons += new QuadMan(i)

  override def preUnload(){
    recv.clear()
    recv.disconnect()
    SceneGraph.root.outputs.clear
    ScreenNode.inputs.clear
  }

	override def onLoad(){
	}

  def loadShaders(){
    Shader.load("s1", File("shaders/basic.vert"), File("shaders/skel.frag")).monitor
    Shader.load("t", File("shaders/basic.vert"), File("shaders/tree.frag")).monitor
  }

  var inited = false
  var feedback:RenderNode = null
  override def init(){
    loadShaders()
    TreeNode.model.shader = "t"
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
  }

  override def draw(){
    FPS.print

    Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE)

    Shader("s1")
    var sh = Shader.shader.get
    sh.uniforms("time") = t

    Scene.alpha = 0.9
    SceneGraph.root.depth = false
    skeletons.foreach( (s) => {
      sh.uniforms("color") = s.color
      s.draw
    })

    trees.foreach(_.draw)
  }

  var t = 0.f
  override def animate(dt:Float){

    if(!inited) init()

    // val z = skeletons(0).joints("torso").z
    // if( z > maxz){ maxz = z; println(maxz)}
    // beta = map(z,0.f,2.7f, 0.85, 0.9999f)

    Shader("composite")
    val fb = Shader.shader.get
    fb.uniforms("u_blend0") = 0.25
    fb.uniforms("u_blend1") = 0.85

    t += dt
    skeletons.foreach((s) => {
      s.animate(dt)
      val w = s.vel("l_hand")
      trees.foreach{ case tree =>
        tree.root.applyForce( w*100.f )
      }
    })

    trees.foreach(_.animate(dt))

  }

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


  Trackpad.clear
  Trackpad.connect
  Trackpad.bind( (i,f) => {

    val t = trees(idx)
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
      case _ => ()
    }

    t.root.pose.pos.set(t.mx,t.my,0)

    if(i > 2){
      t.update(t.mz,t.rx,t.ry,t.rz) 

    }
  })


  val recv = new OSCRecv
  recv.listen(7110)
  // recv.bindp {
  //   case Message(regex(idS,name),x:Float,y:Float,z:Float) =>
  //     val id = idS.toInt - 1
  //     skeletons(id).joints(name).lerpTo(Vec3(x,y,z), 0.5f)
  //     skeletons(id).tracking = true
  //   case Message("/calibrating", id:Int) => 
  //     skeletons(id-1).calibrating = true
  //   case Message("/tracking", id:Int) => 
  //     skeletons(id-1).calibrating = false
  //     skeletons(id-1).tracking = true
  //   case Message("/lost", id:Int) => 
  //     skeletons(id-1).calibrating = false
  //     skeletons(id-1).tracking = false
  //   case _ => ()
  // }
  recv.bindp {
    case Message("/joint", name:String, id:Int, x:Float, y:Float, z:Float) =>
      val pos = Vec3(2*x-1,1-y,z) 
      if(id > 9){} else{
        skeletons(id).updateJoint(name,pos)
        // if(name == "l_hand") println(skeletons(id).vel("l_hand").mag)
        skeletons(id).tracking = true
      }
    case Message("/lost_user", id:Int) => skeletons(id).tracking = false; skeletons(id).calibrating = false;

    case m => ()
  }

}









// must return this from script
Script