
package com.fishuyo.seer

import graphics._
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20

class RDNode extends RenderNode {
	var isTarget2 = true
	var buffer2:Option[FrameBuffer] = None

	shader = "rd"

	outputTo(this)

	scene.push(Plane())

	override def createBuffer(){
    if(buffer.isEmpty){ 
    	buffer = Some(FrameBuffer(viewport.w.toInt, viewport.h.toInt))
    	buffer2 = Some(FrameBuffer(viewport.w.toInt, viewport.h.toInt))
    }
	}
	override def bindBuffer(i:Int) = {
		if( isTarget2 ) buffer.get.getColorBufferTexture().bind(i)
		else buffer2.get.getColorBufferTexture().bind(i)
	}
	override def resize(vp:Viewport){
    viewport = vp
    if(camera.viewportHeight == 1.f){
      camera.viewportWidth = vp.aspect
    }else{
      // camera.viewportWidth = vp.w
      // camera.viewportHeight = vp.h
    }

    if(buffer.isDefined){
      buffer.get.dispose
      buffer = Some(FrameBuffer(vp.w.toInt,vp.h.toInt))
      buffer2.get.dispose
      buffer2 = Some(FrameBuffer(vp.w.toInt,vp.h.toInt))
    }
  }

  override def bindTarget(){
    if( buffer.isDefined ){
      
      if(isTarget2) buffer2.get.begin()
      else buffer.get.begin()

      if( clear ) Gdx.gl.glClear( GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT)
      else Gdx.gl.glClear( GL20.GL_DEPTH_BUFFER_BIT)

      nodes.foreach(_.render()) //hacky
    }
  }
  override def unbindTarget(){
  	if(isTarget2) buffer2.get.end()
    else buffer.get.end()
    isTarget2 = !isTarget2
  }

}
