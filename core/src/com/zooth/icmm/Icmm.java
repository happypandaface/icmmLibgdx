package com.zooth.icmm;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.utils.*;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.assets.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.glutils.*;

class Tile
{
  int x;
  int y;
  float z;
  Tile setPos(int x,int y){this.x=x;this.y=y;return this;}
  Tile setExists(boolean e){exists=e;return this;}
  boolean exists;
  static int OPEN = 0;
  static int TUNNEL = 1;
  int type = OPEN;
}
class Obj
{
  Obj(){};
  void init(){};
  Icmm game;
  void setGame(Icmm g){this.game=g;}
  String tex;
  String sel;
  String inv;
  boolean billboard;
  float angle;
  float renderDist;// used for render ordering
  boolean tall;
  boolean solid;
  boolean canTog;
  Vector3 pos=new Vector3();void setPos(int x,int y){pos=new Vector3(x,0,y);}
  Vector2 getPos(){return new Vector2(pos.x,pos.z);}
  void draw(ShaderProgram sp){// called when invoked
    if (tex!=null){
      game.ass.get(tex, Texture.class).bind();
      Matrix4 mat = new Matrix4();
      mat.translate(pos.x,0,pos.z);
      float ang = angle;
      if (billboard){
        Vector2 viewp = new Vector2(game.cam.position.x, game.cam.position.z); 
        Vector2 thisp = new Vector2(pos.x, pos.z); 
        ang = thisp.cpy().sub(viewp).angle();
      }
      mat.rotate(Vector3.Y, -ang+90);
      sp.setUniformMatrix("u_objectMatrix", mat);
      if (tall)
        game.inWorldTall.render(sp, GL20.GL_TRIANGLES);
      else
        game.inWorld.render(sp, GL20.GL_TRIANGLES);
    }
  }
  void step(float dt){// per fram
  }
  void act(){// called when invoked
  }
  boolean actf(float dt){//frames during action
    return true;// done with anim
  }
  void tog(){}
}
class Hand extends Obj
{
  Hand(){
    super();
    tex=null;
    sel="hand.png";
  }
  void act(){
    game.anim=this;
    actTime=0;
  }
  float actTime = 0;
  float actTimeMax = .25f;
  boolean actf(float dt){
    actTime+=dt;
    if(actTime<actTimeMax)
      sel="hand2.png";
    else{
      sel="hand.png";
      Vector2 thisp = getPos();
      float tarAngle = new Vector2(game.cam.direction.x, game.cam.direction.z).angle();
      float bestDist = 1.2f;
      Obj bestObj = null;
      for (int i = 0; i < game.objs.size; ++i){
        Obj o = game.objs.get(i);
        if (o!=this&&o.canTog){
          Vector2 diff = o.getPos().cpy().sub(thisp);
          if (diff.len() < bestDist&&Icmm.compAngle(diff.angle(),tarAngle,40)){
            bestDist=diff.len();
            bestObj=o;
          }
        }
      }
      if (bestObj != null)
        bestObj.tog();
      return true;
    }
    return false;
  }
}
class GasBall extends Obj
{
  GasBall(){
    tex="gasBall.png";
    billboard=true;
  }
  void step(float dt){
    Vector2 camp = new Vector2(game.cam.position.x, game.cam.position.z); 
    Vector2 diff = camp.sub(getPos());
    diff.nor().scl(dt);
    pos.add(diff.x, 0, diff.y);
    pos=game.rectify(pos);
  }
}
class Sign extends Obj
{
  Sign(){
    canTog=true;
    solid=true;
    tex="sign.png";
    billboard=true;
  }
  String msg="the sign is blank";
  void tog(){Gdx.app.log("icmm", msg);}
}
class Door extends Obj
{
  Door(){
    canTog=true;
    tall=true;
    solid=true;
    tex="doorC.png";
  }
  void tog(){solid=!solid;if(solid){tex="doorC.png";}else{tex="doorO.png";}}
}
public class Icmm extends ApplicationAdapter {
  static boolean compAngle(float ang1, float ang2, float degs){
    while (ang1>180){
      ang1-=360;
    }
    while (ang1<-180){
      ang1+=360;
    }
    while (ang2>180){
      ang2-=360;
    }
    while (ang2<-180){
      ang2+=360;
    }
    return (Math.abs(ang1-ang2)<degs);
  }
  Camera cam;
  Camera uicam;
  ShaderProgram sp;
  static AssetManager ass = new AssetManager(); 
  Mesh floor;
  Mesh wall;
  Mesh tunnelWall;
  Mesh uiitem;
  Mesh inWorld;
  Mesh inWorldTall;
  int srtx = 0;
  int endx = 100;
  int srty = 0;
  int endy = 100;
	
  float tgh=1.5f;// tunnel gap height
	@Override
	public void create () {
    ass.load("stone.png", Texture.class);
    ass.load("sign.png", Texture.class);
    ass.load("gasBall.png", Texture.class);
    ass.load("hand.png", Texture.class);
    ass.load("hand2.png", Texture.class);
    ass.load("doorC.png", Texture.class);
    ass.load("doorO.png", Texture.class);
    uicam = new OrthographicCamera(1,1);
    uicam.near = .01f;
    uicam.position.set(0,0,1);
    uicam.direction.set(0,0,-1);
    uicam.update();// only do this here?
    float ratio = (float)Gdx.graphics.getHeight()/(float)Gdx.graphics.getWidth();
    cam = new PerspectiveCamera(90,1f,ratio);
    cam.near = .01f;
    cam.position.set(0,.75f,0);
    cam.direction.set(0,0,1);
    sp = new ShaderProgram(Gdx.files.internal("vert.glsl"), Gdx.files.internal("frag.glsl"));
    Gdx.app.log("icmm", "shader log:"+sp.getLog());
    float wh=10;// Wall height
    tunnelWall = new Mesh(true, 4, 6,
      new VertexAttribute(VertexAttributes.Usage.Position, 3, "a_position"),
      new VertexAttribute(VertexAttributes.Usage.TextureCoordinates, 2, "a_uv"));
    tunnelWall.setVertices(new float[]{
      0-.5f,tgh,0-.5f,0,tgh,
      0-.5f,tgh,0+.5f,1,tgh,
      0-.5f,wh,0+.5f,1,wh-tgh,
      0-.5f,wh,0-.5f,0,wh-tgh
    });
    tunnelWall.setIndices(new short[]{
      1,2,3,
      3,1,0
    });
    wall = new Mesh(true, 4, 6,
      new VertexAttribute(VertexAttributes.Usage.Position, 3, "a_position"),
      new VertexAttribute(VertexAttributes.Usage.TextureCoordinates, 2, "a_uv"));
    wall.setVertices(new float[]{
      0-.5f,0,0-.5f,0,0,
      0-.5f,0,0+.5f,1,0,
      0-.5f,wh,0+.5f,1,wh,
      0-.5f,wh,0-.5f,0,wh
    });
    wall.setIndices(new short[]{
      1,2,3,
      3,1,0
    });
    floor = new Mesh(true, 4, 6,
      new VertexAttribute(VertexAttributes.Usage.Position, 3, "a_position"),
      new VertexAttribute(VertexAttributes.Usage.TextureCoordinates, 2, "a_uv"));
    floor.setVertices(new float[]{
      0-.5f,0,0-.5f,1,1,
      0-.5f,0,0+.5f,1,0,
      0+.5f,0,0+.5f,0,0,
      0+.5f,0,0-.5f,0,1
    });
    floor.setIndices(new short[]{
      1,2,3,
      3,1,0
    });
    uiitem = new Mesh(true, 4, 6,
      new VertexAttribute(VertexAttributes.Usage.Position, 3, "a_position"),
      new VertexAttribute(VertexAttributes.Usage.TextureCoordinates, 2, "a_uv"));
    uiitem.setVertices(new float[]{
      0,0,0,      0,0,
      0,-.5f,0,   0,1,
      .5f,-.5f,0, 1,1,
      .5f,0,0,    1,0
    });
    uiitem.setIndices(new short[]{
      1,2,3,
      3,1,0
    });
    inWorld = new Mesh(true, 4, 6,
      new VertexAttribute(VertexAttributes.Usage.Position, 3, "a_position"),
      new VertexAttribute(VertexAttributes.Usage.TextureCoordinates, 2, "a_uv"));
    inWorld.setVertices(new float[]{
      -.5f,0,0, 1,1,
      -.5f,1,0, 1,0,
      .5f,1,0,  0,0,
      .5f,0,0,  0,1
    });
    inWorld.setIndices(new short[]{
      1,2,3,
      3,1,0
    });
    inWorldTall = new Mesh(true, 4, 6,
      new VertexAttribute(VertexAttributes.Usage.Position, 3, "a_position"),
      new VertexAttribute(VertexAttributes.Usage.TextureCoordinates, 2, "a_uv"));
    inWorldTall.setVertices(new float[]{
      -.5f,0,0, 1,1,
      -.5f,tgh,0, 1,0,
      .5f,tgh,0,  0,0,
      .5f,0,0,  0,1
    });
    inWorldTall.setIndices(new short[]{
      1,2,3,
      3,1,0
    });
    held=new Hand();
    addObj(held);
    {
      Obj o=new Sign();
      o.setPos(3,3);
      addObj(o);
    }
    {
      Obj o=new GasBall();
      o.setPos(1,4);
      addObj(o);
    }
    {
      Obj o=new Door();
      o.setPos(5,1);
      addObj(o);
    }
    {
      Obj o=new Door();
      o.setPos(9,1);
      addObj(o);
    }
    for (int x=srtx;x<endx;++x)
      for (int y=srty;y<endy;++y)
        tiles[x][y] = new Tile().setPos(x,y);
    for (int x=0;x<5;++x)
      for (int y=0;y<5;++y)
        tileAt(x,y).setExists(true);
    for (int x = 5; x < 10; ++x)
      tileAt(x,1).setExists(true).type |= Tile.TUNNEL;
    for (int x=10;x<15;++x)
      for (int y=0;y<5;++y)
        tileAt(x,y).setExists(true);
    for (int y = 5; y < 7; ++y)
      tileAt(2,y).setExists(true).type |= Tile.TUNNEL;
    for (int x = 2; x < 6; ++x)
      tileAt(x,7).setExists(true).type |= Tile.TUNNEL;
    for (int x = 2; x < 4; ++x)
      tileAt(x,8).setExists(true).type |= Tile.TUNNEL;
    for (int x=6;x<10;++x)
      for (int y=6;y<11;++y)
        tileAt(x,y).setExists(true);
    {
      Obj o=new Door();
      o.setPos(2,5);
      o.angle=90;
      addObj(o);
    }
    {
      Obj o=new Door();
      o.setPos(5,7);
      addObj(o);
    }
    for (int y = 5; y < 9; ++y)
      tileAt(12,y).setExists(true).type |= Tile.TUNNEL;
    for (int x = 10; x < 12; ++x)
      tileAt(x,8).setExists(true).type |= Tile.TUNNEL;
    {
      Obj o=new Door();
      o.setPos(12,5);
      o.angle=90;
      addObj(o);
    }
    {
      Obj o=new Door();
      o.setPos(10,8);
      addObj(o);
    }
	}
  Obj anim = null;
  Obj held;
  Array<Obj> objs=new Array<Obj>();
  Tile[][] tiles=new Tile[endx][endy];
  void addObj(Obj o){
    o.setGame(this);
    o.init();
    objs.add(o);
  }
  Tile tileAt(int x, int y){
    return tiles[x][y];
  }
  boolean tileExists(int x, int y){
    if (x<srtx)return false;
    if (x>endx-1)return false;
    if (y<srty)return false;
    if (y>endy-1)return false;
    return tiles[x][y].exists;
  }
  Vector3 rectify(Vector3 pos){
    Vector2 rtn = new Vector2(pos.x, pos.z);
    int x = (int)Math.round(rtn.x);
    int y = (int)Math.round(rtn.y);
    if (tileExists(x,y)){
      float margin = .25f;
      //shove out of objs
      for (int i = 0; i < objs.size; ++i){
        Obj o = objs.get(i);
        if (o.solid){
          float shoveR=.45f;
          float dx = rtn.x-o.pos.x;
          float dy = rtn.y-o.pos.z;
          if (Math.abs(dx) < shoveR &&
            Math.abs(dy) < shoveR){// needs push
            if (Math.abs(dx)<Math.abs(dy)){
              if (dy<0)
                rtn.add(0,-dy-shoveR);
              else
                rtn.add(0,-dy+shoveR);
            }else{
              if (dx<0)
                rtn.add(-dx-shoveR,0);
              else
                rtn.add(-dx+shoveR,0);
            }
          }
        }
      }
      {
        float dx = rtn.x-x;
        float dy = rtn.y-y;
        if (!tileExists(x-1,y)&&dx<-margin)
          rtn.add(-dx-margin,0);
        if (!tileExists(x+1,y)&&dx>margin)
          rtn.add(-dx+margin,0);
        if (!tileExists(x,y-1)&&dy<-margin)
          rtn.add(0,-dy-margin);
        if (!tileExists(x,y+1)&&dy>margin)
          rtn.add(0,-dy+margin);
      }
      return new Vector3(rtn.x, pos.y, rtn.y);
    }
    return null;//default, this pos doesnt exist
  }

	@Override
	public void render () {
    // logic
    float dt = Gdx.graphics.getDeltaTime();
    // make sure it's loaded first
    if (ass.update())
    {
      { // controls
        held.pos.set(cam.position);
        float ls = 150f;//lookspeed
        float ms = 5f;//movespeed
        Vector3 newPos = cam.position.cpy();
        if (Gdx.input.isKeyPressed(Input.Keys.L)){
          cam.rotate(Vector3.Y, -dt*ls);
        }
        if (Gdx.input.isKeyPressed(Input.Keys.J)){
          cam.rotate(Vector3.Y, dt*ls);
        }
        if (Gdx.input.isKeyPressed(Input.Keys.I)){
          newPos.add(cam.direction.cpy().scl(dt*ms));
        }
        if (Gdx.input.isKeyPressed(Input.Keys.K)){
          newPos.add(cam.direction.cpy().scl(-dt*ms));
        }
        if (Gdx.input.isKeyPressed(Input.Keys.SPACE)&&anim==null){
          held.act();
        }
        newPos = rectify(newPos);
        if (newPos != null)
          cam.position.set(newPos);
        // do held obj anim (also logic)
        if (anim!=null)
          if (anim.actf(dt))
            anim=null;
      }
      {// objs
        for (int i = 0; i < objs.size; ++i){
          Obj o = objs.get(i);
          o.step(dt);
        }
      }
      // rendering
      Gdx.gl.glClearColor(0, 0, 0, 1);
      Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
      Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
      Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
      Gdx.gl.glEnable(GL20.GL_BLEND);
      sp.begin();
      cam.update();
      sp.setUniformMatrix("u_projectionViewMatrix", cam.combined);
      sp.setUniformf("u_light", cam.position.cpy().add(0,0,0));
      // draw box:
      {
        Texture stone  = ass.get("stone.png", Texture.class);
        stone.setWrap(Texture.TextureWrap.Repeat,Texture.TextureWrap.Repeat);
        stone.bind();
        for (int x = srtx; x < endx; ++x){
          for (int y = srty; y < endy; ++y){
            if (tileExists(x,y)){
              {
                Matrix4 mat = new Matrix4();
                mat.setTranslation(x,0,y);
                sp.setUniformMatrix("u_objectMatrix", mat);
                floor.render(sp, GL20.GL_TRIANGLES);
              }
              if ((tileAt(x,y).type&Tile.TUNNEL)>0)
              {
                Matrix4 mat = new Matrix4();
                mat.setTranslation(x,tgh,y);
                sp.setUniformMatrix("u_objectMatrix", mat);
                floor.render(sp, GL20.GL_TRIANGLES);
              }
              if (!tileExists(x-1,y)){
                Matrix4 mat = new Matrix4();
                mat.setTranslation(x,0,y);
                sp.setUniformMatrix("u_objectMatrix", mat);
                wall.render(sp, GL20.GL_TRIANGLES);
              }else{
                Tile t = tileAt(x-1,y);
                if ((t.type&Tile.TUNNEL)>0){
                  Matrix4 mat = new Matrix4();
                  mat.setTranslation(x,0,y);
                  sp.setUniformMatrix("u_objectMatrix", mat);
                  tunnelWall.render(sp, GL20.GL_TRIANGLES);
                }
              }
              if (!tileExists(x+1,y)){
                Matrix4 mat = new Matrix4();
                mat.translate(x,0,y);
                mat.rotate(Vector3.Y, 180);
                sp.setUniformMatrix("u_objectMatrix", mat);
                wall.render(sp, GL20.GL_TRIANGLES);
              }else{
                Tile t = tileAt(x+1,y);
                if ((t.type&Tile.TUNNEL)>0){
                  Matrix4 mat = new Matrix4();
                  mat.translate(x,0,y);
                  mat.rotate(Vector3.Y, 180);
                  sp.setUniformMatrix("u_objectMatrix", mat);
                  tunnelWall.render(sp, GL20.GL_TRIANGLES);
                }
              }
              if (!tileExists(x,y-1)){
                Matrix4 mat = new Matrix4();
                mat.translate(x,0,y);
                mat.rotate(Vector3.Y, -90);
                sp.setUniformMatrix("u_objectMatrix", mat);
                wall.render(sp, GL20.GL_TRIANGLES);
              }else{
                Tile t = tileAt(x,y-1);
                if ((t.type&Tile.TUNNEL)>0){
                  Matrix4 mat = new Matrix4();
                  mat.translate(x,0,y);
                  mat.rotate(Vector3.Y, -90);
                  sp.setUniformMatrix("u_objectMatrix", mat);
                  tunnelWall.render(sp, GL20.GL_TRIANGLES);
                }
              }
              if (!tileExists(x,y+1)){
                Matrix4 mat = new Matrix4();
                mat.translate(x,0,y);
                mat.rotate(Vector3.Y, 90);
                sp.setUniformMatrix("u_objectMatrix", mat);
                wall.render(sp, GL20.GL_TRIANGLES);
              }else{
                Tile t = tileAt(x,y+1);
                if ((t.type&Tile.TUNNEL)>0){
                  Matrix4 mat = new Matrix4();
                  mat.translate(x,0,y);
                  mat.rotate(Vector3.Y, 90);
                  sp.setUniformMatrix("u_objectMatrix", mat);
                  tunnelWall.render(sp, GL20.GL_TRIANGLES);
                }
              }
            }
          }
        }
      }
      // draw stuff
      {
        // order objs
        Array<Obj> orderedObjs = new Array<Obj>();
        Vector2 thisp = new Vector2(cam.position.x, cam.position.z);
        for (int i = 0; i < objs.size; ++i){
          Obj o = objs.get(i);
          o.renderDist = o.getPos().cpy().sub(thisp).len();
          if (orderedObjs.size==0)
            orderedObjs.add(o);
          else
            for (int c = 0; c < orderedObjs.size; ++c){
              if (o.renderDist > orderedObjs.get(c).renderDist){
                orderedObjs.insert(c,o);
                break;
              }else
              if (c==orderedObjs.size-1){
                orderedObjs.add(o);
                break;
              }
            }
        }
        for (int i = 0; i < orderedObjs.size; ++i){
          Obj o = orderedObjs.get(i);
          o.draw(sp);
        }
      }
      // draw ui
      {
        sp.setUniformMatrix("u_projectionViewMatrix", uicam.combined);
        Matrix4 mat = new Matrix4();
        sp.setUniformMatrix("u_objectMatrix", mat);
        sp.setUniformf("u_light", uicam.position);
        ass.get(held.sel, Texture.class).bind();
        uiitem.render(sp, GL20.GL_TRIANGLES);
      }
      sp.end();
    }else
    {
      Gdx.app.log("icmm", "load percent: "+ass.getProgress());
    }
  }
}
