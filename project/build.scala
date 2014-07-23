import sbt._

import Keys._


object TemplateBuild extends Build {

  lazy val project = SeerProject(
  	id = "kodama",
  	base = file("."),
  	settings = BuildSettings.app
  ) dependsOn( SeerBuild.seer_gdx_desktop_app,
  					SeerBuild.seer_multitouch,
  					SeerBuild.seer_script,
  					SeerBuild.seer_kinect,
  					SeerBuild.seer_vrpn,
  					SeerBuild.seer_video)
  
}
