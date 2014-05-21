
package com.fishuyo.seer

import graphics._
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20

class RDNode extends RenderNode {
	var isTarget2 = true
	var buffer1:Option[FloatFrameBuffer] = None
	var buffer2:Option[FloatFrameBuffer] = None

	shader = "rd"

	outputTo(this)

	scene.push(Plane())

	override def createBuffer(){
    if(buffer1.isEmpty){ 
    	buffer1 = Some(new FloatFrameBuffer(viewport.w.toInt, viewport.h.toInt))
    	buffer2 = Some(new FloatFrameBuffer(viewport.w.toInt, viewport.h.toInt))
    }
	}
	override def bindBuffer(i:Int) = {
		if( isTarget2 ) buffer1.get.getColorBufferTexture().bind(i)
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

    if(buffer1.isDefined){
      buffer1.get.dispose
      buffer1 = Some(new FloatFrameBuffer(vp.w.toInt,vp.h.toInt))
      buffer2.get.dispose
      buffer2 = Some(new FloatFrameBuffer(vp.w.toInt,vp.h.toInt))
    }
  }

  override def bindTarget(){
    if( buffer1.isDefined ){
      
      if(isTarget2) buffer2.get.begin()
      else buffer1.get.begin()

      if( clear ) Gdx.gl.glClear( GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT)
      else Gdx.gl.glClear( GL20.GL_DEPTH_BUFFER_BIT)

      nodes.foreach(_.render()) //hacky
    }
  }
  override def unbindTarget(){
  	if(isTarget2) buffer2.get.end()
    else buffer1.get.end()
    isTarget2 = !isTarget2
  }

}
