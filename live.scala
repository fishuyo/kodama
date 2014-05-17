

import com.fishuyo.seer._
import graphics._
import dynamic._
import io._

object Script extends SeerScript {

	Mouse.clear
	Mouse.use

	override def draw(){
		FPS.print
	}
	override def animate(dt:Float){

		Shader("rd")
		val s = Shader.shader.get
		s.uniforms("brush") = Mouse.xy()
	}

}

Script