package com.zooth.icmm;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.utils.*;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.glutils.*;

public class Icmm extends ApplicationAdapter {
  Camera cam;
  ShaderProgram sp;
	
	@Override
	public void create () {
    cam = new PerspectiveCamera(60,1,1);
    cam.position.set(0,0,2);
    cam.up.set(Vector3.Y);
    cam.lookAt(0,0,0);
    sp = new ShaderProgram(Gdx.files.internal("vert.glsl"), Gdx.files.internal("frag.glsl"));
    Gdx.app.log("icmm", "shader log:"+sp.getLog());
	}

	@Override
	public void render () {
    // logic
    float dt = Gdx.graphics.getDeltaTime();
    { // controls
      float ls = 150f;//lookspeed
      float ms = 5f;//movespeed
      if (Gdx.input.isKeyPressed(Input.Keys.L)){
        cam.rotate(Vector3.Y, -dt*ls);
      }
      if (Gdx.input.isKeyPressed(Input.Keys.J)){
        cam.rotate(Vector3.Y, dt*ls);
      }
      if (Gdx.input.isKeyPressed(Input.Keys.I)){
        cam.position.add(cam.direction.cpy().scl(dt*ms));
      }
      if (Gdx.input.isKeyPressed(Input.Keys.K)){
        cam.position.add(cam.direction.cpy().scl(-dt*ms));
      }
    }
    // rendering
		Gdx.gl.glClearColor(0, 0, 0, 1);
    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
    //Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
    //Gdx.gl.glEnable(GL20.GL_BLEND);
    sp.begin();
    cam.update();
    sp.setUniformMatrix("u_projectionViewMatrix", cam.combined);
    sp.setUniformf("u_light", cam.position);
    // draw box:
    {
      int srtx = -5;
      int endx = 5;
      int srty = -5;
      int endy = 5;
      for (int x = srtx; x < endx; ++x)
      {
        for (int y = srty; y < endy; ++y)
        {
          {
            Mesh mesh = new Mesh(true, 4, 6,
              new VertexAttribute(VertexAttributes.Usage.Position, 3, "a_position"));
            mesh.setVertices(new float[]{
              x-.5f,-1,y-.5f,
              x-.5f,-1,y+.5f,
              x+.5f,-1,y+.5f,
              x+.5f,-1,y-.5f
            });
            mesh.setIndices(new short[]{
              1,2,3,
              3,1,0
            });
            mesh.render(sp, GL20.GL_TRIANGLES);
          }
          if (x==srtx){
            Mesh mesh = new Mesh(true, 4, 6,
              new VertexAttribute(VertexAttributes.Usage.Position, 3, "a_position"));
            mesh.setVertices(new float[]{
              x-.5f,-1,y-.5f,
              x-.5f,-1,y+.5f,
              x-.5f,10,y+.5f,
              x-.5f,10,y-.5f
            });
            mesh.setIndices(new short[]{
              1,2,3,
              3,1,0
            });
            mesh.render(sp, GL20.GL_TRIANGLES);
          }
        }
      }
    }
    sp.end();
	}
}
