
// package com.fishuyo.seer 
// package openni

// import graphics._
// import spatial._
// import util._

// // import scala.collection.JavaConversions._
// // import scala.collection.mutable.ArrayBuffer
// import scala.collection.mutable.HashMap
// import scala.collection.mutable.Map
// import scala.collection.mutable.ListBuffer

// import java.nio._

// import org.openni._
// // import com.primesense.nite._


// object OpenNI {

//   var connected = false
// 	var context:Context = _

//   var depthGen:DepthGenerator = _
//   var depthMD:DepthMetaData = _
//   var imageGen:ImageGenerator = _
// 	var imageMD:ImageMetaData = _

// 	var userGen:UserGenerator = _
// 	var skeletonCap:SkeletonCapability = _
// 	var poseDetectionCap:PoseDetectionCapability = _

//   val tracking = HashMap[Int,Boolean]()

//   import SkeletonJoint._
  
//   val colors = RGB(1,0,0) :: RGB(0,1,0) :: RGB(0,0,1) :: RGB(1,1,0) :: RGB(0,1,1) :: RGB(1,0,1) :: RGB(1,1,1) :: List()
//   // val colors = RGB(1,1,1) :: RGB(0.7,0.,0.1) :: RGB(0.,.7,.5) :: RGB(.5,.5,.7) :: RGB(1,1,0) :: RGB(0,1,1) :: RGB(1,0,1) :: RGB(1,1,1) :: List()

//   val skeletons = HashMap[Int,TriangleMan]()
//   for( i <- 1 to 4 ){ 
//     skeletons(i) = new TriangleMan(i)
//     skeletons(i).setColor(colors(i))
//   }
//   // val joints = HashMap[Int,HashMap[String,Vec3]]()
//   // val vel = HashMap[Int,HashMap[String,Vec3]]()
//   val s2j = HashMap[String,SkeletonJoint](
//     "head" -> HEAD,
//     "neck" -> NECK,
//     "torso" -> TORSO,
//     "waist" -> WAIST,
//     "lcollar" -> LEFT_COLLAR,
//     "lshoulder" -> LEFT_SHOULDER,
//     "lelbow" -> LEFT_ELBOW,
//     "lwrist" -> LEFT_WRIST,
//     "lhand" -> LEFT_HAND,
//     "lfingers" -> LEFT_FINGER_TIP,
//     "rcollar" -> RIGHT_COLLAR,
//     "rshoulder" -> RIGHT_SHOULDER,
//     "relbow" -> RIGHT_ELBOW,
//     "rwrist" -> RIGHT_WRIST,
//     "rhand" -> RIGHT_HAND,
//     "rfingers" -> RIGHT_FINGER_TIP,
//     "lhip" -> LEFT_HIP,
//     "lknee" -> LEFT_KNEE,
//     "lankle" -> LEFT_ANKLE,
//     "lfoot" -> LEFT_FOOT,
//     "rhip" -> RIGHT_HIP,
//     "rknee" -> RIGHT_KNEE,
//     "rankle" -> RIGHT_ANKLE,
//     "rfoot" -> RIGHT_FOOT
//   )

// 	def connect(){
//     if(connected) return
// 		try{
// 			context = new Context
	 		
//       depthGen = DepthGenerator.create(context)
//       depthMD = depthGen.getMetaData()

//       imageGen = ImageGenerator.create(context)
// 			imageMD = imageGen.getMetaData()

// 			userGen = UserGenerator.create(context)
// 			skeletonCap = userGen.getSkeletonCapability()
// 		  poseDetectionCap = userGen.getPoseDetectionCapability()

// 			userGen.getNewUserEvent().addObserver(new NewUserObserver())
// 			userGen.getLostUserEvent().addObserver(new LostUserObserver())
		
// 			skeletonCap.getCalibrationCompleteEvent().addObserver(new CalibrationObserver());

// 			skeletonCap.setSkeletonProfile(SkeletonProfile.ALL);
// 			context.startGeneratingAll()
//       connected = true
// 		} catch { case e:Exception => println(e)}
// 	}

// 	def disconnect(){
// 		// userGen.getNewUserEvent().deleteObservers
// 		// userGen.getLostUserEvent().deleteObservers
// 		// skeletonCap.getCalibrationCompleteEvent().deleteObservers
// 		if(context != null) context.release
//     connected = false
// 	}



//   val histogram = new Array[Float](10000)
//   def calcHist(depth:ShortBuffer){
//     // reset
//     for (i <- 0 until histogram.length)
//       histogram(i) = 0
        
//     depth.rewind()

//     var points = 0;
//     while(depth.remaining() > 0)
//     {
//       val depthVal = depth.get();
//       if (depthVal != 0)
//       {
//         histogram(depthVal) += 1
//         points += 1
//       }
//     }
        
//     for (i <- 1 until histogram.length)
//     {
//       histogram(i) += histogram(i-1);
//     }

//     if (points > 0)
//     {
//       for (i <- 1 until histogram.length)
//       {
//         histogram(i) = 1.0f - (histogram(i) / points.toFloat)
//       }
//     }
//   }

//   val imgbytes = Array.fill(640*480*4)(255.toByte)
//   val rgbbytes = Array.fill(640*480*4)(255.toByte)
//   def updateDepth(){
//   	if( context == null) return
//     try {
//       context.waitNoneUpdateAll();

//       val depthMD = depthGen.getMetaData();
//       val imageMD = imageGen.getMetaData();
//       val sceneMD = userGen.getUserPixels(0);

//       val scene = sceneMD.getData().createShortBuffer();
//       val image = imageMD.getData().createByteBuffer();
//       val depth = depthMD.getData().createShortBuffer();
//       calcHist(depth);
//       depth.rewind();
        
//       while(depth.remaining() > 0)
//       {
//         val pos = depth.position();
//         val pixel = depth.get();
//         val user = scene.get();
            
//         imgbytes(4*pos) = 0;
//         imgbytes(4*pos+1) = 0;
//         imgbytes(4*pos+2) = 0;
//         imgbytes(4*pos+3) = 0;

//         rgbbytes(4*pos) = image.get()
//     		rgbbytes(4*pos+1) = image.get()
//         rgbbytes(4*pos+2) = image.get()                  
//     		rgbbytes(4*pos+3) = 255.toByte                	

//     		val drawBackground = false
//         if (drawBackground || pixel != 0)
//         {
//         	var c = user % (colors.length-1);
//         	if (user == 0)
//         	{
//         		c = colors.length-1;
//         	}
//         	if (pixel != 0)
//         	{
//         		val histValue = histogram(pixel);
//         		imgbytes(4*pos) = (colors(c).r*histValue*255).toByte 
//         		imgbytes(4*pos+1) = (colors(c).g*histValue*255).toByte
//             imgbytes(4*pos+2) = (colors(c).b*histValue*255).toByte
//         		imgbytes(4*pos+3) = 255.toByte
//         	}
//         }
//       }
//     } catch { case e:Exception => e.printStackTrace(); }
//   }

//   def getJoints(user:Int){
//     getJoint(user,"head")
//     getJoint(user,"neck")
//     getJoint(user,"torso")
//     getJoint(user,"lshoulder")
//     getJoint(user,"lelbow")
//     getJoint(user,"lhand")
//     getJoint(user,"rshoulder")
//     getJoint(user,"relbow")
//     getJoint(user,"rhand")
//     getJoint(user,"lhip")
//     getJoint(user,"lknee")
//     getJoint(user,"lfoot")
//     getJoint(user,"rhip")
//     getJoint(user,"rknee")
//     getJoint(user,"rfoot")
//   }
//   def getJoint(user:Int, joint:String) = {
//     val jpos = skeletonCap.getSkeletonJointPosition(user, s2j(joint))
//     val p = jpos.getPosition
//     val x = -p.getX / 1000.f
//     val y = p.getY / 1000.f + 1.f
//     val z = -p.getZ / 1000.f
//     val v = Vec3(x,y,z)
//     skeletons(user).updateJoint(joint,v)
//     (v, jpos.getConfidence )
//   }

// }

// class NewUserObserver extends IObserver[UserEventArgs]{
// 	override def update( observable:IObservable[UserEventArgs], args:UserEventArgs){
// 		val sk = OpenNI.skeletonCap
//     val id = args.getId
// 		println("New user " + id + " pose: " + sk.needPoseForCalibration() );
// 		sk.requestSkeletonCalibration(id, true);
//     OpenNI.skeletons.getOrElseUpdate(id, new TriangleMan(id)).calibrating = true
// 	}
// }
// class LostUserObserver extends IObserver[UserEventArgs]{
// 	override def update( observable:IObservable[UserEventArgs], args:UserEventArgs){
//     val id = args.getId
// 		println("Lost user " + id);
//     OpenNI.tracking(id) = false
//     OpenNI.skeletons.getOrElseUpdate(id, new TriangleMan(id)).tracking = false
//     OpenNI.skeletons(id).calibrating = false
// 	}
// }

// class CalibrationObserver extends IObserver[CalibrationProgressEventArgs]{
// 	override def update( observable:IObservable[CalibrationProgressEventArgs], args:CalibrationProgressEventArgs){
// 		println("Calibration complete " + args.getStatus());

// 		if (args.getStatus() == CalibrationProgressStatus.OK){
//       val id = args.getUser
// 			println("starting tracking "  + id);
// 			OpenNI.skeletonCap.startTracking(id);
//       OpenNI.skeletons.getOrElseUpdate(id, new TriangleMan(id)).calibrating = false
//       OpenNI.skeletons(id).randomizeIndices
//       OpenNI.skeletons(id).tracking = true

//       OpenNI.tracking(id) = true
// 		} else if (args.getStatus() != CalibrationProgressStatus.MANUAL_ABORT){
// 			OpenNI.skeletonCap.requestSkeletonCalibration(args.getUser(), true);
// 		}
// 	}
// }

// object Bone {
//   def apply() = new Bone(Vec3(),Quat(),0.f)
//   def apply(p:Vec3,q:Quat,l:Float) = new Bone(p,q,l)
// }
// class Bone( var pos:Vec3, var quat:Quat, var length:Float)


// class Skeleton(val id:Int) extends Animatable {

//   val color = RGB(1,1,1)
//   var calibrating = false
//   var tracking = false
//   var droppedFrames = 0

//   var joints = HashMap[String,Vec3]()
//   var vel = HashMap[String,Vec3]()

//   var bones = ListBuffer[Bone]()
//   for( i <- (0 until 8)) bones += Bone()


//   def setJoints(s:Skeleton){
//     joints = s.joints.clone
//     droppedFrames = 0
//   }

//   def updateJoint(s:String,pos:Vec3){
//     val oldpos = joints.getOrElseUpdate(s,pos)
//     vel(s) = pos - oldpos
//     joints(s) = pos
//     droppedFrames = 0
//   }

//   def updateBones(){
//     bones(0).pos.set(joints("lshoulder"))
//     var a = joints("lelbow") - joints("lshoulder")
//     bones(0).length = a.mag()
//     bones(0).quat = Quat().getRotationTo(Vec3(0,0,1), a.normalized)

//     bones(1).pos.set(joints("lelbow"))
//     a = joints("lhand") - joints("lelbow")
//     bones(1).length = a.mag()
//     bones(1).quat = Quat().getRotationTo(Vec3(0,0,1), a.normalized)

//     bones(2).pos.set(joints("rshoulder"))
//     a = joints("relbow") - joints("rshoulder")
//     bones(2).length = a.mag()
//     bones(2).quat = Quat().getRotationTo(Vec3(0,0,1), a.normalized)

//     bones(3).pos.set(joints("relbow"))
//     a = joints("rhand") - joints("relbow")
//     bones(3).length = a.mag()
//     bones(3).quat = Quat().getRotationTo(Vec3(0,0,1), a.normalized)

//     bones(4).pos.set(joints("lhip"))
//     a = joints("lknee") - joints("lhip")
//     bones(4).length = a.mag()
//     bones(4).quat = Quat().getRotationTo(Vec3(0,0,1), a.normalized)

//     bones(5).pos.set(joints("lknee"))
//     a = joints("lfoot") - joints("lknee")
//     bones(5).length = a.mag()
//     bones(5).quat = Quat().getRotationTo(Vec3(0,0,1), a.normalized)

//     bones(6).pos.set(joints("rhip"))
//     a = joints("rknee") - joints("rhip")
//     bones(6).length = a.mag()
//     bones(6).quat = Quat().getRotationTo(Vec3(0,0,1), a.normalized)

//     bones(7).pos.set(joints("rknee"))
//     a = joints("rfoot") - joints("rknee")
//     bones(7).length = a.mag()
//     bones(7).quat = Quat().getRotationTo(Vec3(0,0,1), a.normalized)
//   }

// }

// class StickMan(override val id:Int) extends Skeleton(id) {

//   val loadingModel = Cube().scale(0.1f).translate(0,0.5f,0)
//   val m = Cube().rotate(45.f.toRadians,0,45.f.toRadians)
//   loadingModel.addPrimitive(m)
//   m.material.color = color
//   loadingModel.material.color = color

//   var jointModels = Map[String,Model]()

//   jointModels += "head" -> Sphere().scale(.05f,.065f,.05f)
//   jointModels += "neck" -> Sphere().scale(.02f)
//   jointModels += "torso" -> Sphere().scale(.07f,.10f,.05f)
//   jointModels += "rshoulder" -> Sphere().scale(.02f)
//   jointModels += "relbow" -> Sphere().scale(.02f)
//   jointModels += "rhand" -> Sphere().scale(.02f)
//   jointModels += "lshoulder" -> Sphere().scale(.02f)
//   jointModels += "lelbow" -> Sphere().scale(.02f)
//   jointModels += "lhand" -> Sphere().scale(.02f)
//   jointModels += "rhip" -> Sphere().scale(.03f)
//   jointModels += "rknee" -> Sphere().scale(.02f)
//   jointModels += "rfoot" -> Sphere().scale(.02f)
//   jointModels += "lhip" -> Sphere().scale(.03f)
//   jointModels += "lknee" -> Sphere().scale(.02f)
//   jointModels += "lfoot" -> Sphere().scale(.02f)
  
//   jointModels.values.foreach( (m) => {
//     m.material = Material.basic
//     m.material.color = color 
//   })

//   val boneModels = new ListBuffer[Model]()
//   for( i <- (0 until 8)) boneModels += Cylinder()
//   boneModels.foreach( (b) => {
//     b.material = Material.basic
//     b.material.color = color
//     b.scale.set(.015f,.015f,.15f) 
//   })

//   def setShader(s:String){
//     jointModels.values.foreach(_.shader = s)
//     boneModels.foreach(_.shader = s)
//   }

//   override def draw(){
//     if(calibrating) loadingModel.draw()
//     if(tracking){ 
//       jointModels.values.foreach(_.draw())
//       boneModels.foreach(_.draw())
//     }
//   }

//   override def animate(dt:Float){
//     droppedFrames += 1
//     loadingModel.rotate(0,0.10f,0)
//     updateBones()

//     jointModels.foreach{ case(name,m) => 
//       m.pose.pos.set( joints(name) )
//     }
//     boneModels.zip(bones).foreach{ case (m,b) =>
//       m.pose.pos.set( b.pos )
//       m.pose.quat.set( b.quat )
//       m.scale.z = b.length
//     }
//   }

//   def setColor(c:RGBA){
//     color.set(c)
//     loadingModel.material.color = c
//     m.material.color = c
//     // joints.values.foreach( _.material.color = color)
//     boneModels.foreach( _.material.color = color)
//   }
// }

// class QuadMan(override val id:Int) extends Skeleton(id) {

//   var jointModels = Map[String,Model]()
//   jointModels += "head" -> Plane().scale(.05f,.065f,.05f)
//   jointModels += "neck" -> Plane().scale(.02f)
//   jointModels += "torso" -> Plane().scale(.07f,.10f,.05f)
//   jointModels += "rshoulder" -> Plane().scale(.02f)
//   jointModels += "relbow" -> Plane().scale(.02f)
//   jointModels += "rhand" -> Plane().scale(.02f)
//   jointModels += "lshoulder" -> Plane().scale(.02f)
//   jointModels += "lelbow" -> Plane().scale(.02f)
//   jointModels += "lhand" -> Plane().scale(.02f)
//   jointModels += "rhip" -> Plane().scale(.03f)
//   jointModels += "rknee" -> Plane().scale(.02f)
//   jointModels += "rfoot" -> Plane().scale(.02f)
//   jointModels += "lhip" -> Plane().scale(.03f)
//   jointModels += "lknee" -> Plane().scale(.02f)
//   jointModels += "lfoot" -> Plane().scale(.02f)
  
//   jointModels.values.foreach( (m) => {
//     m.material = Material.basic
//     m.material.color = color
//     m.shader = "s1"
//   })

//   val boneModels = new ListBuffer[Model]()
//   for( i <- (0 until 8)) boneModels += Cylinder() //Plane()
//   boneModels.foreach( (b) => {
//     b.material = Material.basic
//     b.material.color = color
//     b.shader = "s1"
//     b.scale.set(.015f,.015f,.15f) 
//     // b.scale.set(.5f) 
//   })

//   def setShader(s:String){
//     jointModels.values.foreach(_.shader = s)
//     boneModels.foreach(_.shader = s)
//   }

//   override def draw(){
//     if(tracking){ 
//       jointModels.values.foreach(_.draw())
//       boneModels.foreach(_.draw())
//     }
//   }

//   override def animate(dt:Float){
//     droppedFrames += 1
//     updateBones()

//     jointModels.foreach{ case(name,m) => 
//       m.pose.pos.set( joints(name) )
//     }    
//     boneModels.zip(bones).foreach{ case (m,b) =>
//       m.pose.pos.set( b.pos )
//       m.pose.quat.set( b.quat )
//       m.scale.z = b.length
//     }
//   }

//   def setColor(c:RGBA){
//     color.set(c)
//     // joints.values.foreach( _.material.color = color)
//     boneModels.foreach( _.material.color = color)
//   }
// }


// class TriangleMan(override val id:Int) extends Skeleton(id) {

//   val mesh = new Mesh()
//   mesh.primitive = Triangles
//   mesh.maxVertices = 100
//   mesh.maxIndices = 500

//   val linemesh = new Mesh()
//   linemesh.primitive = Lines
//   linemesh.maxVertices = 100
//   linemesh.maxIndices = 100
//   val linemodel = Model(linemesh)
//   linemodel.shader = "bone"

//   val lineindices = Array[Int](13,11,11,6,6,12,12,14,11,0,0,2,2,9,6,5,11,5,0,5,5,1,5,10,1,10,1,3,3,7,10,8,8,4)


//   val model = Model(mesh)  
//   model.material = Material.specular
//   model.material.color = color
//   // model.shader = "joint"

//   var jointModels = Map[String,Model]()
//   jointModels += "head" -> Plane().scale(.06f,.065f,.06f)
//   jointModels += "neck" -> Plane().scale(.02f)
//   jointModels += "torso" -> Plane().scale(.08f,.08f,.08f)
//   jointModels += "rshoulder" -> Plane().scale(.02f)
//   jointModels += "relbow" -> Plane().scale(.02f)
//   jointModels += "rhand" -> Plane().scale(.02f)
//   jointModels += "lshoulder" -> Plane().scale(.02f)
//   jointModels += "lelbow" -> Plane().scale(.02f)
//   jointModels += "lhand" -> Plane().scale(.02f)
//   jointModels += "rhip" -> Plane().scale(.03f)
//   jointModels += "rknee" -> Plane().scale(.02f)
//   jointModels += "rfoot" -> Plane().scale(.02f)
//   jointModels += "lhip" -> Plane().scale(.03f)
//   jointModels += "lknee" -> Plane().scale(.02f)
//   jointModels += "lfoot" -> Plane().scale(.02f)

//   jointModels.values.foreach( (m) => {
//     m.material = Material.specular
//     m.material.color = color
//     m.shader = "joint"
//   })

//   var phase = Map[String,Float]()
//   jointModels.keys.foreach((k) => { phase(k) = 2*Pi*Random.float() })
  
//   var indices = for( i <- 0 until 30; j <- 0 until 3) yield Random.int(0,15)()

//   def randomizeIndices(){
//     indices = for( i <- 0 until 30; j <- 0 until 3) yield Random.int(0,15)() 
//   }

//   override def draw(){
//     if(tracking){
//       model.draw()
//       linemodel.draw()
//       // jointModels.foreach{ case (k,m) => 
//       //   Shader("joint")
//       //   var sh = Shader.shader.get
//       //   sh.uniforms("phase") = phase(k)
//       //   m.draw()
//       // }
//     }
//   }

//   def drawJoints(){
//     if(tracking){
//       jointModels.foreach{ case (k,m) => 
//         Shader("joint")
//         var sh = Shader.shader.get
//         sh.uniforms("phase") = phase(k)
//         sh.uniforms("color") = m.material.color
//         m.draw()
//       }
//     }
//   }

//   override def animate(dt:Float){
//     droppedFrames += 1
//     updateBones()

//     jointModels.foreach{ case(name,m) => 
//       m.pose.pos.set( joints(name) )
//     } 

//     // val list = joints.values.toArray
//     // joints.zipWithIndex.foreach{ case((k,v),i) =>
//     //   println(s"$i $k : $v ${list(i)}")
//     // }

//     mesh.clear
//     // val vs = joints.values.toSeq
//     // for( i <- 0 until 9; j <- 0 until 3){
//       // mesh.vertices += Random.oneOf(vs : _*)()
//     // }
//     mesh.vertices ++= joints.values
//     // mesh.texCoords ++= joints.values.map( _.xy )
//     mesh.indices ++= indices
//     mesh.recalculateNormals()
//     mesh.update

//     linemesh.clear
//     linemesh.vertices ++= mesh.vertices
//     linemesh.indices ++= lineindices
//     linemesh.update
//   }

//   def setColor(c:RGBA){
//     color.set(c)
//     model.material.color.set(color)
//     jointModels.values.foreach(_.material.color.set(color))
//   }
// }
