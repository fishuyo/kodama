
import com.fishuyo.seer._
import graphics._
import dynamic._
import io._
import openni._

import com.badlogic.gdx.graphics.{Texture => GdxTexture}
import com.badlogic.gdx.graphics.Pixmap

import org.openni._
import java.nio.ShortBuffer
import java.nio.ByteBuffer

object Script extends SeerScript {

	// OpenNI.disconnect()
	OpenNI.connect()
	val context = OpenNI.context

	println(OpenNI.depthMD.getFullXRes)


	val quad1 = Plane().scale(1,-480.f/640.f,1).translate(-1,0,0)
	val quad2 = Plane().scale(1,-480.f/640.f,1).translate(1,0,0)
  val dpix = new Pixmap(640,480, Pixmap.Format.RGB888)
  val vpix = new Pixmap(640,480, Pixmap.Format.RGB888)
  var tex1:GdxTexture = _
  var tex2:GdxTexture = _

	override def draw(){
		FPS.print
		OpenNI.updateDepth()

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
		if( tex1 != null ){ 
		// 	tex1.draw(Kinect.depthPix,0,0)
		//   tex2.draw(Kinect.videoPix,0,0)
			val bb = dpix.getPixels
			bb.put(OpenNI.imgbytes)
			bb.rewind
			tex1.draw(dpix,0,0)
		}
	}
	override def onUnload(){
		context.release
	}

	val colors = RGB(1,0,0) :: RGB(0,1,0) :: RGB(0,0,1) :: RGB(1,1,0) :: RGB(0,1,1) :: RGB(1,1,1) :: List()
  val imgbytes = Array.fill(640*480*3)(255.toByte)
  def updateDepth(){
  	if( OpenNI.context == null) return
    try {
      OpenNI.context.waitNoneUpdateAll();

      val depthMD = OpenNI.depthGen.getMetaData();
      val sceneMD = OpenNI.userGen.getUserPixels(0);

      val scene = sceneMD.getData().createShortBuffer();
      val depth = depthMD.getData().createShortBuffer();
      OpenNI.calcHist(depth);
      depth.rewind();
        
      while(depth.remaining() > 0)
      {
        val pos = depth.position();
        val pixel = depth.get();
        val user = scene.get();
            
    		this.imgbytes(3*pos) = 0;
    		this.imgbytes(3*pos+1) = 0;
    		this.imgbytes(3*pos+2) = 0;                	

    		val drawBackground = false
        if (drawBackground || pixel != 0)
        {
        	var c = user % (colors.length-1);
        	if (user == 0)
        	{
        		c = colors.length-1;
        	}
        	if (pixel != 0)
        	{
        		val histValue = OpenNI.histogram(pixel);
        		this.imgbytes(3*pos) = (colors(c).r*histValue*255).toByte 
        		this.imgbytes(3*pos+1) = (colors(c).g*histValue*255).toByte
        		this.imgbytes(3*pos+2) = (colors(c).b*histValue*255).toByte
        	}
        }
      }
    } catch { case e:Exception => e.printStackTrace(); }
  }

}

// class NewUserObserver extends IObserver[UserEventArgs]{
// 	override def update( observable:IObservable[UserEventArgs], args:UserEventArgs){
// 		val sk = Script.skeletonCap
// 		println("New user " + args.getId() + " pose: " + sk.needPoseForCalibration() );
// 		sk.requestSkeletonCalibration(args.getId(), true);
// 	}
// }
// class LostUserObserver extends IObserver[UserEventArgs]{
// 	override def update( observable:IObservable[UserEventArgs], args:UserEventArgs){
// 		println("Lost user " + args.getId());
// 	}
// }

// class CalibrationObserver extends IObserver[CalibrationProgressEventArgs]{
// 	override def update( observable:IObservable[CalibrationProgressEventArgs], args:CalibrationProgressEventArgs){
// 		println("Calibration complete " + args.getStatus());

// 		if (args.getStatus() == CalibrationProgressStatus.OK){
// 			println("starting tracking "  +args.getUser());
// 			Script.skeletonCap.startTracking(args.getUser());
//       // joints.put(new Integer(args.getUser()), new HashMap<SkeletonJoint, SkeletonJointPosition>());
// 		} else if (args.getStatus() != CalibrationProgressStatus.MANUAL_ABORT){
// 			Script.skeletonCap.requestSkeletonCalibration(args.getUser(), true);
// 		}
// 	}
// }

Script