

import com.fishuyo.seer._
import graphics._
import dynamic._
import maths._
import spatial._
import trees._
import io._
import io.kinect._
import util._

import concurrent.duration._

import collection.mutable.ListBuffer

object Script extends SeerScript {

	var tree = new Tree()
	TreeNode.model.material = Material.basic

	tree.branch()

  var ground:Model = _ //Model(Plane.generateMesh(100,100,100,100,Quat.up))

	val skyblue = Vec3(68.f/256,122.f/256,222.f/256)
	val skypink = Vec3(.7f,0,.3f)
	val sky = Vec3(0.1)

	Schedule.clear
	Schedule.cycle(60 seconds){ 
		case t if t < 0.25f => sky.lerpTo(skyblue,0.01f)
		case t if t < 0.5f => ()
		case t if t < 0.75f => sky.lerpTo(skypink,0.01f)
		case t => sky.lerpTo(Vec3(0.1), 0.01f)
	}

	override def draw(){
		FPS.print
		if( ground == null){
			ground = Obj("res/landscapealien.obj")
		  ground.material = Material.specular
		  ground.pose.quat.set(0.42112392f,-0.09659095f, 0.18010217f, -0.8836787f)
		  ground.pose.pos.set(0.f,-.5f,-.0f)
		  ground.scale.set(5.f)
		  ground.shader = "test"

			TreeNode.model.material.loadTexture("res/mond.png")

		  // ground.mesh.recalculateNormals
		}
		ground.draw
		tree.draw
	}
	override def animate(dt:Float){
		Shader.bg.set(RGBA(sky,1.f))
		tree.animate(dt)
	}

}





Script