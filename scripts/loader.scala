
import com.fishuyo.seer._
import dynamic._
import io._
import collection.mutable.ListBuffer

object Loader extends SeerScript {
	var loaders = ListBuffer[SeerScriptLoader]()
	loaders += new SeerScriptLoader("scripts/onetree.scala")
	loaders += new SeerScriptLoader("scripts/rd.scala")
	// var loader:SeerScriptLoader = new SeerScriptLoader("scripts/rd.scala")
	// var loader:SeerScriptLoader = new SeerScriptLoader("scripts/rd.scala")

	override def onUnload(){
		loaders.foreach( _.unload )
	}


	// def load(s:String){
	// 	if(loader != null) loader.unload
	// 	loader = new SeerScriptLoader(s)
	// }

	// Keyboard.clear
	// Keyboard.use
	// Keyboard.bind("1", ()=>{load("scripts/onetree.scala")}) 
	// Keyboard.bind("2", ()=>{load("scripts/rd.scala")} )
	// Keyboard.bind("3", ()=>{load("scripts/kinecttest.scala")} )
	// Keyboard.bind("6", ()=>{println("6")})

}
Loader