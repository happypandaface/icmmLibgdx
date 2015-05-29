package com.zooth.icmm;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.utils.*;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.assets.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.glutils.*;
import com.badlogic.gdx.audio.*;

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
  float renderDist;// used for render ordering
  boolean remove;// if true, will be removed this step
  boolean flipX;// whether or not to flip the tex
  float pushFor = 0;// push the obj towards the camera
  long soundLoop=-1;// id of the looped sound
  String soundLoopName="";// name of the looped sound (for removal)
  Sound loop;
  Obj(){};
  void init(){};
  Icmm game;
  void setGame(Icmm g){this.game=g;}
  String tex;
  String sel;
  //String inv;
  boolean billboard;
  float scale=1;
  float angle;
  float offY;
  float shoveX=.45f;float shoveY=.45f;// these are for shoving out of solid objs
  boolean tall;
  Mesh customMesh(){return null;};
  boolean solid;
  boolean stationary;
  boolean canHit;
  boolean dead;// different than remove
  boolean inWorld=true;
  float radius=-1;// if bigger than 0 it means the obj is hittable (not solid)
  boolean canTog;
  Vector3 pos=new Vector3();void setPos(int x,int y){pos=new Vector3(x,0,y);}
  float ms=0;
  Vector2 getPos(){return new Vector2(pos.x,pos.z);}
  void draw(ShaderProgram sp){// called when invoked
    if (tex!=null&&inWorld){
      String t = getTex();// if we have multiple texs
      if (t==null)
        t=tex;
      Texture t2 = game.ass.get(t, Texture.class);
      t2.setWrap(Texture.TextureWrap.Repeat,Texture.TextureWrap.Repeat);
      t2.bind();
      Matrix4 mat = new Matrix4();
      Vector3 add = game.cam.position.cpy().sub(pos);
      add.y=0;
      add.nor().scl(pushFor);
      mat.translate(new Vector3(pos.x,(1-scale/2f)-.5f+offY,pos.z).add(add));
      float ang = angle;
      if (billboard){
        Vector2 viewp = new Vector2(game.cam.position.x, game.cam.position.z); 
        Vector2 thisp = new Vector2(pos.x, pos.z); 
        ang = thisp.cpy().sub(viewp).angle();
      }
      mat.rotate(Vector3.Y, -ang+90);
      mat.scale(scale,scale,scale);
      sp.setUniformMatrix("u_objectMatrix", mat);
      Mesh cm=customMesh();
      if (cm!=null){
        cm.render(sp, GL20.GL_TRIANGLES);
      }else{
        if (tall)
          game.inWorldTall.render(sp, GL20.GL_TRIANGLES);
        else
          if (flipX)
            game.inWorldR.render(sp, GL20.GL_TRIANGLES);
          else
            game.inWorld.render(sp, GL20.GL_TRIANGLES);
      }
    }
  }
  String getTex(){
    return null;
  }
  void step(float dt){// per fram
    if (ms>0){
      Vector2 dir = new Vector2(1,0).rotate(angle);
      dir.scl(dt*ms);
      Vector3 pos2=game.rectify(pos.add(dir.x, 0, dir.y),this);
      if (pos2!=null)
        pos.set(pos2);
    }
  }
  void act(){// called when invoked
  }
  boolean actf(float dt){//frames during action
    return true;// done with anim
  }
  void tog(Obj o){}
  void damaged(float f, Obj o){}
  void getHit(Obj o){};
  Array<Obj> inv = new Array<Obj>();
  void toInv(Obj o, Obj from){
    o.inWorld=false;
    inv.add(o);
  }
  void fromInv(Obj o, Obj taker){
    inv.removeValue(o, true);
    o.inWorld=true;
    o.pos=pos.cpy();
  }
  void dropInv(){
    for (Obj o : inv){
      o.inWorld=true;
      o.pos=pos.cpy().add((float)Math.random()*.01f,0,0);
    }
    inv.clear();
  }
}
class Guy extends Obj
{
  Guy(){
    radius=.3f;
  }
  void damaged(float f, Obj o){
    game.die();
  }
  void toInv(Obj o, Obj from){
    game.toInv(o);
  }
  void fromInv(Obj o, Obj taker){
    game.fromInv(o);
  }
}
class Sword extends Obj
{
  Sword(){
    super();
    tex="swordW.png";
    sel="sword1.png";
  }
  void act(){
    game.anim=this;
    game.playSound("swordW", getPos());
    actTime=0;
  }
  float actTime = 0;
  float actTimeMax = .5f;
  boolean actf(float dt){
    boolean setup=(actTime<actTimeMax*.5f);
    actTime+=dt;
    if(actTime<actTimeMax*.5f)
      sel="sword2.png";
    if(actTime>actTimeMax*.5f){
      sel="sword1.png";
      if (setup){
        Vector2 thisp = getPos();
        float tarAngle = new Vector2(game.cam.direction.x, game.cam.direction.z).angle();
        float bestDist = 1.2f;
        Obj bestObj = null;
        for (int i = 0; i < game.objs.size; ++i){
          Obj o = game.objs.get(i);
          if (o!=this&&o.canHit){
            Vector2 diff = o.getPos().cpy().sub(thisp);
            if (diff.len() < bestDist&&Icmm.compAngle(diff.angle(),tarAngle,80)){
              bestDist=diff.len();
              bestObj=o;
            }
          }
        }
        if (bestObj != null)
          bestObj.damaged(1f,this);
      }
    }
    if (actTime>actTimeMax)
      return true;
    return false;
  }
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
      game.playSound("click", getPos());
      Icmm.ObjTester iot = new Icmm.ObjTester(){
        boolean works(Obj o){
          return o.canTog;
        }
      };
      Obj bestObj = game.getFirst(this, 70, 1f, iot);
      if (bestObj != null)
        bestObj.tog(this);
      return true;
    }
    return false;
  }
  void toInv(Obj o, Obj from){
    game.toInv(o);
  }
  void fromInv(Obj o, Obj taker){
    game.fromInv(o);
  }
}
class Exp extends Obj
{
  Exp(){
    tex="exp1.png";
    billboard=true;
  }
  Obj dontKill=null;
  void init(){
    super.init();
    game.playSound("fireW", getPos());
    Array<Obj> hit = game.getRadius(.5f,getPos());
    for(int i = 0; i<hit.size;++i){
      Obj o = hit.get(i);
      if (o!=dontKill)
        o.damaged(1f, this);
    }
  }
  float expTimer=0;
  float maxExpTime=.25f;
  void step(float dt){
    expTimer+=dt;
    if (expTimer>maxExpTime*2f/3f)
      tex="exp3.png";
    else
    if (expTimer>maxExpTime/3f)
      tex="exp2.png";
    else
      tex="exp1.png";
    if (expTimer>maxExpTime){
      remove = true;
    }
  }
}
class FB extends Obj
{
  FB(){
    radius=.2f;
    tex="fb1.png";
    scale=.55f;
    billboard=true;
  }
  Obj dontKill=null;
  float spinTimer=0;
  float ms=4f;//move speed
  Vector2 dir=new Vector2(1,0);
  void step(float dt){
    game.loopSound("fire", getPos(), this);
    spinTimer+=dt;
    if ((int)(spinTimer*14)%3==0)
      tex="fb1.png";
    else
    if ((int)(spinTimer*14)%3==1)
      tex="fb2.png";
    else
      tex="fb3.png";
    pos.add(dir.x*dt*ms, 0, dir.y*dt*ms);
    Icmm.RectRTN rr = new Icmm.RectRTN();
    game.rectify(pos,this,rr);
  }
  void getHit(Obj o){
    if (o!=dontKill)
    {
      Exp exp = new Exp();
      exp.dontKill=dontKill;
      exp.pos=pos.cpy();
      game.addObj(exp);
      remove = true;
    }
  }
}
class WitchGoo extends Obj
{
  WitchGoo(){
    tex="witch.png";
    billboard=true;
  }
  float gooCount=0;
  float gooMax=1.75f;
  void init(){
    game.playSound("witchD", getPos());
  }
  void step(float dt){
    gooCount+=dt;
    if (gooCount>gooMax*.75f)
      tex="witchD4.png";
    else
    if (gooCount>gooMax*.5f)
      tex="witchD3.png";
    else
    if (gooCount>gooMax*.25f)
      tex="witchD2.png";
    else
      tex="witchD1.png";
  }
}
class Reliquary extends Obj
{
  Reliquary(){
    pushFor=.15f;
    tex="reliquary.png";
    billboard=true;
    scale=.4f;
    canTog=true;
    offY=.15f;
  }
  Array<CrystalDoor> cds = new Array<CrystalDoor>();
  void addDoor(CrystalDoor o){
    cds.add(o);
  }
  float sparkleTimer=0;
  void step(float dt){
    if (crystal!=null){
      sparkleTimer+=dt;
      if ((int)(sparkleTimer*4f)%2==0){
        tex="reliquaryDone.png";
      }else{
        tex="reliquaryDone2.png";
      }
    }else{
      tex="reliquary.png";
    }
  }
  Crystal crystal=null;
  void tog(Obj o){
    if (crystal!=null){
      for (CrystalDoor cd : cds)
        cd.tog(this);
      o.toInv(crystal, this);
      crystal=null;
    }
    //game.toInv(this);
  }
  void getCrystal(Crystal c){
    for (CrystalDoor cd : cds)
      cd.tog(this);
    crystal=c;
  }
}
class Crystal extends Obj
{
  Crystal(){
    tex="crystal.png";
    sel="crystalH.png";
    billboard=true;
    scale=.5f;
    canTog=true;
    offY=0;//.25f;
  }
  void tog(Obj o){
    game.toInv(this);
  }
  void act(){
    Icmm.ObjTester iot = new Icmm.ObjTester(){
      boolean works(Obj o){
        return (o instanceof Reliquary&&((Reliquary)o).crystal==null);
      }
    };
    Obj bestObj = game.getFirst(this, 70, 1f, iot);
    if (bestObj != null)
    {
      game.playSound("cry", getPos());
      game.fromInv(this);
      remove=true;
      ((Reliquary)bestObj).getCrystal(this);
    }
  }
}
class Grave extends Obj
{
  Grave(){
    tex="grave.png";
    billboard=true;
    scale=.5f;
    offY=-.25f;
  }
}
class Pedestal extends Obj
{
  Pedestal(){
    solid=true;
    tex="pedestal.png";
  }
  boolean drawingBase=false;
  void draw(ShaderProgram sp){
    tex="pedestal.png";
    drawingBase=true;
    super.draw(sp);
    tex="pedestalTop.png";
    drawingBase=false;
    super.draw(sp);
  }
  Mesh customMesh(){
    if (drawingBase)
      return game.pedestal;
    return game.pedestalTop;
  }
}
class Rat extends Obj
{
  Rat(){
    radius=.25f;
    tex="ratD1.png";
    billboard=true;
    canHit=true;
    ms=1f;
    angle=90;
  }
  float changeAngCount=0;
  float changeAngCountMax=2f;
  float changeAngCountMin=1f;
  void step(float dt){
    super.step(dt);
    if (!dead){
      changeAngCount-=dt;
      if (changeAngCount<=0){
        angle=(float)Math.random()*360f;
        changeAngCount=changeAngCountMin+(float)Math.random()*(changeAngCountMax-changeAngCountMin);
      }
    }
  }
  String getTex(){
    Vector2 camp = game.getCamPos().cpy();
    float camAng = camp.sub(getPos()).angle();
    //float camAng = new Vector2(game.cam.direction.x, game.cam.direction.z).angle();
    float diff = Icmm.normAngle(camAng-angle);
    if (diff < 45 && diff > -45){
      if ((changeAngCount*8)%2==0)
        flipX=true;
      else
        flipX=false;
      return "ratD1.png";
    }else
    if (diff < 135 && diff > 45){
      flipX=false;
      if ((int)(changeAngCount*8)%2==0)
        return "ratR1.png";
      else
        return "ratR2.png";
    }else
    if (diff > -135 && diff < -45){
      flipX=true;
      if ((int)(changeAngCount*8)%2==0)
        return "ratR1.png";
      else
        return "ratR2.png";
    }else
    if (diff > 135 || diff < -135){
      if ((changeAngCount*8)%2==0)
        flipX=true;
      else
        flipX=false;
      return "ratU1.png";
    }
    return null;
  }
  void damaged(float f, Obj by){
    if (!dead){
      remove=true;
      Grave o = new Grave();
      o.pos=pos;
      game.addObj(o);
      dead=true;
      ms=0;
      game.playSound("ratD", getPos());
    }
  }
}
class Witch extends Obj
{
  Witch(){
    radius=.40f;
    tex="witch.png";
    billboard=true;
    canHit=true;
  }
  float walkCount=0;
  float minWCount = 1;
  float maxWCount= 1;
  float castCount=0;
  float minCCount = 3;
  float maxCCount= 4;
  float castTimer = 0;
  Vector2 dir=new Vector2();
  void step(float dt){
    //Vector2 camp = new Vector2(game.cam.position.x, game.cam.position.z); 
    //Vector2 diff = camp.sub(getPos());
    //diff.nor().scl(dt);
    if (castTimer > 0)
    {
      castTimer -= dt;
      if (castTimer<=0){
        FB fb = new FB();
        fb.dontKill=this;
        Vector2 camp = new Vector2(game.cam.position.x, game.cam.position.z); 
        Vector2 diff = camp.sub(getPos());
        diff.nor();
        fb.dir=diff;
        fb.pos=pos.cpy();
        game.addObj(fb);
      }
      if ((int)(castTimer*5)%2==0)
        tex="witchCast.png";
      else
        tex="witch.png";
    }else
    {
      walkCount -= dt;
      if (walkCount<=0){
        walkCount=minWCount+(float)Math.random()*(maxWCount-minWCount);
        dir = new Vector2(1,0).rotate((float)Math.random()*360);
      }
      castCount -= dt;
      if (castCount<=0){
        castCount=minCCount+(float)Math.random()*(maxCCount-minCCount);
        castTimer = 1.5f;
      }
      if ((int)(walkCount*8)%2==0)
        tex="witchWalk1.png";
      else
        tex="witchWalk2.png";
      pos.add(dir.x*dt, 0, dir.y*dt);
      Vector3 pos2=game.rectify(pos,this);
      if (pos2!=null)
        pos.set(pos2);
    }
  }
  void damaged(float f, Obj hitter){
    dropInv();
    remove=true;
    game.playSound("manHit", getPos());
    WitchGoo o = new WitchGoo();
    o.pos=pos.cpy();
    game.addObj(o);
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
    Vector3 pos2=game.rectify(pos,this);
    if (pos2!=null)
      pos.set(pos2);
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
  void tog(Obj o){Gdx.app.log("icmm", msg);}
}
class CrystalDoor extends Obj
{
  CrystalDoor(){
    tall=true;
    solid=true;
    stationary=true;
    tex="crystalDoor.png";
  }
  void init(){
    // make sure we block if we're in a row
    if (angle!=0){
      shoveX=.75f;
    }else{
      shoveY=.75f;
    }
  }
  void tog(Obj o){
    game.playSound("door", getPos());
    solid=!solid;if(solid){offY=0;}else{offY=1.1f;}}
}
class Door extends Obj
{
  Door(){
    canTog=true;
    tall=true;
    solid=true;
    stationary=true;
    tex="doorC.png";
  }
  void tog(Obj o){
    game.playSound("door", getPos());
    solid=!solid;if(solid){tex="doorC.png";}else{tex="doorO.png";}}
}
public class Icmm extends ApplicationAdapter {
  static class ObjTester{
    boolean works(Obj o){return true;}
  }
  Obj getFirst(Obj obj, float rad, float dist, ObjTester ot){
    Vector2 thisp = obj.getPos();
    float tarAngle = new Vector2(cam.direction.x, cam.direction.z).angle();
    float bestDist = dist;
    Obj bestObj = null;
    for (int i = 0; i < objs.size; ++i){
      Obj o = objs.get(i);
      if (o!=obj&&ot.works(o)){
        Vector2 diff = o.getPos().cpy().sub(thisp);
        if (diff.len() < bestDist&&Icmm.compAngle(diff.angle(),tarAngle,(1-diff.len()/bestDist)*rad)){
          bestDist=diff.len();
          bestObj=o;
        }
      }
    }
    return bestObj;
  }
  Array<Obj> getRadius(float rad, Vector2 pos){
    Array<Obj> hit = new Array<Obj>();
    for (int i = 0; i < objs.size; ++i){
      Obj o = objs.get(i);
      if (o.radius>0)
      {
        if (pos.cpy().sub(o.getPos()).len()<o.radius+rad)
          hit.add(o);
      }
    }
    return hit;
  }
  void stopLoop(Obj o){
    if (o.loop!=null)
    {
      o.loop.stop(o.soundLoop);
    }
  }
  void loopSound(String s, Vector2 pos, Obj o){
    Vector2 diff = getGuyPos().cpy().sub(pos);
    float maxDist=10f;
    float pow=4f;
    float vol = (float)Math.pow((maxDist-diff.len())/maxDist,pow);
    if (vol > 0){
      float pan = normAngle(new Vector2(cam.direction.x,cam.direction.z).angle()-diff.angle())/180f;
      // pan wasn't even working, dunno if left and right are correct
      if (pan<0)
        pan+=1;
      else
      if (pan>0)
        pan=1-pan;
      if (vol>.95f)
        pan=0;
      if (o.loop!=null)
        o.loop.setPan(o.soundLoop, pan, vol);
      else 
      {
        o.loop = ass.get(s+".ogg", Sound.class);
        o.soundLoop = o.loop.loop(vol, 1f, pan);
        o.soundLoopName=s;
      }
    }
  }
  void playSound(String s, Vector2 pos){
    Vector2 diff = getGuyPos().cpy().sub(pos);
    float maxDist=10f;
    float pow=4f;
    float vol = (float)Math.pow((maxDist-diff.len())/maxDist,pow);
    if (vol > 0){
      float pan = normAngle(new Vector2(cam.direction.x,cam.direction.z).angle()-diff.angle())/180f;
      // pan wasn't even working, dunno if left and right are correct
      if (pan<0)
        pan+=1;
      else
      if (pan>0)
        pan=1-pan;
      ass.get(s+".ogg", Sound.class).play(vol, 1f, pan);
    }
  }
  Vector2 getCamPos(){
    return new Vector2(cam.position.x, cam.position.z);
  }
  Vector2 getGuyPos(){
    return new Vector2(cam.position.x, cam.position.z);
  }
  static float normAngle(float ang1){
    while (ang1>180){
      ang1-=360;
    }
    while (ang1<-180){
      ang1+=360;
    }
    return ang1;
  }
  static boolean compAngle(float ang1, float ang2, float degs){
    ang1=normAngle(ang1);
    ang2=normAngle(ang2);
    float compAng=normAngle(Math.abs(ang1-ang2));
    return (compAng<degs);
  }
  void die(){dying=true;}
  boolean dying=false;
  float deathTimer = 0;
  Camera cam;
  Camera uicam;
  ShaderProgram sp;
  static AssetManager ass = new AssetManager(); 
  Mesh floor;
  Mesh wall;
  Mesh tunnelWall;
  Mesh uiitem;
  Mesh inWorld;
  Mesh inWorldR;
  Mesh pedestal;
  Mesh pedestalTop;
  Mesh inWorldTall;
  int srtx = 0;
  int endx = 100;
  int srty = 0;
  int endy = 100;
	
  float tgh=1.5f;// tunnel gap height
	@Override
	public void create () {
    ass.load("ratD.ogg", Sound.class);
    ass.load("click.ogg", Sound.class);
    ass.load("cry.ogg", Sound.class);
    ass.load("door.ogg", Sound.class);
    ass.load("witchD.ogg", Sound.class);
    ass.load("manHit.ogg", Sound.class);
    ass.load("fire.ogg", Sound.class);
    ass.load("fireW.ogg", Sound.class);
    ass.load("swordW.ogg", Sound.class);
    ass.load("stone.png", Texture.class);
    ass.load("pedestal.png", Texture.class);
    ass.load("crystal.png", Texture.class);
    ass.load("crystalDoor.png", Texture.class);
    ass.load("reliquary.png", Texture.class);
    ass.load("reliquaryDone.png", Texture.class);
    ass.load("reliquaryDone2.png", Texture.class);
    ass.load("crystalH.png", Texture.class);
    ass.load("pedestalTop.png", Texture.class);
    ass.load("grave.png", Texture.class);
    ass.load("ratU1.png", Texture.class);
    ass.load("ratD1.png", Texture.class);
    ass.load("ratR1.png", Texture.class);
    ass.load("ratR2.png", Texture.class);
    ass.load("sword1.png", Texture.class);
    ass.load("sword2.png", Texture.class);
    ass.load("swordW.png", Texture.class);
    ass.load("sign.png", Texture.class);
    ass.load("gasBall.png", Texture.class);
    ass.load("witch.png", Texture.class);
    ass.load("witchCast.png", Texture.class);
    ass.load("witchWalk1.png", Texture.class);
    ass.load("witchWalk2.png", Texture.class);
    ass.load("witchD1.png", Texture.class);
    ass.load("witchD2.png", Texture.class);
    ass.load("witchD3.png", Texture.class);
    ass.load("witchD4.png", Texture.class);
    ass.load("fb1.png", Texture.class);
    ass.load("fb2.png", Texture.class);
    ass.load("fb3.png", Texture.class);
    ass.load("exp1.png", Texture.class);
    ass.load("exp2.png", Texture.class);
    ass.load("exp3.png", Texture.class);
    ass.load("hand.png", Texture.class);
    ass.load("hand2.png", Texture.class);
    ass.load("doorC.png", Texture.class);
    ass.load("doorO.png", Texture.class);
    reset();
  }
  void removeObjs(){
    for (int i = objs.size-1; i >= 0; --i){
      Obj o = objs.get(i);
      if (o.remove){
        stopLoop(o);
        objs.removeIndex(i);
      }
    }
  }
  void reset(){
    if (objs!=null){
      removeObjs();
    }
    Gdx.app.log("icmm", "reset");
    anim=null;
    dying=false;
    deathTimer=0;
    objs = new Array<Obj>();
    inv = new Array<Obj>();
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
    float f = 0;//-.2f;
    inWorld.setVertices(new float[]{
      -.5f,0,f, 1,1,
      -.5f,1,f, 1,0,
      .5f,1,f,  0,0,
      .5f,0,f,  0,1
    });
    inWorld.setIndices(new short[]{
      1,2,3,
      3,1,0
    });
    inWorldR = new Mesh(true, 4, 6,
      new VertexAttribute(VertexAttributes.Usage.Position, 3, "a_position"),
      new VertexAttribute(VertexAttributes.Usage.TextureCoordinates, 2, "a_uv"));
    inWorldR.setVertices(new float[]{
      -.5f,0,0, 0,1,
      -.5f,1,0, 0,0,
      .5f,1,0,  1,0,
      .5f,0,0,  1,1
    });
    inWorldR.setIndices(new short[]{
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
    {
      float s = .2f;
      float h = .45f;
      pedestal = new Mesh(true, 8, 24,
        new VertexAttribute(VertexAttributes.Usage.Position, 3, "a_position"),
        new VertexAttribute(VertexAttributes.Usage.TextureCoordinates, 2, "a_uv"));
      pedestal.setVertices(new float[]{
        -s,h,s,0,0,
        -s,0,s,1,1,
        s,0,s,0,1,
        s,h,s,1,0,
        -s,h,-s,1,0,
        -s,0,-s,0,1,
        s,0,-s,1,1,
        s,h,-s,0,0
      });
      pedestal.setIndices(new short[]{
        1,2,3,
        3,1,0,
        5,6,7,
        7,5,4,
        7,6,3,
        2,6,3,
        1,5,4,
        4,0,1
      });
      pedestalTop = new Mesh(true, 4, 6,
        new VertexAttribute(VertexAttributes.Usage.Position, 3, "a_position"),
        new VertexAttribute(VertexAttributes.Usage.TextureCoordinates, 2, "a_uv"));
      pedestalTop.setVertices(new float[]{
        0-s,h,0-s,1,1,
        0-s,h,0+s,1,0,
        0+s,h,0+s,0,0,
        0+s,h,0-s,0,1
      });
      pedestalTop.setIndices(new short[]{
        1,2,3,
        3,1,0
      });
    }
    held=new Hand();
    addObj(held);
    toInv(held);
    {
      Sword o = new Sword();
      addObj(o);
      toInv(o);
    }
    guy=new Guy();
    addObj(guy);
    {
      Obj o=new Pedestal();
      o.setPos(6,10);
      addObj(o);
    }
    {
      Obj o=new Sign();
      o.setPos(3,3);
      addObj(o);
    }
    {
      Obj o=new Witch();
      o.setPos(6,6);
      addObj(o);
    }
    {
      Obj w=new Witch();
      w.setPos(14,4);
      addObj(w);
      {
        Obj o=new Crystal();
        o.setPos(0,4);
        addObj(o);
        w.toInv(o,null);
      }
    }
    {
      Obj o=new Rat();
      o.setPos(1,3);
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
    {
      CrystalDoor c1=new CrystalDoor();
      c1.setPos(7,11);
      c1.angle=90;
      addObj(c1);
      CrystalDoor c2=new CrystalDoor();
      c2.setPos(8,11);
      c2.angle=90;
      addObj(c2);
      Reliquary o=new Reliquary();
      o.setPos(6,10);
      o.addDoor(c1);
      o.addDoor(c2);
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
    for (int x=6;x<10;++x)
      for (int y=14;y<18;++y)
        tileAt(x,y).setExists(true);
    for (int y = 11; y < 14; ++y){
      tileAt(7,y).setExists(true).type |= Tile.TUNNEL;
      tileAt(8,y).setExists(true).type |= Tile.TUNNEL;
    }
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
  Array<Obj> inv = new Array<Obj>();
  Array<Obj> objs=new Array<Obj>();
  Guy guy=new Guy();
  Tile[][] tiles=new Tile[endx][endy];
  void toInv(Obj o){
    o.inWorld=false;
    inv.add(o);
    switchTo(o);
  }
  void fromInv(Obj o){
    if (o==anim)
      anim=null;
    switchInv(-1);
    o.inWorld=true;
    inv.removeValue(o, true);
    held=inv.get(0);
  }
  void switchTo(Obj o){
    held=o;
  }
  void switchInv(int dir){
    for (int i = 0; i < inv.size; ++i){
      Obj o = inv.get(i);
      if (o==held){
        held=inv.get((inv.size+(i+dir))%inv.size);
        break;
      }
    }
  }
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
  static class RectRTN{
    boolean pushRad=false;
    boolean hit=false;
    Obj obj=null;
  }
  Vector3 rectify(Vector3 pos, Obj obj){return rectify(pos, obj, new RectRTN());}
  Vector3 solidRectify(Vector3 pos, Obj obj, RectRTN rr){
    Vector2 rtn = new Vector2(pos.x, pos.z);
    int x = (int)Math.round(rtn.x);
    int y = (int)Math.round(rtn.y);
    if (tileExists(x,y)){
      float margin = .25f;
      //shove out of objs
      for (int i = 0; i < objs.size; ++i){
        Obj o = objs.get(i);
        if (o!=obj&&o!=null)
        {
          if (o.solid){
            float shoveX=o.shoveX;
            float shoveY=o.shoveY;
            float dx = rtn.x-o.pos.x;
            float dy = rtn.y-o.pos.z;
            if (Math.abs(dx) < shoveX &&
              Math.abs(dy) < shoveY){// needs push
              rr.hit=true;
              obj.getHit(o);
              if (Math.abs(dx)<Math.abs(dy)){
                if (dy<0)
                  rtn.add(0,-dy-shoveY);
                else
                  rtn.add(0,-dy+shoveY);
              }else{
                if (dx<0)
                  rtn.add(-dx-shoveX,0);
                else
                  rtn.add(-dx+shoveX,0);
              }
            }
          }
        }
      }
      {
        float dx = rtn.x-x;
        float dy = rtn.y-y;
        if (!tileExists(x-1,y)&&dx<-margin){
          rr.hit=true;
          obj.getHit(null);
          rtn.add(-dx-margin,0);
        }
        if (!tileExists(x+1,y)&&dx>margin){
          rr.hit=true;
          obj.getHit(null);
          rtn.add(-dx+margin,0);
        }
        if (!tileExists(x,y-1)&&dy<-margin){
          rr.hit=true;
          obj.getHit(null);
          rtn.add(0,-dy-margin);
        }
        if (!tileExists(x,y+1)&&dy>margin){
          rr.hit=true;
          obj.getHit(null);
          rtn.add(0,-dy+margin);
        }
      }
      return new Vector3(rtn.x, pos.y, rtn.y);
    }
    rr.hit=true;
    obj.getHit(null);
    return null;//default, this pos doesnt exist
  }
  Vector3 rectify(Vector3 pos, Obj obj, RectRTN rr){
    Vector2 rtn = new Vector2(pos.x, pos.z);
    int x = (int)Math.round(rtn.x);
    int y = (int)Math.round(rtn.y);
    if (tileExists(x,y)){
      float margin = .25f;
      //shove out of objs
      for (int i = 0; i < objs.size; ++i){
        Obj o = objs.get(i);
        if (o!=obj&&o!=null)
        {
          if (o.radius>0)
          {
            Vector2 diff = rtn.cpy().sub(o.getPos());
            float dist = diff.len()-o.radius-obj.radius;
            if (dist<0)
            {
              rr.hit=true;
              rr.obj=o;
              o.getHit(obj);
              obj.getHit(o);
              rtn.add(diff.nor().scl(-dist/2f));
              if (rr.pushRad||true)
              {
                Vector2 newOPos = o.getPos().cpy().add(diff.nor().scl(dist/2f));
                Vector3 n3 = new Vector3(newOPos.x, o.pos.y, newOPos.y);
                n3 = solidRectify(n3, o, new RectRTN());
                if (n3!=null)
                  o.pos.set(n3);
              }
            }
          }
        }
      }
    }
    return solidRectify(pos,obj,rr);
  }

	@Override
	public void render () {
    // logic
    boolean needsReset=false;
    float dt = Gdx.graphics.getDeltaTime();
    // make sure it's loaded first
    if (ass.update())
    {
      { // controls
        guy.pos.set(cam.position);
        held.pos.set(cam.position);
        if (!dying){
          float ls = 170f;//lookspeed
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
          if (Gdx.input.isKeyJustPressed(Input.Keys.E)){
            switchInv(1);
          }
          if (Gdx.input.isKeyJustPressed(Input.Keys.R)){
            needsReset=true;
          }
          if (Gdx.input.isKeyJustPressed(Input.Keys.Q)){
            switchInv(-1);
          }
          if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)&&anim==null){
            held.act();
          }
          newPos = rectify(newPos,guy);
          if (newPos != null)
            cam.position.set(newPos);
          // do held obj anim (also logic)
          if (anim!=null)
            if (anim.actf(dt))
              anim=null;
        }
      }
      {// objs
        if (!dying){
          for (int i = 0; i < objs.size; ++i){
            Obj o = objs.get(i);
            o.step(dt);
          }
        }else{
          deathTimer+=dt*(float)Math.max(1f, 5f-deathTimer);
          cam.position.set(cam.position.x, cam.position.y-dt*.25f, cam.position.z);
          cam.direction.rotate(Vector3.Y, dt*180f);
          if (deathTimer > 5f)
            needsReset=true;
        }
        removeObjs();
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
      if (dying)
        sp.setUniformf("u_light", cam.position.cpy().add(0,-deathTimer*2f,0));
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
        if (!dying)
        {
          sp.setUniformMatrix("u_projectionViewMatrix", uicam.combined);
          Matrix4 mat = new Matrix4();
          sp.setUniformMatrix("u_objectMatrix", mat);
          sp.setUniformf("u_light", uicam.position);
          ass.get(held.sel, Texture.class).bind();
          uiitem.render(sp, GL20.GL_TRIANGLES);
        }
      }
      sp.end();
    }else
    {
      Gdx.app.log("icmm", "load percent: "+ass.getProgress());
    }
    if (needsReset)
      reset();
  }
}
