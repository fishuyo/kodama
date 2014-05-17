
import com.fishuyo.seer._
import graphics._
import dynamic._
import io._
import io.kinect._

import com.badlogic.gdx.graphics.{Texture => GdxTexture}
import com.badlogic.gdx.graphics.Pixmap

object Script extends SeerScript {


	Kinect.connect()
	Kinect.startVideo
	Kinect.startDepth

	Kinect.setAngle(0)

	val quad1 = Plane().scale(1,-480.f/640.f,1).translate(-1,0,0)
	val quad2 = Plane().scale(1,-480.f/640.f,1).translate(1,0,0)

  val dpix = new Pixmap(640,480, Pixmap.Format.RGBA8888)
  val vpix = new Pixmap(640,480, Pixmap.Format.RGB888)
  var tex1:GdxTexture = _
  var tex2:GdxTexture = _

	override def draw(){
		FPS.print
		if(tex1 == null){ 
			tex1 = new GdxTexture(dpix)
			tex2 = new GdxTexture(vpix)
			quad1.material = Material.basic
			quad1.material.texture = Some(tex1)
			quad1.material.textureMix = 1.f
			quad2.material = Material.basic
			quad2.material.texture = Some(tex2)
			quad2.material.textureMix = 1.f
		}
		quad1.draw
		quad2.draw
	}

	override def animate(dt:Float){
		if( tex1 != null ) tex1.draw(Kinect.depthPix,0,0)
		if( tex2 != null ) tex2.draw(Kinect.videoPix,0,0)
	}

}

Script