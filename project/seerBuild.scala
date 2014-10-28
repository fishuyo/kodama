import sbt._

import Keys._

import com.typesafe.sbt.SbtNativePackager._

import seer.unmanaged._


object BuildSettings {
  val defaults = Defaults.defaultSettings ++ Seq(
    organization := "fttttm",
    version := "0.1",
    scalaVersion := "2.10.2",
    exportJars := true,
    autoCompilerPlugins := true,
    scalacOptions += "-Xexperimental",

    SeerLibs.updateGdxTask,
    SeerLibs.downloadTask

    // scalaSource in Compile := baseDirectory.value / "src",
    // resourceDirectory in Compile := baseDirectory.value / "resources"
  )

  lazy val app = packageArchetype.java_application ++ BuildSettings.defaults ++ Seq (
    fork in run := true,
    // javaOptions in run += "-Djava.library.path=.;./lib;/usr/local/lib", 
    javaOptions in run <<= (fullClasspath in Compile) map { (cp) => 
       val cpString = cp.map(_.data).mkString(System.getProperty("path.separator"))
       Seq("-cp",cpString)
    },
    connectInput in run := true,
    cancelable := true,
    outputStrategy := Some(StdoutOutput)
  )
}
  
object SeerProject {
  def apply(id:String, base:File, settings:Seq[Project.Setting[_]] = BuildSettings.defaults) = Project(id = id, base = base, settings = settings)
}

object SeerBuild extends Build {


  // core
  lazy val seer_core = SeerProject (
    id = "seer-core",
    base = file("seer/seer-core")
  )

  lazy val seer_gdx = SeerProject (
    id = "seer-gdx",
    base = file("seer/seer-gdx")
  ) dependsOn seer_core

  lazy val seer_gdx_desktop_app = SeerProject (
    id = "seer-gdx-desktop-app",
    base = file("seer/seer-gdx/seer-gdx-desktop-app")
  ) dependsOn (seer_gdx, seer_repl, seer_script)



  // examples
  lazy val examples = Project (
    "examples",
    file("seer/examples"),
    settings = BuildSettings.app
  ) dependsOn( seer_gdx_desktop_app, seer_multitouch  )


    // interaction
  lazy val seer_kinect = SeerProject (
    "seer-kinect",
    file("seer/seer-modules/seer-kinect")
  ) dependsOn( seer_core, seer_opencv )

  lazy val seer_leap = SeerProject (
    "seer-leap",
    file("seer/seer-modules/seer-leap")
  ) dependsOn seer_core

  lazy val seer_multitouch = SeerProject (
    "seer-multitouch",
    file("seer/seer-modules/seer-multitouch")
  ) dependsOn seer_core

  lazy val seer_vrpn = SeerProject ( // TODO get vrpn dependency..
    "seer-vrpn",
    file("seer/seer-modules/seer-vrpn")
  ) dependsOn seer_core


  // image video computer vision
  lazy val seer_opencv = SeerProject (
    "seer-opencv",
    file("seer/seer-modules/seer-opencv")
  ) dependsOn( seer_core, seer_video )

  lazy val seer_video = SeerProject (
    "seer-video",
    file("seer/seer-modules/seer-video")
  ) dependsOn seer_gdx 


  // audio
  lazy val seer_portaudio = SeerProject (
    "seer-portaudio",
    file("seer/seer-modules/seer-portaudio")
  ) dependsOn seer_core


  // dynamic and livecoding related
  lazy val seer_jruby = SeerProject (
    "seer-jruby",
    file("seer/seer-modules/seer-dynamic/seer-jruby")
  ) dependsOn seer_core

  lazy val seer_luaj = SeerProject (
    "seer-luaj",
    file("seer/seer-modules/seer-dynamic/seer-luaj")
  ) dependsOn seer_core

  lazy val seer_script = SeerProject (
    "seer-eval",
    file("seer/seer-modules/seer-dynamic/seer-eval")
  ) dependsOn( seer_core )

  lazy val seer_repl = SeerProject (
    "seer-repl",
    file("seer/seer-modules/seer-dynamic/seer-repl")
  ) dependsOn seer_core 


  // allosphere related
  lazy val seer_allosphere = SeerProject (
    id = "seer-allosphere",
    base = file("seer/seer-modules/seer-allosphere"),
    settings = BuildSettings.app
  ) dependsOn ( seer_gdx_desktop_app, seer_luaj, seer_script )

}


