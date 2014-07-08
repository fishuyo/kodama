
import com.fishuyo.seer._
import dynamic._
import io._
import collection.mutable.ListBuffer

object Loader extends SeerScript {
	var loaders = ListBuffer[SeerScriptLoader]()
	// loaders += new SeerScriptLoader("scripts/onetree.scala")
	// loaders += new SeerScriptLoader("scripts/ground.scala")
	// loaders += new SeerScriptLoader("scripts/ground_kinect.scala")
	// loaders += new SeerScriptLoader("scripts/rd.scala")
	// loaders += new SeerScriptLoader("scripts/rd_kinect.scala")
	// loaders += new SeerScriptLoader("scripts/dla.scala")
	// loaders += new SeerScriptLoader("scripts/dla_kinect.scala")
	// loaders += new SeerScriptLoader("scripts/kinecttest.scala")
	// loaders += new SeerScriptLoader("scripts/opennitest.scala")
	// loaders += new SeerScriptLoader("scripts/floor.scala")
	loaders += new SeerScriptLoader("scripts/test.scala")


	override def onUnload(){
		loaders.foreach( _.unload )
	}
}
Loader