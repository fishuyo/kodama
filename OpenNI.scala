
package com.fishuyo.seer 
package openni

import graphics._

// import scala.collection.JavaConversions._
// import scala.collection.mutable.ArrayBuffer
// import scala.collection.mutable.Map

import java.nio._

import org.openni._
// import com.primesense.nite._


object OpenNI {

	var context:Context = _

	var depthGen:DepthGenerator = _
	var depthMD:DepthMetaData = _

	var userGen:UserGenerator = _
	var skeletonCap:SkeletonCapability = _
	var poseDetectionCap:PoseDetectionCapability = _


	def connect() = {
		try{
			context = new Context
	 		
	 		depthGen = DepthGenerator.create(context)
			depthMD = depthGen.getMetaData()

			userGen = UserGenerator.create(context)
			skeletonCap = userGen.getSkeletonCapability()
		  poseDetectionCap = userGen.getPoseDetectionCapability()

			userGen.getNewUserEvent().addObserver(new NewUserObserver())
			userGen.getLostUserEvent().addObserver(new LostUserObserver())
		
			skeletonCap.getCalibrationCompleteEvent().addObserver(new CalibrationObserver());

			skeletonCap.setSkeletonProfile(SkeletonProfile.ALL);
			context.startGeneratingAll()
		} catch { case e:Exception => println(e)}
	}

	def disconnect(){
		// userGen.getNewUserEvent().deleteObservers
		// userGen.getLostUserEvent().deleteObservers
		// skeletonCap.getCalibrationCompleteEvent().deleteObservers
		if(context != null) context.release
	}



val histogram = new Array[Float](10000)
def calcHist(depth:ShortBuffer){
    // reset
    for (i <- 0 until histogram.length)
      histogram(i) = 0
        
    depth.rewind()

    var points = 0;
    while(depth.remaining() > 0)
    {
      val depthVal = depth.get();
      if (depthVal != 0)
      {
        histogram(depthVal) += 1
        points += 1
      }
    }
        
    for (i <- 1 until histogram.length)
    {
      histogram(i) += histogram(i-1);
    }

    if (points > 0)
    {
      for (i <- 1 until histogram.length)
      {
        histogram(i) = 1.0f - (histogram(i) / points.toFloat)
      }
    }
  }

  val colors = RGB(1,0,0) :: RGB(0,1,0) :: RGB(0,0,1) :: RGB(1,1,0) :: RGB(0,1,1) :: RGB(1,1,1) :: List()
  val imgbytes = Array.fill(640*480*3)(255.toByte) //new Array[Byte](640*480*3)
  def updateDepth(){
  	if( context == null) return
    try {
      context.waitNoneUpdateAll();

      val depthMD = depthGen.getMetaData();
      val sceneMD = userGen.getUserPixels(0);

      val scene = sceneMD.getData().createShortBuffer();
      val depth = depthMD.getData().createShortBuffer();
      calcHist(depth);
      depth.rewind();
        
      while(depth.remaining() > 0)
      {
        val pos = depth.position();
        val pixel = depth.get();
        val user = scene.get();
            
    		imgbytes(3*pos) = 0;
    		imgbytes(3*pos+1) = 0;
    		imgbytes(3*pos+2) = 0;                	

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
        		val histValue = histogram(pixel);
        		imgbytes(3*pos) = (colors(c).r*histValue*255).toByte 
        		imgbytes(3*pos+1) = (colors(c).g*histValue*255).toByte
        		imgbytes(3*pos+2) = (colors(c).b*histValue*255).toByte
        	}
        }
      }
    } catch { case e:Exception => e.printStackTrace(); }
  }
}

class NewUserObserver extends IObserver[UserEventArgs]{
	override def update( observable:IObservable[UserEventArgs], args:UserEventArgs){
		val sk = OpenNI.skeletonCap
		println("New user " + args.getId() + " pose: " + sk.needPoseForCalibration() );
		sk.requestSkeletonCalibration(args.getId(), true);
	}
}
class LostUserObserver extends IObserver[UserEventArgs]{
	override def update( observable:IObservable[UserEventArgs], args:UserEventArgs){
		println("Lost user " + args.getId());
	}
}

class CalibrationObserver extends IObserver[CalibrationProgressEventArgs]{
	override def update( observable:IObservable[CalibrationProgressEventArgs], args:CalibrationProgressEventArgs){
		println("Calibration complete " + args.getStatus());

		if (args.getStatus() == CalibrationProgressStatus.OK){
			println("starting tracking "  +args.getUser());
			OpenNI.skeletonCap.startTracking(args.getUser());
      // joints.put(new Integer(args.getUser()), new HashMap<SkeletonJoint, SkeletonJointPosition>());
		} else if (args.getStatus() != CalibrationProgressStatus.MANUAL_ABORT){
			OpenNI.skeletonCap.requestSkeletonCalibration(args.getUser(), true);
		}
	}
}
