

import com.fishuyo.seer._
import graphics._
import dynamic._
import maths._
import spatial._
import trees._
import io._
import io.kinect._
import util._

import collection.mutable.ListBuffer

object Script extends SeerScript {

	var tree = new Tree()
	tree.branch()

  var ground:Model = Model(Plane.generateMesh(100,100,100,100,Quat.up))
  ground.mesh.primitive = Lines
  // Run(()=>{
  // 	ground = OBJ("res/landscapealien.obj") 
	 //  ground.pose.quat.set(0.42112392f,-0.09659095f, 0.18010217f, -0.8836787f)
	 //  ground.pose.pos.set(0.f,-1.3f,-.0f)
	 //  ground.scale.set(5.f,5.f,5.f)
  // })



	override def draw(){
		FPS.print
		// if( ground == null){
			// ground = OBJ("../seer/res/obj/bunnyl.obj") //"landscapealien.obj") 
		  // ground.pose.quat.set(0.42112392f,-0.09659095f, 0.18010217f, -0.8836787f)
		  // ground.pose.pos.set(0.f,-1.3f,-.0f)
		  // ground.scale.set(5.f,5.f,5.f)
		// }
		ground.draw
		tree.draw
	}
	override def animate(dt:Float){
		tree.animate(dt)
	}

}

// object Main extends SeerApp with AudioSource {

//   var volume = Vec3(0)
//   var color = Vec3(68.0/255.0,122.0/256.0,222.0/256.0)

//   var day = Vec3(1.f,0,1.f)
//   var useLocalZ = Vec3(1.f)
//   var dayUniforms = Vec3(0.f,4.1f,0.f)
//   var nightUniforms = Vec3(0.f,2.f,1.f)

//   var move = Vec3(0.f)
//   var rot = Vec3(0.f)
//   var nmove = Vec3(0.f)
//   var nrot = Vec3(0.f)

//   var pref = ""

//   var live:Ruby = null

//   var theta = 0.f
//   var dw = 0.f
//   var blurDist = 0.1f

//   // DesktopApp() 

//   override def init(){

//     val f = Gdx.files.internal(pref +"res/wind.mp3")
//     wind = Gdx.audio.newMusic(f)
//     wind.setVolume(0.0f)
//     wind.setLooping(true)
//     wind.play

//     var s = Shader.load("firstPass", Gdx.files.internal(pref+"res/shaders/firstPass.vert"), Gdx.files.internal(pref+"res/shaders/firstPass.frag"))
//     s.monitor()
//     s = Shader.load("secondPass", Gdx.files.internal(pref+"res/shaders/secondPass.vert"), Gdx.files.internal(pref+"res/shaders/secondPass.frag"))
//     s.monitor()
//     s = Shader.load("test", Gdx.files.internal(pref+"res/shaders/test.vert"), Gdx.files.internal(pref+"res/shaders/test.frag"))
//     s.monitor()
//     s = Shader.load("texture", Gdx.files.internal(pref+"res/shaders/texture.vert"), Gdx.files.internal(pref+"res/shaders/texture.frag"))
//     s.monitor()
//     s = Shader.load("composite", Gdx.files.internal(pref+"res/shaders/composite.vert"), Gdx.files.internal(pref+"res/shaders/composite.frag"))
//     s.monitor()

//     SceneGraph.root.shader = "firstPass"

//     println("trees init.")
//     val node = new RenderNode
//     node.shader = "secondPass"
//     val quad = new Drawable {
//       val m = Mesh(Primitive2D.quad)
//       override def draw(){
//         if( day.x == 1.f) Shader("secondPass").setUniformf("u_depth", 0.f)
//         else Shader("secondPass").setUniformf("u_depth", 1.f)

//         m.draw()
//       }
//     }
//     node.scene.push( quad )
//     SceneGraph.root.outputTo(node)

//     val node2 = new RenderNode
//     node2.shader = "test"
//     val quad2 = new Drawable {
//       val m = Mesh(Primitive2D.quad)
//       override def draw(){
//         Shader("test").setUniformf("u_dist", getBlurDist)
//         m.draw()
//       }
//     }
//     node2.scene.push( quad2 )
//     // node.outputTo(node2)

//     val compNode = new RenderNode
//     compNode.shader = "composite"
//     compNode.clear = false
//     val quag = new Drawable {
//       val m = Mesh(Primitive2D.quad)
//       override def draw(){
//         // Shader("composite").setUniformf("u_blend0", 1.0f)
//         // Shader("composite").setUniformf("u_blend1", 1.0f)
//         // Shader("composite").setUniformMatrix("u_projectionViewMatrix", new Matrix4())
//         m.draw()
//       }
//     }
//     compNode.scene.push( quag )
//     node.outputTo(compNode)
//     compNode.outputTo(compNode)
//     compNode.outputTo(ScreenNode)

//     // Kinect.init()
//     tree.init()
//     live = new Ruby(pref+"trees.rb")
//   }
//   override def draw(){

//     theta += dw
//     MatrixStack.rotate(0.f,theta,0.f)

//     if(day.x == 1.f){
//       Shader("firstPass").setUniformf("u_useLocalZ", dayUniforms.z)
//       Shader("firstPass").setUniformf("u_near", dayUniforms.x)
//       Shader("firstPass").setUniformf("u_far", dayUniforms.y)
//       Shader("firstPass").setUniformf("u_useTexture", 0.f)
//       ground.draw()
//       Shader("firstPass").setUniformf("u_useLocalZ", dayUniforms.z)
//       Shader("firstPass").setUniformf("u_near", dayUniforms.x)
//       Shader("firstPass").setUniformf("u_far", dayUniforms.y)
//       Shader("firstPass").setUniformf("u_useTexture", 1.f)
//       // if(day.z == 1.f) Kinect.draw()
//       tree.draw()
//     }else{
//       Shader("firstPass").setUniformf("u_useLocalZ", 0.f)
//       Shader("firstPass").setUniformf("u_near", 0.f)
//       Shader("firstPass").setUniformf("u_far", 4.1f)
//       Shader("firstPass").setUniformf("u_useTexture", 0.f)
//       ground.draw()
//       Shader("firstPass").setUniformf("u_useLocalZ", nightUniforms.z)
//       Shader("firstPass").setUniformf("u_near", nightUniforms.x)
//       Shader("firstPass").setUniformf("u_far", nightUniforms.y)
//       Shader("firstPass").setUniformf("u_useTexture", 1.f)
//       //Kinect.draw()
//       tree.draw()
//     }

//   }

//   def rotateWorld(v:Float) = dw = v
//   def blurDist(v:Float){ blurDist = v }
//   def getBlurDist() = blurDist
  
//   def draw2(){
//       // Shader("secondPass").setUniformf("u_edge", 1.)
//     if( day.x == 1.f) Shader("secondPass").setUniformf("u_depth", 0.f)
//     else Shader("secondPass").setUniformf("u_depth", 1.f)
//   }
//   override def animate(dt:Float){

//     live.animate(dt)
//     // Kinect.animate(dt)
//     // MatrixStack.worldPose.rotate(0,dw,0)
//     tree.animate(dt)
//   }

//   override def audioIO( in:Array[Float], out:Array[Array[Float]], numOut:Int, numSamples:Int){
//     var sum = 0.f
//     for( i<-(0 until numSamples)){ //Audio.bufferSize)){
//       sum += in(i)*in(i) //Audio.in(i)*Audio.in(i)
//       rms = math.sqrt( sum / numSamples ).toFloat
//     }
//     //println( rms )
//   }
// }





Script