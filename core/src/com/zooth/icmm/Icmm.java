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
import java.util.Random;

class Tile
{
  int x;
  int y;
  float z;
  Tile setPos(int x,int y){this.x=x;this.y=y;return this;}
  Tile setExists(boolean e){exists=e;return this;}
  Vector2 getPos(){return new Vector2(x,y);}
  boolean exists;
  static int OPEN = 1;
  static int TUNNEL = (1<<2);
  static int PIT = (1<<3);
  static int DIRT = (1<<4);
  static int BLOCKED = (1<<5);
  int type = OPEN;
  boolean checkType(int t){
    return ((type&t)>0);
  }
  Tile addType(int t){
    type |= t;
    return this;
  }void removeType(int stt){type&=~stt;}
}
class Obj
{
  // statuses
  static long PSN=(1<<0);
  static long STUN=(1<<1);
  // types
  static long ITEM=(1<<0);
  static long NOCLIP=(1<<1);
  static long UNDER=(1<<2);// rats can sneak under
  static long INGOOP=(1<<3);// slowed by goop
  // item types
  static long HAND=(1<<0);
  static long WEAP=(1<<1);
  static long MAGIC=(1<<2);
  static long POTS=(1<<3);
  static long MISC=(1<<4);
  long status;boolean checkStatus(long stt){return (status&stt)>0;};void addStatus(long stt){status|=stt;}void removeStatus(long stt){status&=~stt;}
  long type;boolean checkType(long stt){return (type&stt)>0;};void addType(long stt){type|=stt;}void removeType(long stt){type&=~stt;}
  long itemType=0;boolean checkItemType(long stt){return (itemType&stt)>0;};void addItemType(long stt){itemType|=stt;}void removeItemType(long stt){itemType&=~stt;}
  float psnTime=0;float psnTimeC=0;
  float stunTime=0;float stunTimeC=0;
  float renderDist;// used for render ordering
  boolean remove;// if true, will be removed this step
  boolean flipX;// whether or not to flip the tex
  boolean flipSel2X;// whether to flip the other hand (very limited use)
  int viewPoints;// this # of angles this can billboard at (0 is inf)
  float pushFor = 0;// push the obj towards the camera
  long soundLoop=-1;// id of the looped sound
  String soundLoopName="";// name of the looped sound (for removal)
  Sound loop;
  Obj(){};
  void init(){hp=maxhp;};
  Icmm game;
  void setGame(Icmm g){this.game=g;if(ai!=null){ai.setGame(g);ai.init(this);}}
  String tex;
  String sel;
  //String inv;
  boolean billboard;
  float scale=1;
  float angle;
  float offY;
  float shoveX=.60f;float shoveY=.60f;// these are for shoving out of solid objs
  float weight=.1f;// how much the push when radius
  boolean tall;
  Mesh customMesh(){return null;};
  AI ai=null;
  void setAI(AI a){ai=a;ai.setGame(game);if(game!=null){ai.init(this);}}
  boolean solid;
  boolean stationary;
  boolean canHit;
  boolean dead;// different than remove
  float hp;float maxhp;float getHP(){return hp;}
  boolean inWorld=true;
  Array<Obj> dontHit=new Array<Obj>();
  void addDontHit(Obj o){dontHit.add(o);}
  float radius=-1;// if bigger than 0 it means the obj is hittable (not solid)
  boolean canTog;
  boolean canMove=true;
  Vector3 pos=new Vector3();void setPos(float x,float y){pos=new Vector3(x,0,y);}void setPos(Vector2 n){setPos(n.x,n.y);}void addPos(Vector2 p){pos.x+=p.x;pos.z+=p.y;}
  float ms=0;
  float vel=0;// velocity degrades unlike ms
  Vector2 getPos(){return new Vector2(pos.x,pos.z);}
  void setColor(ShaderProgram sp){sp.setUniformf("u_color", 1,1,1,1);};
  Vector2 texCoordsStart=new Vector2(0,0);
  Vector2 texCoordsScale=new Vector2(1,1);
  //Vector2 texCoordsScale=new Vector2(.5f,.5f);
  void setCoords(ShaderProgram sp){sp.setUniformf("u_texCoords", texCoordsStart.x, texCoordsStart.y, texCoordsScale.x, texCoordsScale.y);};
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
        Vector2 viewp = new Vector2(game.getCurrCamPos2().x, game.getCurrCamPos2().y); 
        Vector2 thisp = new Vector2(pos.x, pos.z); 
        ang = thisp.cpy().sub(viewp).angle();
        //if(this instanceof LionHead&&(int)ang%8==0)
        //  Gdx.app.log("",""+ang);
        if(viewPoints>0)
          ang=(float)Math.round(ang/180f*(float)viewPoints/2f)*180f/(float)viewPoints*2f;
      }
      mat.rotate(Vector3.Y, -ang+90);
      mat.scale(scale,scale,scale);
      sp.setUniformMatrix("u_objectMatrix", mat);
      setColor(sp);
      setCoords(sp);
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
  String getSel2(){
    return null;
  }
  String getSel(){
    return sel;
  }
  String getTex(){
    return null;
  }
  // default obj tester (for rectifying) there should be a second one for hitting
  Icmm.ObjTester objTester = new Icmm.ObjTester(this){
    boolean works(Obj o){
      return (!o.checkType(Obj.NOCLIP)&&!user.checkType(Obj.NOCLIP));
    }
  };boolean testRect(Obj o){return objTester.works(o);}
  Icmm.TileTester customTileTester=null;
  float psnThresh=0f;//min health psn will leave you at (0 is no min);
  void step(float dt){if(inWorld){if(ai!=null){ai.act(this,dt);}oldStep(dt);}
    if (checkStatus(STUN))
      stunTimeC+=dt;stunTime-=dt;if(stunTime<=0){removeStatus(STUN);}
    if (checkStatus(PSN)){
      psnTimeC+=dt;psnTime-=dt;if(psnTime<=0){removeStatus(PSN);}
      if(hp>psnThresh){
        hp-=dt*.05f;
        if(hp<psnThresh){hp=psnThresh;}
      }
      if(hp<=0){
        damaged(1f,null);//trigger death
      }
    }
  }
  boolean slowed=false;
  void oldStep(float dt){// per fram
    slowed=false;
    if(checkType(INGOOP))
      slowed=true;
    if(checkType(STUN)&&stunTime<.5f)
      slowed=true;
    removeType(INGOOP);// to check if we're in goop again
    float effms=ms;//effective movespeed
    if(vel>0){
      vel-=dt;
      if (vel<0)
        vel=0;
      effms=vel;
    }
    if (!canMove)
      effms=0;
    if (checkStatus(STUN)){
      if(psnTime<.5f)
        effms*=.2f;
      else
        effms=0;
    }
    if (effms>0){
      Vector2 dir = new Vector2(1,0).rotate(angle);
      dir.scl(dt*effms);
      Icmm.RectRTN rr = new Icmm.RectRTN();
      if (customTileTester!=null)
        rr.tt=customTileTester;
      rr.dontHit=dontHit;
      Vector3 pos2=game.rectify(pos.cpy().add(dir.x, 0, dir.y),this,rr);
      if (pos2!=null&&this!=game.possess){// make sure we're not the possessed target
        pos.set(pos2);
      }
    }
  }
  void act(){// called when invoked
  }
  void drop(){
    angle=holder.getAngle();
    setPos(holder.getPos().add(new Vector2(.25f,0).rotate(angle)));
    vel=1f;
    holder.fromInv(this,null);
  }
  boolean actf(float dt){//frames during action
    return true;// done with anim
  }
  void tog(Obj o){// only works if its an item
    if (inWorld)
      game.toInv(this);
  }
  boolean canDie;
  boolean playDieSounds=true;
  boolean leaveGrave=true;
  static int DMG_PHYS=1;
  static int DMG_STUN=2;
  static int DMG_FIRE=3;
  void damaged(float f, Obj hitter, long hitType){
    damaged(f,hitter);
  }
  void damaged(float f, Obj hitter){
    if(canDie){
      hp-=f;
      if (hp<=0){
        dropInv();
        remove=true;
        if(playDieSounds)
          game.playSound("manHit", getPos());
        if(leaveGrave){
          Grave o = new Grave();
          o.pos=pos.cpy();
          game.addObj(o);
        }
      }else{
        if(playDieSounds)
          game.playSound("armorHit", getPos());
      }
    }
  }
  void heal(float f, Obj o){hp+=f;if(hp>maxhp){hp=maxhp;}}
  void getHit(Obj o){};
  Array<Obj> inv = new Array<Obj>();
  Obj holder=null;
  void toInv(Obj o, Obj from){
    o.inWorld=false;
    o.holder=this;
    inv.add(o);
  }
  void fromInv(Obj o, Obj taker){
    inv.removeValue(o, true);
    o.inWorld=true;
    o.holder=null;
    o.pos=pos.cpy();
  }
  void dropSpecial(Obj o){
  }
  void dropInv(){
    for (Obj o : inv){
      o.inWorld=true;
      o.pos=pos.cpy().add((float)Math.random()*.01f,0,0);
      dropSpecial(o);
    }
    inv.clear();
  }
  Vector2 getDir(){
    return new Vector2(1,0).rotate(angle);
  }
  float getAngle(){
    return getDir().angle();
  }
  void fight(){
  }
}
class Guy extends Obj
{
  Guy(){
    hp=1f;
    maxhp=1f;
    weight=.1f;
    canHit=true;
    radius=.3f;
    billboard=true;
    tex="evilWiz.png";
  }
  void damaged(float f, Obj o, long type){
    if(type==Obj.DMG_STUN){
      stunTime=1.1f;
      if (!checkStatus(Obj.STUN))// for animation
        stunTimeC=0;
      addStatus(Obj.STUN);
    }
    damaged(f,o);
  }
  void damaged(float f, Obj o){
    if(o!=null)
      game.tweenAng(o.getPos().cpy().sub(getPos()).angle());
    game.playSound("wizD", getPos());
    if (!(Icmm.dev==1)){
      if(o instanceof Spider){
        if (!checkStatus(Obj.PSN))// for animation
          psnTimeC=0;
        psnTime=12f;
        addStatus(Obj.PSN);
      }else{
        hp -= f*.4f;
      }
    }
    if (hp<=0){
      hp=0;
      game.die();
    }
  }
  void step(float dt){
    ms=0f;// so step doesn't move us
    super.step(dt);
    if(slowed)
      ms=.2f;
    else
      if(checkStatus(Obj.STUN)){
        ms=0f;
      }else
        ms=1f;
  }
  void toInv(Obj o, Obj from){
    o.holder=this;
    game.toInv(o);
  }
  void fromInv(Obj o, Obj taker){
    o.holder=null;
    game.fromInv(o);
  }
  void setPos(float x, float y){
    game.cam.position.set(x,game.guyHeight,y);
  }
  Vector2 getDir(){return new Vector2(game.cam.direction.x, game.cam.direction.z);}
}
class Sword extends Obj
{
  Sword(){
    super();
    addItemType(Obj.WEAP);
    addType(Obj.ITEM);
    tex="swordW.png";
    sel="sword1.png";
    canTog=true;// for pickup
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
        Icmm.ObjTester iot = new Icmm.ObjTester(){
          boolean works(Obj o){
            return !(o instanceof Guy)&&o.canHit&&o.inWorld;
          }
        };
        Obj bestObj = game.getFirst(this, 70, game.guyReach, iot);
        if (bestObj != null){
          if(Icmm.dev==1){
          bestObj.damaged(10f,this);
          }else{
          bestObj.damaged(1f,this);
          }
        }
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
    addType(Obj.ITEM);
    addItemType(Obj.HAND);
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
          return o.canTog&&o.inWorld;
        }
      };
      Obj bestObj = game.getFirst(this, 90, game.guyReach, iot);
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
  }
  float expTimer=0;
  float maxExpTime=.25f;
  Exp delay(float t){expTimer=-t;return this;};
  void step(float dt){
    boolean ready=expTimer<=0;
    expTimer+=dt;
    tex=null;// so we dont draw before delay
    if (expTimer>0){
      if (ready){
        game.playSound("fireW", getPos());
        Array<Obj> hit = game.getRadius(.5f,getPos());
        for(int i = 0; i<hit.size;++i){
          Obj o = hit.get(i);
          if (o!=dontKill){
            o.damaged(1f, this);
          }
        }
      }
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
}
class Purp extends Obj
{
  Purp(){
    addType(Obj.NOCLIP);
    radius=.2f;
    tex="purp.png";
    scale=.55f;
    billboard=true;
    customTileTester=new Icmm.TileTester(){
      boolean works(Tile t){
        return (t!=null&&t.exists);
      }
    };
    ms=4f;
  }
  Obj dontKill=null;
  float spinTimer=0;
  Vector2 dir=new Vector2(1,0);
  void step(float dt){
    game.loopSound("fire", getPos(), this);
    spinTimer+=dt;
    if ((int)(spinTimer*14)%3==0)
      tex="purp.png";
    else
    if ((int)(spinTimer*14)%3==1)
      tex="purp.png";
    else
      tex="purp.png";
    angle=dir.angle();
    super.step(dt);
  }
  void getHit(Obj o){
    if (!remove&&o!=dontKill)
    {
      remove=true;
      if(o instanceof Rat){
        o.ai=null;
        game.possess=o;
      }
    }
  }
}
class FB extends Obj
{
  FB(){
    addType(Obj.NOCLIP);
    radius=.2f;
    tex="fb1.png";
    scale=.55f;
    billboard=true;
    customTileTester=new Icmm.TileTester(){
      boolean works(Tile t){
        return (t!=null&&t.exists);
      }
    };
    ms=4f;
  }
  Obj dontKill=null;
  float spinTimer=0;
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
    angle=dir.angle();
    super.step(dt);
  }
  void getHit(Obj o){
    if (!remove&&o!=dontKill)
    {
      Exp exp = new Exp();
      exp.dontKill=dontKill;
      exp.pos=pos.cpy();
      game.addObj(exp);
      remove = true;
    }
  }
}
class WizGoo extends Obj
{
  WizGoo(){
    tex="wiz.png";
    billboard=true;
  }
  float gooCount=0;
  float gooMax=1.75f;
  void init(){
    game.playSound("wizD", getPos());
  }
  void step(float dt){
    gooCount+=dt;
    if (gooCount>gooMax*.75f)
      if ((int)(gooCount*4)%2==0)
        tex="wizGoo1.png";
      else
        tex="wizGoo2.png";
    else
    if (gooCount>gooMax*.5f)
      tex="wizD3.png";
    else
    if (gooCount>gooMax*.25f)
      tex="wizD2.png";
    else
      tex="wizD1.png";
  }
}
class KnightGoo extends Obj
{
  KnightGoo(){
    tex="knightD6.png";
    billboard=true;
  }
  float gooCount=0;
  float gooMax=1.75f;
  void init(){
    game.playSound("wizD", getPos());
  }
  void step(float dt){
    gooCount+=2f*dt;
    if (gooCount>gooMax)
      tex="knightD6.png";
    else
    if (gooCount>gooMax*.80f)
      tex="knightD5.png";
    else
    if (gooCount>gooMax*.6f)
      tex="knightD4.png";
    else
    if (gooCount>gooMax*.4f)
      tex="knightD3.png";
    else
    if (gooCount>gooMax*.2f)
      tex="knightD2.png";
    else
      tex="knightD1.png";
  }
}
class Portal extends Obj
{
  Portal(){
    solid=true;
    canTog=true;
    tex="portal1.png";
    billboard=true;
  }
  float gooCount=0;
  float gooMax=1.75f;
  void init(){
  }
  void step(float dt){
    game.loopSound("portal", getPos(), this);
    gooCount+=dt*3f;
    if (gooCount>gooMax)
      gooCount-=gooMax;
    if (gooCount>gooMax*.666666f)
      tex="portal3.png";
    else
    if (gooCount>gooMax*.333333f)
      tex="portal2.png";
    else
      tex="portal1.png";
  }
  void tog(Obj o){
    if (o instanceof Hand){
      game.win();
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
class LionHead extends Obj
{
  LionHead(){
    viewPoints=4;
    pushFor=.15f;
    tex="lionF.png";
    billboard=true;
    scale=.4f;
    //canTog=true;
    offY=.15f;
  }
  float castTimer=0;
  float castCooldown=2.5f;
  void step(float dt){
    if (castTimer>=0){
      castTimer -= dt;
      if (castTimer<=0){
        fireCount=fireCastTime;
      }
    }else
    if (fireCastTime>=0){
      game.loopSound("fire", getPos(), this);
      fireCount-=dt;
      if (fireCount<=0){
        castTimer=castCooldown;
        game.stopLoop(this);
        castTimer=castCooldown;
        fireCount=0;
        FB fb = new FB();
        fb.dontKill=this;
        fb.dir=new Vector2(1,0).rotate(angle);
        fb.dir.scl(.7f);
        fb.pos=pos.cpy().add(new Vector3(fb.dir.x,0,fb.dir.y));
        fb.dir.nor();
        game.addObj(fb);
      }
    }
  }
  float fireCount=0;
  float fireCastTime=2.5f;
  String getTex(){
    Vector2 camp = game.getCurrCamPos2().cpy();
    float camAng = camp.sub(getPos()).angle();
    float diff = Icmm.normAngle(camAng-angle);
    if (diff < 45 && diff > -45){
      if ((int)(fireCount*8)%2==0)
        return "lionF.png";
      else
        return "lionFF.png";
    }else
    if (diff < 135 && diff > 45){
      flipX=true;
      if ((int)(fireCount*8)%2==0)
        return "lionS.png";
      else
        return "lionSF.png";
    }else
    if (diff > -135 && diff < -45){
      flipX=false;
      if ((int)(fireCount*8)%2==0)
        return "lionS.png";
      else
        return "lionSF.png";
    }else
    if (diff > 135 || diff < -135){
      return "lionB.png";
    }
    return null;
  }
}
class Torch extends Obj
{
  Torch(){
    tall=true;
    tex="torch.png";
    billboard=true;
    canHit=true;
    solid=true;
    radius=.25f;
    stationary=true;
  }
  Array<LockedDoor> lds = new Array<LockedDoor>();
  Array<Torch> torches = new Array<Torch>();
  void addDoor(LockedDoor o){
    lds.add(o);
  }
  void addTorch(Torch t){
    torches.add(t);
  }
  boolean onFire;
  float sparkleTimer=0;
  void step(float dt){
    if (onFire){
      sparkleTimer+=dt;
      if ((int)(sparkleTimer*4f)%2==0){
        tex="torchF1.png";
      }else{
        tex="torchF2.png";
      }
    }else{
      tex="torch.png";
    }
  }
  void damaged(float d, Obj o){
    if (!onFire){// only activate if we werent on fire
      if (o instanceof Exp){
        onFire=true;
        boolean allOn=true;
        for (Torch t : torches)
          if (!t.onFire)
            allOn=false;
        if (allOn)
          for (LockedDoor ld : lds)
            ld.tog(this);
      }
    }
  }
}
class Phono extends Obj
{
  Phono(){
    pushFor=.15f;
    tex="phono1.png";
    billboard=true;
    scale=.4f;
    canTog=true;
    offY=.05f;
  }
  float bounceTimer;
  void step(float dt){
    game.loopSound("short", getPos(), this, .05f);
    bounceTimer+=dt;
    if ((int)(bounceTimer*4f)%2==0){
      tex="phono1.png";
    }else{
      tex="phono2.png";
    }
  }
  void tog(Obj o){
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
class Chest extends Obj
{
  Chest(){
    tex="chest.png";
    billboard=true;
    canTog=true;
    solid=true;
    radius=.2f;
    stationary=true;
    // for image:
    scale=.8f;
    offY=-.1f;
  }
  boolean open=false;
  Obj lastAct=null;
  void tog(Obj o){
    lastAct=o;
    if(!open){
      dropInv();
      open=true;
      tex="chestO.png";
    }else
    if(open){
      open=false;
      tex="chest.png";
    }
  }
  void dropSpecial(Obj o){
    o.angle=lastAct.getPos().cpy().sub(o.getPos()).angle();
    o.vel=.5f;
    o.dontHit.add(this);
  }
}
class Crystal extends Obj
{
  Crystal(){
    addType(Obj.ITEM);
    addItemType(Obj.MISC);
    tex="crystal.png";
    sel="crystalH.png";
    billboard=true;
    scale=.5f;
    canTog=true;
    offY=0;//.25f;
  }
  void tog(Obj o){
    if (inWorld)
      game.toInv(this);
  }
  void act(){
    Icmm.ObjTester iot = new Icmm.ObjTester(){
      boolean works(Obj o){
        return (o instanceof Reliquary&&((Reliquary)o).crystal==null);
      }
    };
    Obj bestObj = game.getFirst(this, 70, game.guyReach, iot);
    if (bestObj != null)
    {
      game.playSound("cry", getPos());
      game.fromInv(this);
      remove=true;
      ((Reliquary)bestObj).getCrystal(this);
    }
  }
}
class ExpBarrel extends Obj
{
  ExpBarrel(){
    radius=.55f;
    stationary=true;
    canHit=true;
    tex="expBarrel.png";
    billboard=true;
    solid=true;
  }
  float expCount;
  void step(float dt){
    if (expCount>0){
      if ((int)(expCount*(expCount<1.5f?8:4))%2==0){
        tex="expBarrel2.png";
      }else{
        tex="expBarrel.png";
      }
      expCount-=dt;
      if (expCount<=0){
        remove=true;
        {
          Exp exp = new Exp();
          exp.setPos(getPos());
          game.addObj(exp);
        }
        for (float deg=0; deg<=360; deg+=(360f/8f)){
          Exp exp = new Exp();
          exp.delay(.1f);
          Vector2 expPos=getPos().add(new Vector2(1,0).rotate(deg));
          exp.pos=new Vector3(expPos.x, 0, expPos.y);
          game.addObj(exp);
        }
      }
    }
  }
  void damaged(float d, Obj o){
    if (expCount==0&&!remove&&o instanceof Exp){
      expCount=2;
    }
  }
}
class BombHolder extends Obj
{
  BombHolder(){
    radius=0;//.2f;
    //solid=true;
    tex="bomb1.png";
    billboard=true;
    scale=.5f;
    canTog=true;
    offY=0;//.25f;
  }
  void tog(Obj o){
    if (inWorld){
      Bomb b=new Bomb();
      game.addObj(b);
      b.tog(o);
      b.bh=this;
      hasBomb=false;
      tex="bombHolder.png";
    }
  }
  boolean hasBomb=true;
  void resetBomb(Bomb b){
    hasBomb=true;
    tex="bomb1.png";
  }
}
class Bomb extends Obj
{
  Bomb(){
    radius=.2f;
    canHit=true;
    tex="bomb1.png";
    sel="bombH1.png";
    billboard=true;
    scale=.5f;
    canTog=true;
    offY=0;//.25f;
  }
  BombHolder bh;
  float expCount;
  void step(float dt){
    super.step(dt);
    if (expCount>0){
      if ((int)(expCount*(expCount<1.5f?8:4))%2==0){
        tex="bomb2.png";
        sel="bombH2.png";
      }else{
        tex="bomb1.png";
        sel="bombH1.png";
      }
      expCount-=dt;
      if (expCount<=0){
        remove=true;
        if(bh!=null)
          bh.resetBomb(this);
        {
          Exp exp = new Exp();
          exp.setPos(getPos());
          game.addObj(exp);
        }
        for (float deg=0; deg<=360; deg+=(360f/8f)){
          Exp exp = new Exp();
          exp.delay(.1f);
          Vector2 expPos=getPos().add(new Vector2(1,0).rotate(deg));
          exp.pos=new Vector3(expPos.x, 0, expPos.y);
          game.addObj(exp);
        }
      }
    }
  }
  void tog(Obj o){
    if (inWorld)
      o.toInv(this,null);
  }
  void act(){
    angle=holder.getAngle();
    setPos(holder.getPos().add(new Vector2(.5f,0).rotate(angle)));
    vel=1f;
    holder.fromInv(this,null);
  }
  void damaged(float d, Obj o){
    if (expCount==0&&!remove&&o instanceof Exp){
      expCount=4;
    }
  }
}
class Book extends Obj
{
  static int EMPTY=0;
  static int FB=1;
  static int POSSESS=2;
  int bookType=EMPTY;void setBookType(int t){bookType=t;}
  Book(){
    addType(Obj.ITEM);
    addItemType(Obj.MAGIC);
    tex="flaskE.png";
    sel="flaskEH.png";
    billboard=true;
    scale=.5f;
    canTog=true;
    offY=0;//.25f;
  }
  String getTex(){
    if(bookType==FB){
      return "bookFB.png";
    }else
    if(bookType==POSSESS){
      return "bookPB.png";
    }else
    return "bookE.png";
  }
  String getSel(){
    if(bookType==FB){
      if(game.anim==this){
        if((int)(actTime*8)%2==0)
          return "bookFBH2.png";
      }
      return "bookFBH.png";
    }else
    if(bookType==POSSESS){
      if(game.anim==this){
        if((int)(actTime*8)%2==0)
          return "bookPH2.png";
      }
      return "bookPH.png";
    }
    return "bookEH.png";
  }
  String getSel2(){
    if(game.anim==this){
      flipSel2X=true;
      if((int)(actTime*8)%2==0)
        return "zap.png";
      else
        return "hand.png";
    }
    return null;
  }
  void step(float dt){
    super.step(dt);
  }
  void tog(Obj o){
    if (inWorld)
      game.toInv(this);
  }
  void act(){
    if(bookType!=EMPTY){
      game.anim=this;
      actTime=0;
    }
  }
  float actTime = 0;
  float actTimeMax = 1.5f;
  float actTimeRatio = .75f;
  boolean actf(float dt){
    boolean setup=(actTime<actTimeMax*actTimeRatio);
    game.loopSound("electric", getPos(), this, .25f);
    actTime+=dt;
    if(actTime>actTimeMax*actTimeRatio){
      if (setup){
        if(bookType==Book.FB){
          FB fb = new FB();
          fb.dontKill=this.holder;
          fb.dir=getDir().cpy();
          fb.pos=pos.cpy();
          fb.addPos(fb.dir.cpy().scl(.5f));
          game.addObj(fb);
        }else
        if(bookType==Book.POSSESS){
          Purp fb = new Purp();
          fb.dontKill=this.holder;
          fb.dir=getDir().cpy();
          fb.pos=pos.cpy();
          fb.addPos(fb.dir.cpy().scl(.5f));
          game.addObj(fb);
        }
      }
    }
    if (actTime>actTimeMax){
      game.stopLoop(this);
      return true;
    }
    return false;
  }
}
class Flask extends Obj
{
  static int EMPTY=0;
  static int HEALTH=1;
  static int ANTIDOTE=2;
  int flaskType=EMPTY;void setFlaskType(int t){flaskType=t;}
  Flask(){
    addType(Obj.ITEM);
    addItemType(Obj.POTS);
    tex="flaskE.png";
    sel="flaskEH.png";
    billboard=true;
    scale=.5f;
    canTog=true;
    offY=0;//.25f;
  }
  String getTex(){
    if(flaskType==HEALTH){
      if ((int)(bubbleTimer*4)%2==0)
        return "flaskR1.png";
      else
        return "flaskR2.png";
    }else
    if(flaskType==ANTIDOTE){
      if ((int)(bubbleTimer*4)%2==0)
        return "flaskG1.png";
      else
        return "flaskG2.png";
    }
    return "flaskE.png";
  }
  String getSel(){
    if(flaskType==HEALTH){
      if ((int)(bubbleTimer*4)%2==0)
        return "flaskR1H.png";
      else
        return "flaskR2H.png";
    }else
    if(flaskType==ANTIDOTE){
      if ((int)(bubbleTimer*4)%2==0)
        return "flaskG1H.png";
      else
        return "flaskG2H.png";
    }
    return "flaskEH.png";
  }
  float bubbleTimer=0;
  void step(float dt){
    super.step(dt);
    bubbleTimer+=dt;
  }
  void tog(Obj o){
    if (inWorld)
      game.toInv(this);
  }
  void act(){
    if(flaskType!=EMPTY){
      game.anim=this;
      game.playSound("drinkPot", getPos());
      actTime=0;
    }
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
        if(flaskType==HEALTH){
          game.guy.heal(1,this);
        }
        if(flaskType==ANTIDOTE){
          game.guy.removeStatus(Obj.PSN);
        }
        flaskType=EMPTY;
      }
    }
    if (actTime>actTimeMax)
      return true;
    return false;
  }
}
class StarBurst extends Obj
{
  StarBurst(){
    solid=false;
    tex="starBurst.png";
    billboard=true;
    scale=1f;
    //offY=-.25f;
  }
  float starCount=0;
  float totTime=.5f;
  String getTex(){
    if(starCount>totTime*2f/3f)
      return "starBurst1.png";
    else
    if(starCount>totTime/3f)
      return "starBurst2.png";
    return "starBurst3.png";
  }
  void step(float dt){
    super.step(dt);
    starCount+=dt;
    if(starCount>totTime)
    {
      remove = true;
    }
  }
}
class Grave extends Obj
{
  Grave(){
    solid=false;
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
class Dog extends Obj
{
  Dog(){
    radius=.25f;
    tex="dog.png";
    billboard=true;
    canHit=true;
    ms=1f;
    scale=.6f;
    offY=-.25f;
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
        Icmm.ObjTester iot = new Icmm.ObjTester(){
          boolean works(Obj o){
            return (o instanceof Guy);
          }
        };
        Array<Tile> path = game.getPath(getPos(), 17, this, iot);
        if (path!=null&&path.size>2)
          angle=path.get(path.size-3).getPos().cpy().sub(getPos()).angle();
        changeAngCount=changeAngCountMin+(float)Math.random()*(changeAngCountMax-changeAngCountMin);
      }
    }
  }
  String getTex(){
    if ((int)(changeAngCount*6)%2==0)
      flipX=true;
    else
      flipX=false;
    return "dog.png";
  }
  void damaged(float f, Obj by){
    if (!dead){
      remove=true;
      Grave o = new Grave();
      //o.msg="Here lies dog, son of dog";
      o.pos=pos;
      game.addObj(o);
      dead=true;
      ms=0;
      game.playSound("dogD", getPos());
    }
  }
}
class Spider extends Obj
{
  Spider(){
    radius=.25f;
    tex="spider.png";
    billboard=true;
    canHit=true;
    ms=1f;
    ai=new WanderFightAI();
    angle=90;
  }
  void init(){
    super.init();
    WanderFightAI wfai = (WanderFightAI)ai;
    wfai.fai.actTimeHalf=.75f;
    wfai.fai.actTimeMax=1f;
  }
  float changeAngCount=0;
  float changeAngCountMax=2f;
  float changeAngCountMin=1f;
  void fight(){
    game.playSound("swordW", getPos());
    angle=game.getGuyPos().cpy().sub(getPos()).angle();
    Icmm.ObjTester iot = new Icmm.ObjTester(){
      boolean works(Obj o){
        return o.inWorld&&o.canHit;
      }
    };
    Obj bestObj = game.getFirst(this, 90, game.diffHit, iot);
    if (bestObj != null){
      bestObj.damaged(1f,this);
    }
  }
  void step(float dt){
    if(inWorld)
      game.loopSound("spider", getPos(), this);
    super.step(dt);
  }
  String getTex(){
    if (ai instanceof WanderFightAI){
      WanderFightAI wfai = (WanderFightAI)ai;
      boolean attacking = wfai.currAct== WanderFightAI.ACT_KILL&&wfai.fai.attacking;
      if ((int)(wfai.actTime*8f)%2==0){
        flipX=true;
        if (attacking){return "spiderA1.png";}
      }else{
        flipX=false;
        if (attacking){return "spiderA2.png";}
      }
      return "spiderW1.png";
    }else
    if ((int)(changeAngCount*4)%2==0){
      return "spiderW1.png";
    }else{
      return "spiderW2.png";
    }
  }
  void damaged(float f, Obj by){
    if (!dead){
      remove=true;
      Grave o = new Grave();
      //o.msg="Here lies spider, son of spider";
      o.pos=pos;
      game.addObj(o);
      dead=true;
      ms=0;
      game.playSound("ratD", getPos());
    }
  }
}
class GoopPile extends Obj
{
  GoopPile(){
    billboard=true;
    radius=.5f;
    addType(Obj.NOCLIP);
    tex="goopPile.png";
  }
  void getHit(Obj obj){
    obj.addType(Obj.INGOOP);
  }
  float remC=4f;
  void step(float dt){
    remC-=dt;
    if(remC<=0)
      remove=true;
  }
}
class Goop extends Obj
{
  Goop(){
    radius=.1f;
    maxhp=1f;
    tex="goopF.png";
    billboard=true;
    canHit=true;
    canDie=true;
    //addType(Obj.NOCLIP);
    ai=new PatrolAI();
    ms=1f;
    playDieSounds=false;
    leaveGrave=false;
  }
  Icmm.ObjTester objTesterNotDoor=new Icmm.ObjTester(this){
    boolean works(Obj o){
      return (!o.checkType(Obj.UNDER)&&!user.checkType(Obj.UNDER));
    }
  };
  boolean testRect(Obj o){return objTester.works(o)&&objTesterNotDoor.works(o);}
  float changeAngCount=0;
  float changeAngCountMax=2f;
  float changeAngCountMin=1f;
  float leaveGoopC=0;
  void step(float dt){
    super.step(dt);
    if(inWorld){
      leaveGoopC+=dt;
      if(leaveGoopC>1f*scale){
        GoopPile gp = new GoopPile();
        gp.radius-=((float)iter)*gp.radius/6f;
        gp.scale=scale;
        gp.setPos(getPos());
        game.addObj(gp);
        leaveGoopC=0;
      }
    }
  }
  int iter=0;
  void damaged(float d, Obj h){
    super.damaged(d, h);
    if(h instanceof Exp||iter>2){
      game.playSound("splatD", getPos());
    }else{
      game.playSound("splat", getPos());
      for(int i = 0; i < 2;++i){// two new goops
        Goop g = new Goop();
        g.setPos(getPos());
        if(i==0)// one goes to opposite way
          if(ai instanceof PatrolAI){// make this one go a different way
            PatrolAI pai = (PatrolAI)ai;
            if(pai.path!=null)
              ((PatrolAI)g.ai).lastPos=pai.path.get(0).getPos().cpy().add((float)Math.random(), (float)Math.random());// set last pos to our dest
          }
        else
          if(ai instanceof PatrolAI){
            PatrolAI pai = (PatrolAI)ai;
            ((PatrolAI)g.ai).lastPos=pai.lastPos.cpy().add((float)Math.random(), (float)Math.random());// same last pos
          }
        g.scale=scale*.8f;
        g.iter=iter+1;
        game.addObj(g);
      }
    }
  }
}
class Rat extends Obj
{
  Rat(){
    radius=.25f;
    tex="ratD1.png";
    billboard=true;
    canHit=true;
    ai=new FleeAI();
    ms=1f;
    angle=90;
  }
  Icmm.ObjTester objTesterNotDoor=new Icmm.ObjTester(this){
    boolean works(Obj o){
      return (!o.checkType(Obj.UNDER)&&!user.checkType(Obj.UNDER));
    }
  };
  boolean testRect(Obj o){return objTester.works(o)&&objTesterNotDoor.works(o);}
  float changeAngCount=0;
  float changeAngCountMax=2f;
  float changeAngCountMin=1f;
  void step(float dt){
    super.step(dt);
    if(ai==null){
    /*
      if (!dead){
        changeAngCount-=dt;
        if (changeAngCount<=0){
          angle=(float)Math.random()*360f;
          changeAngCount=changeAngCountMin+(float)Math.random()*(changeAngCountMax-changeAngCountMin);
        }
      }*/
    }
  }
  String getTex(){
    Vector2 camp = new Vector2(game.getCurrCamPos().x, game.getCurrCamPos().z);
    float camAng = camp.sub(getPos()).angle();
    //float camAng = new Vector2(game.cam.direction.x, game.cam.direction.z).angle();
    float diff = Icmm.normAngle(camAng-angle);
    if(ai instanceof FleeAI){
      if (diff < 45 && diff > -45){
        if ((int)(((FleeAI)ai).checkSightCount*8)%2==0)
          flipX=true;
        else
          flipX=false;
        return "ratD1.png";
      }else
      if (diff < 135 && diff > 45){
        flipX=false;
        if ((int)(((FleeAI)ai).checkSightCount*8)%2==0)
          return "ratR1.png";
        else
          return "ratR2.png";
      }else
      if (diff > -135 && diff < -45){
        flipX=true;
        if ((int)(((FleeAI)ai).checkSightCount*8)%2==0)
          return "ratR1.png";
        else
          return "ratR2.png";
      }else
      if (diff > 135 || diff < -135){
        if ((int)(((FleeAI)ai).checkSightCount*8)%2==0)
          flipX=true;
        else
          flipX=false;
        return "ratU1.png";
      }
    }
    return null;
  }
  void damaged(float f, Obj by){
    if (!dead){
      remove=true;
      Grave o = new Grave();
      //o.msg="Here lies rat, son of rat";
      o.pos=pos;
      game.addObj(o);
      dead=true;
      ms=0;
      game.playSound("ratD", getPos());
    }
  }
}
class Wiz extends Obj
{
  Wiz(){
    radius=.40f;
    tex="wiz.png";
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
  float teleCoolDown=0;
  void step(float dt){
    //Vector2 camp = new Vector2(game.cam.position.x, game.cam.position.z); 
    //Vector2 diff = camp.sub(getPos());
    //diff.nor().scl(dt);
    teleCoolDown-=dt;
    if(teleCoolDown<=0){
      Vector2 diff=game.guy.getPos().cpy().sub(getPos());
      if(diff.len()<3){
        final Obj away = game.guy;
        Icmm.TileTester tt = new Icmm.TileTester(){
          boolean works(Tile t){return (t!=null&&t.exists&&(t.type&~Tile.PIT)>0)&&away.getPos().cpy().sub(t.getPos()).len()>6;}
        };
        Array<Tile> tiles = game.getPath(getPos(), 10, this, tt);
        teleCoolDown=.5f;// dont wanna tile check all the time
        if(tiles!=null)
        {
          teleCoolDown=1f;
          castTimer=0;
          {
            game.playSound("tele", getPos(), .5f);
            StarBurst o = new StarBurst();
            o.pos=pos.cpy();
            game.addObj(o);
          }
          setPos(tiles.get(0).getPos());
        }
      }
    }
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
        tex="wizC.png";
      else
        tex="wiz.png";
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
        tex="wizW1.png";
      else
        tex="wizW2.png";
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
    {
      WizGoo o = new WizGoo();
      o.pos=pos.cpy();
      game.addObj(o);
    }
  }
}
class Worm extends Obj
{
  Worm(){
    canHit=true;
    radius=.4f;
    weight=2f;
    tex="wormUp.png";
    billboard=true;
    maxhp=2f;
    ai=new WormAI();
    ms=.3f;
    customTileTester= new Icmm.TileTester(){
      boolean works(Tile t){return (t!=null&&t.checkType(Tile.DIRT)&&t.exists&&(t.type&~Tile.PIT)>0);}
    };
    connectedWorms.add(this);
  }
  Array<Worm> connectedWorms=new Array<Worm>();
  void addWorm(Worm w){
    for(int i = connectedWorms.size-1;i>=0;--i)
      w.addWorm_r(connectedWorms.get(i));
    for(int i = connectedWorms.size-1;i>=0;--i)
      connectedWorms.get(i).addWorm_r(w);
  }
  void addWorm_r(Worm w){
    connectedWorms.add(w);
  }
  void remWorm(Worm w){
    for(int i = connectedWorms.size-1;i>=0;--i)
      if(connectedWorms.get(i)==w)
        connectedWorms.removeIndex(i);
  }
  void step(float dt){
    super.step(dt);
    if (ai instanceof WanderFightAI){
      WanderFightAI wfai = (WanderFightAI)ai;
      if ((int)(wfai.actTime*8f)%2==0)
        flipX=true;
      else
        flipX=false;
      if (wfai.currAct==WanderFightAI.ACT_KILL){
        tex="wormUp.png";
      }else
      if (wfai.currAct==WanderFightAI.ACT_WALK){
        tex="wormDig.png";
      }
    }else
    if (ai instanceof WormAI){
      WormAI wai = (WormAI)ai;
      if(wai.state==WormAI.ROAM)
        game.loopSound("dig", getPos(), this,.5f);
      if(wai.state==WormAI.BURROW||wai.state==WormAI.DELAY)
        game.loopSound("dig", getPos(), this);
      else
        game.stopLoop(this);
      if(wai.state==WormAI.BURST)
        tex="wormOut.png";
      else
        tex="wormDig.png";
      if ((int)(wai.actTime*8f)%2==0)
        flipX=true;
      else
        flipX=false;
    }
  }
  void damaged(float f, Obj o){
    if (ai instanceof WormAI){
      WormAI wai = (WormAI)ai;
      if(wai.state==WormAI.BURST||Icmm.dev==1){
        hp-=f;
        if(hp<=0){
          remove=true;
          game.playSound("deepDead",getPos());
          Grave g = new Grave();
          g.setPos(getPos());
          g.tex="wormDig.png";
          game.addObj(g);
          for(int i = connectedWorms.size-1;i>=0;--i)
            if(connectedWorms.size>i)
              connectedWorms.get(i).remWorm(this);
          if(connectedWorms.size==0)
            dropInv();
        }else
          game.playSound("deepHurt",getPos());
      }
    }
  }
  void fight(){
    game.playSound("swordW", getPos());
    angle=game.getGuyPos().cpy().sub(getPos()).angle();
    Icmm.ObjTester iot = new Icmm.ObjTester(){
      boolean works(Obj o){
        return o.inWorld&&o.canHit;
      }
    };
    Obj bestObj = game.getFirst(this, 90, game.diffHit, iot);
    if (bestObj != null){
      bestObj.damaged(1f,this);
    }
  }
}
class Skeli extends Obj
{
  Skeli(){
    radius=.40f;
    weight=1f;
    ms=.9f;
    tex="skeli.png";
    billboard=true;
    canHit=true;
    ai=new FightAI();
  }
  void fight(){
    game.playSound("swordW", getPos());
    angle=game.getGuyPos().cpy().sub(getPos()).angle();
    Icmm.ObjTester iot = new Icmm.ObjTester(){
      boolean works(Obj o){
        return o.inWorld&&o.canHit&&!(o instanceof Necro);
      }
    };
    Obj bestObj = game.getFirst(this, 90, game.diffHit, iot);
    if (bestObj != null){
      bestObj.damaged(1f,this);
    }
  }
  float walkCount=0;
  float minWCount = 1;
  float maxWCount= 1;
  float castCount=0;
  float minCCount = 3;
  float maxCCount= 4;
  float castTimer = 0;
  Vector2 dir=new Vector2();
  float changeAngCount=0;
  float changeAngCountMax=.5f;
  float changeAngCountMin=.3f;
  float actTime=0;
  float actTimeMax=2f;
  String getTex(){
    FightAI fai=null;
    if(ai instanceof FightAI){
      fai = (FightAI)ai;
    }
    if(ai instanceof WanderFightAI){
      fai = ((WanderFightAI)ai).fai;
    }
    if (fai!=null){
      if(fai.actTime<fai.actTimeMax){
        if(fai.actTime<fai.actTimeMax*.5f)
          return "skeliA1.png";
        if(fai.actTime>fai.actTimeMax*.5f)
          return "skeliA2.png";
      }else
      {
        if ((int)(fai.walkCount*8)%2==0)
          tex="skeliW1.png";
        else
          tex="skeliW2.png";
      }
    }
    return null;
  }
  void step(float dt){
    super.step(dt);
  }
  float health=2f;
  void damaged(float f, Obj hitter){
    health-=f;
    if (health<=0){
      dropInv();
      remove=true;
      game.playSound("manHit", getPos());
      Grave o = new Grave();
      o.pos=pos.cpy();
      game.addObj(o);
    }else{
      game.playSound("armorHit", getPos());
    }
  }
}
class GoldKnight extends SilverKnight{
  GoldKnight(){
    super();
    if(ai instanceof NormAI){
      NormAI nai = (NormAI)ai;
      nai.hitTime=.25f;
      nai.recoilTime=.4f;
      nai.seekSpeed=1.6f;
      nai.plusDist=.1f;
      nai.mustSeekTime=.1f;
      nai.roamDist=4;
      nai.seekDist=6;
    }
  }
  void setColor(ShaderProgram sp){
    sp.setUniformf("u_color", 2f, 2f, 1f, 1f);
  }
}
class Goblin extends Obj{
  Goblin(){
    super();
    canDie=true;
    NormAI nai=new NormAI();
    nai.hitTime=.1f;
    nai.recoilTime=.1f;
    nai.mustSeekTime=5f;
    nai.roamDist=4;
    nai.seekDist=10;
    ai=nai;
    radius=.40f;
    weight=.1f;
    ms=.9f;
    tex="knight.png";
    billboard=true;
    canHit=true;
    //ms=1.4f;
    //dmg=1.25f;
  }
  void setColor(ShaderProgram sp){
    sp.setUniformf("u_color", 1f, 1f, 1f, 1f);
  }
  void step(float dt){
    super.step(dt);
    if(lit){
      litTime-=dt;
      if(litTime<=0){
        remove=true;
        {
          Exp exp = new Exp();
          exp.setPos(getPos());
          game.addObj(exp);
        }
        for (float deg=0; deg<=360; deg+=(360f/8f)){
          Exp exp = new Exp();
          exp.delay(.1f);
          Vector2 expPos=getPos().add(new Vector2(1,0).rotate(deg));
          exp.pos=new Vector3(expPos.x, 0, expPos.y);
          game.addObj(exp);
        }
      }
    }
  }
  boolean lit=false;
  float litTime=0f;
  void fight(){
    game.playSound("match", getPos());
    litTime=2f;
    lit=true;
  }
  String getTex(){
    if(ai instanceof NormAI){
      NormAI nai = (NormAI)ai;
      if (nai.state==NormAI.ROAM||nai.state==NormAI.SEEK){
        if((int)(nai.actTime*8)%2==0)
          flipX=true;
        else
          flipX=false;
        if(lit&&(int)(nai.actTime*14)%2==0)
          return "goblinF.png";
      }
    }
    return "goblin.png";
  }
}
class SilverKnight extends Knight{
  SilverKnight(){
    super();
    NormAI nai=new NormAI();
    ai=nai;
    //ms=1.4f;
    //dmg=1.25f;
  }
  void setColor(ShaderProgram sp){
    sp.setUniformf("u_color", 2f, 2f, 2f, 1f);
  }
  String getTex(){
    if(ai instanceof NormAI){
      NormAI nai = (NormAI)ai;
      if (nai.state==NormAI.ROAM||nai.state==NormAI.SEEK){
        if((int)(nai.actTime*8)%2==0)
          return "knightW1.png";
        else
          return "knightW2.png";
      }else
      if(nai.state==NormAI.FIGHT){
        if(nai.actTime<nai.hitTime)
          return "knightA1.png";
        else
          return "knightA2.png";
      }
    }
    return "knight.png";
  }
}
class Dwarf extends Obj
{
  float dmg=1.5f;
  Dwarf(){
    maxhp=3f;
    billboard=true;
    tex="dwarfSheet.png";
    texCoordsStart=new Vector2(0,0);
    texCoordsScale=new Vector2(1f/3f,.5f);
    ai=new NormAI();
    radius=.35f;
    weight=.5f;
    canHit=true;
    canDie=true;
    ms=1.1f;
  }
  String getTex(){
    if(ai.getAnimState()==AI.WALKING){
      if((int)(ai.getAnimTime()*4)%2==0){
        texCoordsStart=new Vector2(1f/3f,.5f);
      }else{
        texCoordsStart=new Vector2(0,.5f);
      }
    }
    if(ai.getAnimState()==AI.CHARGING){
      texCoordsStart=new Vector2(1f/3f,0f);
    }
    if(ai.getAnimState()==AI.ATTACKING){
      texCoordsStart=new Vector2(2f/3f,0f);
    }
    return "dwarfSheet.png";
  }
  void fight(){
    Array<Obj> hit = game.getRadiusAll(3.5f,getPos());
    for(int i = 0; i<hit.size;++i){
      Obj o = hit.get(i);
      if(!(o instanceof Dwarf))
        o.damaged(.1f,this,Obj.STUN);
    }
    game.playSound("swordW", getPos());
    angle=game.getGuyPos().cpy().sub(getPos()).angle();
    Icmm.ObjTester iot = new Icmm.ObjTester(){
      boolean works(Obj o){
        return !(o instanceof Dwarf)&&o.inWorld&&o.canHit;
      }
    };
    Obj bestObj = game.getFirst(this, 90, game.diffHit, iot);
    if (bestObj != null){
      bestObj.damaged(dmg,this);
    }
  }
}
class Knight extends Obj
{
  Knight(){
    radius=.40f;
    weight=1f;
    ms=.9f;
    tex="knight.png";
    billboard=true;
    canHit=true;
    ai=new FightAI();
  }
  float dmg=1f;
  void setColor(ShaderProgram sp){
    super.setColor(sp);
    //sp.setUniformf("u_color", 1.5f,1.3f,.2f,1);
  }
  void fight(){
    game.playSound("swordW", getPos());
    angle=game.getGuyPos().cpy().sub(getPos()).angle();
    Icmm.ObjTester iot = new Icmm.ObjTester(){
      boolean works(Obj o){
        return o.inWorld&&o.canHit;
      }
    };
    Obj bestObj = game.getFirst(this, 90, game.diffHit, iot);
    if (bestObj != null){
      bestObj.damaged(dmg,this);
    }
  }
  float walkCount=0;
  float minWCount = 1;
  float maxWCount= 1;
  float castCount=0;
  float minCCount = 3;
  float maxCCount= 4;
  float castTimer = 0;
  Vector2 dir=new Vector2();
  float changeAngCount=0;
  float changeAngCountMax=.5f;
  float changeAngCountMin=.3f;
  float actTime=0;
  float actTimeMax=2f;
  String getTex(){
    FightAI fai=null;
    if(ai instanceof FightAI){
      fai = (FightAI)ai;
    }
    if(ai instanceof WanderFightAI){
      fai = ((WanderFightAI)ai).fai;
    }
    if (fai!=null){
      if(fai.actTime<fai.actTimeMax){
        if(fai.actTime<fai.actTimeMax*.5f)
          return "knightA1.png";
        if(fai.actTime>fai.actTimeMax*.5f)
          return "knightA2.png";
      }else
      {
        if ((int)(fai.walkCount*8)%2==0)
          tex="knightW1.png";
        else
          tex="knightW2.png";
      }
    }
    return null;
  }
  void step(float dt){
    //Vector2 sight = game.sight(this, getPos(), game.guy, .5f, 6f);
    if (ai!=null)
      ai.act(this,dt);
    else{
      if (actTime < actTimeMax)
      {
        boolean setup=(actTime<actTimeMax*.5f);
        actTime+=dt;
        if(actTime<actTimeMax*.5f)
          tex="knightA1.png";
        if(actTime>actTimeMax*.5f){
          tex="knightA2.png";
          if (setup){
            game.playSound("swordW", getPos());
            angle=game.getGuyPos().cpy().sub(getPos()).angle();
            Icmm.ObjTester iot = new Icmm.ObjTester(){
              boolean works(Obj o){
                return o.inWorld&&o.canHit;
              }
            };
            Obj bestObj = game.getFirst(this, 90, game.diffHit, iot);
            if (bestObj != null){
              bestObj.damaged(1f,this);
            }
          }
        }
      }else
      {
        walkCount += dt;
        if ((int)(walkCount*8)%2==0)
          tex="knightW1.png";
        else
          tex="knightW2.png";
        changeAngCount-=dt;
        if (changeAngCount<=0){
          Icmm.ObjTester iot = new Icmm.ObjTester(){
            boolean works(Obj o){
              return (o instanceof Guy);
            }
          };
          Array<Tile> path = game.getPath(getPos(), 17, this, iot);
          if (path!=null&&path.size>2){
            // open any doors in our way
            Tile t = path.get(path.size-3);
            for (Obj o : game.objs){
              if (game.tileAt(o.getPos())==t&&
                o.solid&&o.canTog){
                o.tog(this);
              }
            }
            angle=t.getPos().cpy().sub(getPos()).angle();
          }
          if (path!=null&&path.size < 4){
            // we should attack, the target is close
            actTime=0f;
          }
          changeAngCount=changeAngCountMin+(float)Math.random()*(changeAngCountMax-changeAngCountMin);
        }
        super.oldStep(dt);
        /*
        castCount -= dt;
        if (castCount<=0){
          castCount=minCCount+(float)Math.random()*(maxCCount-minCCount);
          actTime = 0f;
        }*/
      }
    }
  }
  float health=5f;
  void damaged(float f, Obj hitter){
    health-=f;
    if (health<=0){
      dropInv();
      remove=true;
      game.playSound("manHit", getPos());
      KnightGoo o = new KnightGoo();
      o.pos=pos.cpy();
      game.addObj(o);
    }else{
      game.playSound("armorHit", getPos());
    }
  }
}
class Necro extends Obj
{
  Necro(){
    radius=.40f;
    ms=2f;
    tex="necro1.png";
    billboard=true;
    canHit=true;
    maxhp=2f;
    //FindAI fai = new FindAI();
    FleeFindAI ffai = new FleeFindAI();
    ffai.fiai.actTimeMax=.5f;
    ffai.fiai.iot=new Icmm.ObjTester(){
      boolean works(Obj o){
        return (o instanceof Grave);
      }
    };
    ai=ffai;
  }
  float teleCoolDown=0;
  void step(float dt){
    super.step(dt);
    teleCoolDown-=dt;
    if(teleCoolDown<=0){
      Vector2 diff=game.guy.getPos().cpy().sub(getPos());
      if(diff.len()<3){
        final Obj away = game.guy;
        Icmm.TileTester tt = new Icmm.TileTester(){
          boolean works(Tile t){
            Obj sol=null;
            for (int i=0;i<game.objs.size;++i){
              Obj o=game.objs.get(i);
              Tile curT=game.tileAt(o.getPos());
              if (curT==t&&o.solid){
                sol=o;
                break;
              }
            }
            return (sol==null&&t!=null&&t.exists&&(t.type&~Tile.PIT)>0)&&away.getPos().cpy().sub(t.getPos()).len()>6;}
        };
        Icmm.TileTester tt2 = new Icmm.TileTester(){
          boolean works(Tile t){
            Obj sol=null;
            for (int i=0;i<game.objs.size;++i){
              Obj o=game.objs.get(i);
              Tile curT=game.tileAt(o.getPos());
              if (curT==t&&o.solid){
                sol=o;
                break;
              }
            }
            return (t!=null&&t.exists&&(t.type&~Tile.PIT)>0);}
        };
        Array<Tile> tiles = game.getPath(getPos(), 10, this, tt, tt2);
        teleCoolDown=.4f;// dont wanna tile check all the time
        if(tiles!=null)
        {
          teleCoolDown=1.75f;
          {
            game.playSound("tele", getPos(), .5f);
            StarBurst o = new StarBurst();
            o.pos=pos.cpy();
            game.addObj(o);
          }
          setPos(tiles.get(0).getPos());
        }
      }
    }
  }
  void fight(){
    Array<Obj> hit = game.getRadiusAll(2f,getPos());
    for(int i = 0; i<hit.size;++i){
      Obj o = hit.get(i);
      if (o instanceof Grave){
        o.remove=true;
        Skeli s = new Skeli();
        s.setPos(o.getPos());
        game.addObj(s);
      }
    }
  }
  void damaged(float f, Obj hitter){
    if((hp=hp-f)<=0){
      dropInv();
      remove=true;
      game.playSound("manHit", getPos());
      Grave o = new Grave();
      o.pos=pos.cpy();
      game.addObj(o);
    }
  }
}
class Witch extends Obj
{
  Witch(){
    radius=.40f;
    ms=2f;
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
  float changeAngCount=0;
  float changeAngCountMax=.5f;
  float changeAngCountMin=.3f;
  void step(float dt){
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
      walkCount += dt;
      if ((int)(walkCount*8)%2==0)
        tex="witchWalk1.png";
      else
        tex="witchWalk2.png";
      changeAngCount-=dt;
      if (changeAngCount<=0){
        Icmm.ObjTester iot = new Icmm.ObjTester(){
          boolean works(Obj o){
            return (o instanceof Guy);
          }
        };
        Array<Tile> path = game.getPath(getPos(), 17, this, iot);
        if (path!=null&&path.size>2){
          // open any doors in our way
          Tile t = path.get(path.size-3);
          for (Obj o : game.objs){
            if (game.tileAt(o.getPos())==t&&
              o.solid&&o.canTog){
              o.tog(this);
            }
          }
          angle=t.getPos().cpy().sub(getPos()).angle();
        }
        changeAngCount=changeAngCountMin+(float)Math.random()*(changeAngCountMax-changeAngCountMin);
      }
      super.step(dt);
      castCount -= dt;
      if (castCount<=0){
        castCount=minCCount+(float)Math.random()*(maxCCount-minCCount);
        castTimer = 1.5f;
      }
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
class LockedDoor extends Door
{
  static int NONE=0;
  static int LOCKED=1;// use crystals instead (they're cooler)
  static int FIRE=2;
  void setDoorType(int t){ldoorType=t;}
  int ldoorType=NONE;
  LockedDoor(){
    addType(Obj.UNDER);
    tall=true;
    solid=true;
    stationary=true;
    tex="lockedDoor.png";
  }
  String getTex(){
    if(ldoorType==LOCKED)
      return "lockedDoor.png";
    if(ldoorType==FIRE)
      return "fireDoor.png";
    return null;
  }
  void tog(Obj o){
    super.tog(o);
    if(solid){offY=0;}else{offY=1.1f;}
  }
}
class CrystalDoor extends Door
{
  CrystalDoor(){
    addType(Obj.UNDER);
    tall=true;
    solid=true;
    stationary=true;
    tex="crystalDoor.png";
  }
  String getTex(){
    return "crystalDoor.png";
  }
  void tog(Obj o){
    if(!(o instanceof Hand)){
      super.tog(o);
      if(solid){offY=0;}else{offY=1.1f;}
    }
  }
}
class MudWall extends Obj
{
  MudWall(){
    tall=true;
    solid=true;
    stationary=true;
    tex="mudWall.png";
  }
  void init(){
    if(!solid){
      tex=null;
    }
  }
  void tog(Obj o){
    game.playSound("door", getPos());
    solid=!solid;if(solid){tex="mudWall.png";}else{tex=null;}}
}
class Trigger extends Obj
{
  Trigger(){
    radius=.75f;
    addType(Obj.NOCLIP);
  }
  Array<Obj> objsToToggle=new Array<Obj>();void addObjToToggle(Obj o){objsToToggle.add(o);}
  boolean triggered=false;
  void getHit(Obj obj){
    if (!triggered&&obj instanceof Guy){
      triggered=true;
      for(Obj o : objsToToggle){
        o.tog(this);
      }
    }
  }
}
class Door extends Obj
{
  Door(){
    addType(Obj.UNDER);
    canTog=true;
    tall=true;
    solid=true;
    stationary=true;
    tex="doorC.png";
  }
  void init(){
    // make sure we block if we're in a row
    if (angle!=0){
      shoveX=.75f;
    }else{
      shoveY=.75f;
    }
    // set the tile we're on to blocked if we're solid
    if(solid)
      game.tileAtNE(getPos()).addType(Tile.BLOCKED);
  }
  void tog(Obj o){
    game.playSound("door", getPos());
    solid=!solid;if(solid){tex="doorC.png";}else{tex="doorO.png";}
    if(solid)
      game.tileAt(getPos()).addType(Tile.BLOCKED);
    else
      game.tileAt(getPos()).removeType(Tile.BLOCKED);
  }
}
class AI{
  //Icmm game;void setGame(Icmm g){game=g;}
  static int ATTACKING=1;
  static int WALKING=2;
  static int CHARGING=3;
  float getAnimTime(){return 0;}
  int getAnimState(){return 0;}
  void setGame(Icmm g){};
  Obj obj;
  void act(float dt){
    act(obj, dt);
  }
  void act(Obj o, float dt){
  }
  void init(Obj o){
    obj=o;
  }
}
class PatrolAI extends AI{
  Vector2 lastPos=null;
  float patrolDist=7f;
  Array<Tile> path=null;
  void act(Obj obj, float dt){
    if(path!=null){
      if(path.size>2){
        Vector2 targ = path.get(path.size-2).getPos();
        Vector2 diff = targ.cpy().sub(obj.getPos());
        obj.angle = diff.angle();
        if(diff.len()<.2f)
          path.pop();
      }else
        path=null;
    }else{
      final Obj fobj=obj;
      Icmm.TileTester tt = new Icmm.TileTester(){
        boolean works(Tile t){
          if (t!=null&&t.exists&&(!t.checkType(Tile.PIT))){
            Vector2 distLastPos = new Vector2();
            if(lastPos==null)
              distLastPos=t.getPos().cpy().sub(fobj.getPos()).scl(2);// scl 2 cause its the first
            else
              distLastPos=lastPos.cpy().sub(t.getPos());
            if(distLastPos.len()>patrolDist*Math.sqrt(2f))
              return true;
          }
          return false;
        }
      };
      path=obj.game.getPath(obj.getPos(), (int)patrolDist, obj, tt);
      if(path==null)// just to make sure it keeps moving
        path=obj.game.getPath(obj.getPos(), (int)patrolDist*2, obj, tt);
      lastPos=obj.getPos().cpy();
    }
  }
}
class WanderAI extends AI{
  float walkCount=0;
  float minWCount=1f;
  float maxWCount=2f;
  void act(Obj obj, float dt){
    walkCount -= dt;
    if (walkCount<=0){
      walkCount=minWCount+(float)Math.random()*(maxWCount-minWCount);
      obj.angle = ((float)Math.random()*360);
    }
    obj.oldStep(dt);
  }
}
class WormAI extends AI{
  static int ROAM = 1;
  static int BURROW = 2;
  static int BURST = 3;
  static int VULN = 4;
  static int DELAY = 5;
  int state = ROAM;
  float walkCount=0;
  float minWCount=1f;
  float maxWCount=2f;
  float actTime=0;
  void act(Obj obj, float dt){
    actTime+=dt*(.5f+(float)Math.random());
    if(state==ROAM){
      if(actTime>3f){
        Vector2 guyDiff=obj.game.guy.getPos().cpy().sub(obj.getPos());
        float chance=1f/((Worm)obj).connectedWorms.size;
        if(guyDiff.len()<9f&&Math.random()<chance){
          actTime=0;
          state=BURROW;
        }else{
          actTime-=(float)Math.random()*3f+1f;
        }
      }
      obj.addType(Obj.NOCLIP);
      obj.ms=.5f;
      walkCount -= dt;
      if (walkCount<=0){
        walkCount=minWCount+(float)Math.random()*(maxWCount-minWCount);
        obj.angle = ((float)Math.random()*360);
      }
    }else
    if(state==BURROW){
      if(actTime>3f){
        actTime=0;
        state=ROAM;
      }
      obj.addType(Obj.NOCLIP);
      Vector2 guyDiff=obj.game.guy.getPos().cpy().sub(obj.getPos());
      if(guyDiff.len()<.3f){
        state=DELAY;
        obj.ms=0;
        actTime=0;
      }else{
        obj.angle=guyDiff.angle();
        obj.ms=1.5f;
      }
    }else
    if(state==DELAY){
      obj.ms=0;
      if(actTime>.75f){
        state=BURST;
        obj.ms=0;
        actTime=0;
        Vector2 guyDiff=obj.game.guy.getPos().cpy().sub(obj.getPos());
        obj.removeType(Obj.NOCLIP);
        obj.game.playSound("earthImpact",obj.getPos());
        if(guyDiff.len()<.5f){
          obj.game.guy.damaged(1f, obj);
        }
      }
    }else
    if(state==BURST){
      obj.ms=0;
      if(actTime>2f)
        state=ROAM;
    }
    obj.oldStep(dt);
  }
}
class NormAI extends AI{
  static int ROAM=1;
  static int SEEK=2;
  static int FIGHT=3;
  int state = ROAM;
  float actTime=0;
  Array<Tile> path;
  Icmm.ObjTester iot;
  NormAI(){
    iot = new Icmm.ObjTester(){
      boolean works(Obj o){
        return (o instanceof Guy);
      }
    };
  }
  int roamDist=6;
  int seekDist=8;
  float roamSpeed=1f;
  float seekSpeed=1.4f;
  float fightSpeed=.1f;
  float plusDist=.3f;
  float hitTime=.5f;
  float recoilTime=.5f;
  float minWCount=1f;
  float maxWCount=2f;
  float walkCount=0;
  float mustSeekTime=.75f;
  float mustSeekCount=0;
  float getAnimTime(){return actTime;}
  int getAnimState(){
    if(state==FIGHT&&actTime<hitTime){return AI.CHARGING;}
    else if(state==FIGHT){return AI.ATTACKING;}
    else{return AI.WALKING;}
  }
  void act(Obj obj, float dt){
    // for attacking later
    boolean setup=false;
    if(actTime<hitTime)
      setup=true;
    actTime+=dt*(.5f+(float)Math.random());
    if(state==ROAM){
      if(actTime>1.5f){
        path = obj.game.getPath(obj.getPos(), roamDist, obj, iot);
        if(path!=null){
          state=SEEK;
        }
        actTime=0;
      }
      obj.ms=roamSpeed;
      walkCount -= dt;
      if (walkCount<=0){
        walkCount=minWCount+(float)Math.random()*(maxWCount-minWCount);
        obj.angle = ((float)Math.random()*360);
      }
    }else
    if(state==SEEK){
      if(path==null||actTime>1.5f){
        path = obj.game.getPath(obj.getPos(), seekDist, obj, iot);
        if(path==null){
          state=ROAM;
        }
        actTime=0;
      }
      if(path!=null){
        Vector2 diff=null;
        obj.ms=seekSpeed;
        // has to be >=2 otherwise diff might cause null exceptions later when we check if we want to attack
        if(path.size>=2){
          Tile t = path.get(path.size-2);
          diff=t.getPos().cpy().sub(obj.getPos());
          if(diff.len()<.1f){
            // goto next space
            path.pop();
          }
          obj.angle=diff.angle();
        }
        mustSeekCount-=dt;
        if(mustSeekCount<=0&&(path.size<2||(path.size==2&&diff.len()<plusDist))){
          actTime=0;
          state=FIGHT;
        }
      }
    }else
    if(state==FIGHT){
      obj.ms=fightSpeed;
      if(setup&&actTime>=hitTime){
        obj.fight();
      }
      if(actTime>hitTime+recoilTime){
        mustSeekCount=mustSeekTime;
        path=null;
        state=SEEK;
        actTime=0;
      }
    }
    obj.oldStep(dt);
  }
}
class WanderFightAI extends AI{
  WanderAI wai;
  FightAI fai;
  void init(Obj o){
    super.init(o);
    wai=new WanderAI();
    fai=new FightAI();
  }
  float checkGuyCount=0;
  float checkGuyMax=1f;
  static int ACT_WALK=0;
  static int ACT_KILL=1;
  int currAct=ACT_WALK;
  float actTime=0;
  void act(Obj obj, float dt){
    actTime+=dt;// just for animation
    checkGuyCount+=dt;
    if (checkGuyCount>=checkGuyMax){
      checkGuyCount-=checkGuyMax;
      Icmm.ObjTester iot = new Icmm.ObjTester(){
        boolean works(Obj o){
          return (o instanceof Guy);
        }
      };
      Array<Tile> path = obj.game.getPath(obj.getPos(), 17, obj, iot);
      if (path!=null&&path.size<6){
        currAct=ACT_KILL;
      }else{
        currAct=ACT_WALK;
      }
    }
    if (currAct==ACT_KILL){
      fai.act(obj, dt);
    }else{
      obj.canMove=true;
      wai.act(obj, dt);
    }
  }
}
class FleeFindAI extends AI{
  FleeAI flai=new FleeAI();
  FindAI fiai=new FindAI();
  void init(Obj o){
    super.init(o);
  }
  float checkGuyCount=0;
  static int ACT_FLEE=0;
  static int ACT_FIND=1;
  int currAct=ACT_FLEE;
  float actTime=0;
  void act(Obj obj, float dt){
    actTime+=dt;// just for animation
    checkGuyCount+=dt;
    if (checkGuyCount>=1.25f){
      checkGuyCount-=1.25f;
      Array<Tile> path = null;
      Vector2 s = obj.game.sight(obj, obj.getPos(), obj.game.guy, .5f, 10f);
      if (s.x<0){
        path = obj.game.getPath(obj.getPos(), 17, obj, fiai.iot);
      }
      if (path!=null&&path.size<10){
        currAct=ACT_FIND;
      }else{
        currAct=ACT_FLEE;
      }
    }
    if (currAct==ACT_FIND){
      fiai.act(obj, dt);
    }else{
      flai.act(obj, dt);
    }
  }
}
class FleeAI extends AI{
  float checkSightCount;
  Array<Tile> fleePath;
  void act(Obj obj, float dt){
    if (checkSightCount<1.2f){
      if (fleePath!=null){
        if (fleePath.size>0){
          Tile t = fleePath.get(fleePath.size-1);
          Vector2 diff=t.getPos().cpy().sub(obj.getPos());
          if (diff.len()<dt*obj.ms)
            fleePath.removeIndex(fleePath.size-1);
          obj.angle=diff.angle();
        }else{
          fleePath=null;
          checkSightCount=999;
        }
      }
      checkSightCount+=dt+Math.random()*dt;
    }else{
      checkSightCount=0;
      if (fleePath==null||checkSightCount>3.5f){
        Vector2 s = obj.game.sight(obj, obj.getPos(), obj.game.guy, .5f, 10f);
        if (s.x>=0){
          final Obj testObj=obj;
          Icmm.TileTester tt = new Icmm.TileTester(){
            boolean works(Tile t){
              if (t!=null&&t.exists&&(t.type&~Tile.PIT)>0){
                Vector2 s = testObj.game.sight(testObj, t.getPos(), testObj.game.guy, .5f, 10f);
                if (s.x<0){
                  return true;
                }
              }
              return false;
            }
          };
          fleePath = obj.game.getPath(obj.getPos(), 17, obj, tt);
        }
      }
    }
  }
}
class FindAI extends AI{
  float changeAngCount=0;
  float changeAngCountMax=.5f;
  float changeAngCountMin=.3f;
  float actTime=0;
  float actTimeMax=2f;
  float walkCount=0;
  Icmm.ObjTester iot = new Icmm.ObjTester(){
    boolean works(Obj o){
      return (o instanceof Guy);
    }
  };
  void act(Obj obj, float dt){
    if (actTime < actTimeMax)
    {
      obj.canMove=false;
      boolean setup=(actTime<actTimeMax*.5f);
      actTime+=dt;
      if(actTime>actTimeMax*.5f){
        if (setup){
          // call fighting ability
          obj.fight();
        }
      }
    }else
    {
      obj.canMove=true;
      walkCount += dt;
      changeAngCount-=dt;
      if (changeAngCount<=0){
        Array<Tile> path = obj.game.getPath(obj.getPos(), 17, obj, iot);
        if (path!=null&&path.size>1){
          // open any doors in our way
          Tile t = path.get(path.size-2);
          for (Obj o : obj.game.objs){
            if (obj.game.tileAt(o.getPos())==t&&
              o.solid&&o.canTog){
              o.tog(obj);
            }
          }
          obj.angle=t.getPos().cpy().sub(obj.getPos()).angle();
        }
        if (path!=null&&path.size < 3){
          // we should attack, the target is close
          actTime=0f;
        }
        changeAngCount=changeAngCountMin+(float)Math.random()*(changeAngCountMax-changeAngCountMin);
      }
      obj.oldStep(dt);
    }
  }
  void fight(Obj obj){
  }
}
class FightAI extends AI{
  float changeAngCount=0;
  float changeAngCountMax=.5f;
  float changeAngCountMin=.3f;
  float actTime=999;
  float actTimeMax=2f;
  float actTimeHalf=.5f;
  float walkCount=0;
  float getAnimTime(){return walkCount;}
  int getAnimState(){
    if(attacking&&actTime<actTimeMax*actTimeHalf){return AI.CHARGING;}
    else if(attacking){return AI.ATTACKING;}
    else{return AI.WALKING;}
  }
  boolean attacking;// for animation
  void act(Obj obj, float dt){
    if (actTime < actTimeMax)
    {
      attacking=true;
      obj.canMove=false;
      boolean setup=(actTime<actTimeMax*actTimeHalf);
      actTime+=dt;
      if(actTime>actTimeMax*actTimeHalf){
        if (setup){
          // call fighting ability
          obj.fight();
        }
      }
    }else
    {
      attacking=false;
      obj.canMove=true;
      walkCount += dt;
      changeAngCount-=dt;
      if (changeAngCount<=0){
        Icmm.ObjTester iot = new Icmm.ObjTester(){
          boolean works(Obj o){
            return (o instanceof Guy);
          }
        };
        Array<Tile> path = obj.game.getPath(obj.getPos(), 17, obj, iot);
        if (path!=null){
          if(path.size<2){
            actTime=0f;
          }else
          if(path.size>1){
            // open any doors in our way
            Tile t = path.get(path.size-2);
            for (Obj o : obj.game.objs){
              if (obj.game.tileAt(o.getPos())==t&&
                o.solid&&o.canTog){
                o.tog(obj);
              }
            }
            Vector2 diff=t.getPos().cpy().sub(obj.getPos());
            obj.angle=diff.angle();
            if (path.size<3&&diff.len()<.15f){
              // we should attack, the target is close
              actTime=0f;
            }
          }
        }
        changeAngCount=changeAngCountMin+(float)Math.random()*(changeAngCountMax-changeAngCountMin);
      }
      obj.oldStep(dt);
    }
  }
  void fight(Obj obj){
  }
}
public class Icmm extends ApplicationAdapter {
  static int dev=0;
  Array<Character> devPressed=new Array<Character>();// for entering dev code
  static class ObjTester{
    public ObjTester(){}
    Obj user;public ObjTester(Obj u){user=u;}
    boolean works(Obj o){return true;}
  }
  static class TileTester{
    // the default tile tester for rectify
    boolean works(Tile t){return (t!=null&&t.exists&&(t.type&~Tile.PIT)>0);}
  }
  Vector2 sight(Obj obj, Vector2 pos, Obj o, float inc, float max){
    float dist = 0;
    Vector2 dir=o.getPos().cpy().sub(pos).nor();
    float ang=dir.angle();
    for (;dist < max; dist += inc){
      Vector2 currDist = pos.add(dir.cpy().scl(dist));
      Icmm.RectRTN rr = new Icmm.RectRTN();
      rr.tt=new TileTester(){
        boolean works(Tile t){
          return (t!=null&&t.exists);
        }
      };
      Vector3 test=rectify(new Vector3(currDist.x, 0, currDist.y), obj, rr);
      if (test==null)
        return new Vector2(-1, ang);
      else{
        float diff=currDist.cpy().sub(o.getPos()).len();
        if (diff<=inc*2)
          return new Vector2(dist, ang);
      }
    }
    return new Vector2(-1,ang);// not found
  }
  /*
  float sightDist(Vector2 pos, Obj o, float max){
    float dist = 0;
  }*/
  class TileDepth{
    Tile tile;
    Tile lastTile;
    int depth;
  }
  Array<Tile> getPath(Vector2 pos, int dist, Obj obj, TileTester tt){
    return getPath(pos, dist, obj, tt, null);
  }
  Array<Tile> getPath(Vector2 pos, int dist, Obj obj, TileTester tt, TileTester tt2){
    Array<TileDepth> stack = new Array<TileDepth>();
    Array<TileDepth> done= new Array<TileDepth>();
    Tile t= null;
    Array<Tile> rtn= getPath(pos, dist, 0, obj, tt, tt2, stack, done);
    if(rtn!=null&&rtn.size>1){
      rtn.removeIndex(rtn.size-1);// otherwise it looks weird
    }
    return rtn;
  }
  Array<Tile> getPath(Vector2 pos, int dist, int depth, Obj obj, TileTester tt, TileTester tt2, Array<TileDepth> stack, Array<TileDepth> done){
    if (dist<depth){
      return null;// too long
    }
    Vector2 rtn = new Vector2(pos.x, pos.y);
    int tx = (int)Math.round(rtn.x);
    int ty = (int)Math.round(rtn.y);
    if (!tileExists(tx,ty)){
      return null;// this tile doesnt exist
    }
    // this is a tile we're on
    Tile t=tiles[tx][ty];
    if (tt.works(t)){
      // found it
      Array<Tile> rtnt=new Array<Tile>();
      rtnt.add(t);
      Tile curTile=t;
      boolean foundFirst=false;// the first will have lastTile and tile the same (because its the only iteration that adds itself)
      while(!foundFirst){
        if (done.size==0)
          foundFirst=true;// we're directly ontop of it you twat
        for (int c = 0; c < done.size; ++c){
          TileDepth cur = done.get(c);
          if (cur.tile==curTile){
            rtnt.add(cur.lastTile);
            if (curTile==cur.lastTile){
              foundFirst=true;
              break;
            }
            curTile=cur.lastTile;
          }
        }
      }
      return rtnt;
    }
    // it's not here!
    for (int x = -1; x <= 1; ++x)
      for (int y = -1; y <= 1; ++y){
        Vector2 curPos = rtn.cpy().add(x,y);
        Tile sAdd = tileAt(curPos);
        TileDepth td = new TileDepth();
        td.tile=sAdd;
        td.depth=depth;
        td.lastTile=t;
        if (sAdd!=null){
          boolean didThisTile=false;
          for (int c = 0; c < done.size; ++c){
            if (done.get(c).tile==sAdd){
              didThisTile=true;
            }
          }
          if (!didThisTile){
            boolean works=true;
            if (x!=0&&y!=0){
              // check if diag works
              if (tileAt(rtn.cpy().add(x,0))==null||
                tileAt(rtn.cpy().add(0,y))==null){
                // it's not a direct diag
                works=false;
              }
            }
            if (tt2!=null&&!tt2.works(sAdd))
              works=false;
            if(works)
            {
              if (sAdd!=null){
                done.add(td);
                stack.add(td);// push to end
              }
            }
          }
        }
      }
    while(stack.size>0){// should only loop through for one
      TileDepth curT = stack.get(0);// get first
      stack.removeIndex(0);
      Array<Tile> rtnts = getPath(curT.tile.getPos(), dist, curT.depth, obj, tt, tt2, stack, done);
      if (rtnts!=null)
      {
        return rtnts;
      }
    }
    // only if stack runs out(we tried all tiles)
    return null;
  }
  Array<Tile> getPath(Vector2 pos, int dist, Obj obj, ObjTester ot){
    Array<TileDepth> stack = new Array<TileDepth>();
    Array<TileDepth> done= new Array<TileDepth>();
    Tile t= null;
    Array<Tile> rtn= getPath(pos, dist, 0, obj, ot, stack, done);
    if(rtn!=null&&rtn.size>1){
      rtn.removeIndex(rtn.size-1);// otherwise it looks weird
    }
    return rtn;
  }
  Array<Tile> getPath(Vector2 pos, int dist, int depth, Obj obj, ObjTester ot, Array<TileDepth> stack, Array<TileDepth> done){
    if (dist<depth){
      return null;// too long
    }
    Vector2 rtn = new Vector2(pos.x, pos.y);
    int tx = (int)Math.round(rtn.x);
    int ty = (int)Math.round(rtn.y);
    if (!tileExists(tx,ty)){
      return null;// this tile doesnt exist
    }
    // this is a tile we're on
    Tile t=tiles[tx][ty];
    for (int i = 0; i < objs.size; ++i)
    {
      Obj o = objs.get(i);
      Tile compT = tileAt(o.getPos());
      if (compT==t&&ot.works(o)){
        // found it
        Array<Tile> rtnt=new Array<Tile>();
        rtnt.add(t);
        Tile curTile=t;
        boolean foundFirst=false;// the first will have lastTile and tile the same (because its the only iteration that adds itself)
        while(!foundFirst){
          if (done.size==0)
            foundFirst=true;// we're directly ontop of it you twat
          for (int c = 0; c < done.size; ++c){
            TileDepth cur = done.get(c);
            if (cur.tile==curTile){
              rtnt.add(cur.lastTile);
              if (curTile==cur.lastTile){
                foundFirst=true;
                break;
              }
              curTile=cur.lastTile;
            }
          }
        }
        /*
        for (int c = 0; c < rtnt.size; ++c){
          Gdx.app.log("getPath", ""+rtnt.get(c).getPos());
        }*/
        return rtnt;
      }
    }
    // it's not here!
    for (int x = -1; x <= 1; ++x)
      for (int y = -1; y <= 1; ++y){
        Vector2 curPos = rtn.cpy().add(x,y);
        Tile sAdd = tileAt(curPos);
        TileDepth td = new TileDepth();
        td.tile=sAdd;
        td.depth=depth+1;
        td.lastTile=t;
        if (sAdd!=null){
          boolean didThisTile=false;
          for (int c = 0; c < done.size; ++c){
            if (done.get(c).tile==sAdd){
              didThisTile=true;
            }
          }
          if (!didThisTile){
            boolean works=true;
            if (x!=0&&y!=0){
              // check if diag works
              if (tileAt(rtn.cpy().add(x,0))==null||
                tileAt(rtn.cpy().add(0,y))==null){
                // it's not a direct diag
                works=false;
              }
            }
            if(works)
            {
              if (sAdd!=null){
                done.add(td);
                stack.add(td);// push to end
              }
            }
          }
        }
      }
    while(stack.size>0){// should only loop through for one
      TileDepth curT = stack.get(0);// get first
      stack.removeIndex(0);
      Array<Tile> rtnts = getPath(curT.tile.getPos(), dist, curT.depth, obj, ot, stack, done);
      if (rtnts!=null)
      {
        return rtnts;
      }
    }
    // only if stack runs out(we tried all tiles)
    return null;
  }
  Obj getFirst(Obj obj, float deg, float dist, ObjTester ot){
    Vector2 thisp = obj.getPos();
    float tarAngle = obj.getDir().angle();
    float bestDist = dist;
    Obj bestObj = null;
    for (int i = 0; i < objs.size; ++i){
      Obj o = objs.get(i);
      if (o!=obj&&ot.works(o)){
        Vector2 diff = o.getPos().cpy().sub(thisp);
        if (diff.len() < bestDist&&Icmm.compAngle(diff.angle(),tarAngle,/*(float)Math.pow(1-diff.len()/bestDist,.4f)**/deg)){
          bestDist=diff.len();
          bestObj=o;
        }
      }
    }
    return bestObj;
  }
  Array<Obj> getRadiusAll(float rad, Vector2 pos){
    Array<Obj> hit = new Array<Obj>();
    for (int i = 0; i < objs.size; ++i){
      Obj o = objs.get(i);
      if (pos.cpy().sub(o.getPos()).len()<rad)
        hit.add(o);
    }
    return hit;
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
      if (o instanceof Portal)
        Gdx.app.log("portal", "stoploop");
      o.loop.stop(o.soundLoop);
      // reset so we dont try to keep playing it
      o.loop=null;
    }
  }
  void loopSound(String s, Vector2 pos, Obj o){
    loopSound(s,pos,o,1f);
  }
  void loopSound(String s, Vector2 pos, Obj o,float v){
    Vector2 diff = getGuyPos().cpy().sub(pos);
    float maxDist=10f;
    float pow=4f;
    float vol = (float)Math.pow((maxDist-diff.len())/maxDist,pow);
    if (maxDist<diff.len())
      vol=0;
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
        o.loop.setPan(o.soundLoop, -1*pan, vol*v);
      else 
      {
        o.loop = ass.get(s+".ogg", Sound.class);
        o.soundLoop = o.loop.loop(vol*v, 1f, pan);
        o.soundLoopName=s;
      }
    }
  }
  void playSound(String s, Vector2 pos){
    playSound(s, pos, .4f);
  }
  void playSound(String s, Vector2 pos, float v){
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
      ass.get(s+".ogg", Sound.class).play(vol*v, 1f, pan);
    }
  }
  Vector2 getCurrCamPos2(){
    return new Vector2(currCam.position.x,currCam.position.z);
  }
  Vector3 getCurrCamPos(){
    return currCam.position.cpy();
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
    float compAng=normAngle(ang1-ang2);
    return (Math.abs(compAng)<degs);
  }
  void die(){dying=true;}
  void win(){dying=true;winning=true;}
  boolean dying=false;
  boolean winning=false;
  float deathTimer = 0;
  Camera cam;
  Camera cam2;
  float swivelAngle;
  float guyHeight=.75f;
  float guyReach=1.5f;
  float diffHit=1.25f;
  boolean swiveling=false;
  void tweenAng(float ang){swiveling=true;swivelAngle=ang;};
  Camera uicam;
  Camera currCam;
  ShaderProgram sp;
  static AssetManager ass = new AssetManager(){
    public synchronized <T> T get (String fileName, Class<T> type) {
      if(isLoaded(fileName))
        return super.get(fileName, type);
      else{
        if(type==Texture.class)
          return (T)Icmm.white;
        if(type==Sound.class)
          return (T)Icmm.blank;
      }
      return null;
    }
  };
  Mesh floor;
  Mesh wall;
  Mesh tunnelWall;
  Mesh pitWall;
  Mesh uiitem;
  Mesh fullui;
  Mesh inWorld;
  Mesh inWorldR;
  Mesh pedestal;
  Mesh pedestalTop;
  Mesh inWorldTall;
  static Texture red;
  static Texture white;
  static Sound blank;
  int srtx = 0;
  int endx = 100;
  int srty = 0;
  int endy = 100;
  Music introMusic;
  float introMusicMaxVol=.4f;
	
  float tgh=1.5f;// tunnel gap height
	@Override
	public void create () {
    {
      Pixmap p = new Pixmap(1,1,Pixmap.Format.RGBA8888);
      p.setColor(1,0,0,1);p.drawPixel(0,0);
      red = new Texture(p);
    }
    {
      Pixmap p = new Pixmap(1,1,Pixmap.Format.RGBA8888);
      p.setColor(1,1,1,1);p.drawPixel(0,0);
      white = new Texture(p);
    }
    blank=Gdx.audio.newSound(Gdx.files.internal("blank.ogg"));
    //asset loading
    //ass.load("introMusic.ogg", Sound.class);
    introMusic=Gdx.audio.newMusic(Gdx.files.internal("adiabloclone.ogg"));
    introMusic.play();
    introMusic.setLooping(true);
    introMusic.setVolume(introMusicMaxVol);
    ass.load("intro1.png", Texture.class);
    ass.load("intro2.png", Texture.class);
    ass.load("intro3.png", Texture.class);
    ass.load("ratD.ogg", Sound.class);
    ass.load("spider.ogg", Sound.class);
    ass.load("splat.ogg", Sound.class);
    ass.load("splatD.ogg", Sound.class);
    ass.load("tele.ogg", Sound.class);
    ass.load("electric.ogg", Sound.class);
    ass.load("earthImpact.ogg", Sound.class);
    ass.load("deepHurt.ogg", Sound.class);
    ass.load("deepDead.ogg", Sound.class);
    ass.load("drinkPot.ogg", Sound.class);
    ass.load("short.ogg", Sound.class);
    ass.load("dig.ogg", Sound.class);
    ass.load("portal.ogg", Sound.class);
    ass.load("dogD.ogg", Sound.class);
    ass.load("click.ogg", Sound.class);
    ass.load("cry.ogg", Sound.class);
    ass.load("door.ogg", Sound.class);
    ass.load("witchD.ogg", Sound.class);
    ass.load("wizD.ogg", Sound.class);
    ass.load("manHit.ogg", Sound.class);
    ass.load("armorHit.ogg", Sound.class);
    ass.load("fire.ogg", Sound.class);
    ass.load("fireW.ogg", Sound.class);
    ass.load("swordW.ogg", Sound.class);
    ass.load("match.ogg", Sound.class);
    ass.load("moveTut1.png", Texture.class);
    ass.load("moveTut2.png", Texture.class);
    ass.load("switchTut1.png", Texture.class);
    ass.load("switchTut2.png", Texture.class);
    ass.load("spaceTut1.png", Texture.class);
    ass.load("spaceTut2.png", Texture.class);
    ass.load("dog.png", Texture.class);
    ass.load("dwarfSheet.png", Texture.class);
    ass.load("evilWiz.png", Texture.class);
    ass.load("goopB.png", Texture.class);
    ass.load("goopPile.png", Texture.class);
    ass.load("goopF.png", Texture.class);
    ass.load("goopS.png", Texture.class);
    ass.load("purp.png", Texture.class);
    ass.load("starBurst1.png", Texture.class);
    ass.load("starBurst2.png", Texture.class);
    ass.load("starBurst3.png", Texture.class);
    ass.load("goblin.png", Texture.class);
    ass.load("goblinF.png", Texture.class);
    ass.load("bookFBH2.png", Texture.class);
    ass.load("bookPH.png", Texture.class);
    ass.load("bookPH2.png", Texture.class);
    ass.load("bookPB.png", Texture.class);
    ass.load("zap.png", Texture.class);
    ass.load("bookFB.png", Texture.class);
    ass.load("bookFBH.png", Texture.class);
    ass.load("mudWall.png", Texture.class);
    ass.load("spiderA1.png", Texture.class);
    ass.load("spiderA2.png", Texture.class);
    ass.load("necro1.png", Texture.class);
    ass.load("necro2.png", Texture.class);
    ass.load("stone.png", Texture.class);
    ass.load("dirt.png", Texture.class);
    ass.load("pedestal.png", Texture.class);
    ass.load("crystal.png", Texture.class);
    ass.load("wormDig.png", Texture.class);
    ass.load("wormOut.png", Texture.class);
    ass.load("wormUp.png", Texture.class);
    ass.load("phono1.png", Texture.class);
    ass.load("phono2.png", Texture.class);
    ass.load("expBarrel.png", Texture.class);
    ass.load("expBarrel2.png", Texture.class);
    ass.load("bomb1.png", Texture.class);
    ass.load("bombHolder.png", Texture.class);
    ass.load("bomb2.png", Texture.class);
    ass.load("bombH1.png", Texture.class);
    ass.load("bombH2.png", Texture.class);
    ass.load("flaskR1.png", Texture.class);
    ass.load("flaskR2.png", Texture.class);
    ass.load("flaskR1H.png", Texture.class);
    ass.load("flaskR2H.png", Texture.class);
    ass.load("flaskG1.png", Texture.class);
    ass.load("flaskG2.png", Texture.class);
    ass.load("flaskG1H.png", Texture.class);
    ass.load("flaskG2H.png", Texture.class);
    ass.load("flaskE.png", Texture.class);
    ass.load("flaskEH.png", Texture.class);
    ass.load("crystalDoor.png", Texture.class);
    ass.load("reliquary.png", Texture.class);
    ass.load("reliquaryDone.png", Texture.class);
    ass.load("reliquaryDone2.png", Texture.class);
    ass.load("fireDoor.png", Texture.class);
    ass.load("torch.png", Texture.class);
    ass.load("torchF1.png", Texture.class);
    ass.load("torchF2.png", Texture.class);
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
    ass.load("spiderW1.png", Texture.class);
    ass.load("spiderW2.png", Texture.class);
    ass.load("spider.png", Texture.class);
    ass.load("sign.png", Texture.class);
    ass.load("gasBall.png", Texture.class);
    ass.load("wiz.png", Texture.class);
    ass.load("wizC.png", Texture.class);
    ass.load("wizW1.png", Texture.class);
    ass.load("wizW2.png", Texture.class);
    ass.load("wizD1.png", Texture.class);
    ass.load("wizD2.png", Texture.class);
    ass.load("wizD3.png", Texture.class);
    ass.load("skeli.png", Texture.class);
    ass.load("skeliA1.png", Texture.class);
    ass.load("skeliA2.png", Texture.class);
    ass.load("skeliW1.png", Texture.class);
    ass.load("skeliW2.png", Texture.class);
    ass.load("knight.png", Texture.class);
    ass.load("knightA1.png", Texture.class);
    ass.load("knightA2.png", Texture.class);
    ass.load("knightW1.png", Texture.class);
    ass.load("knightW2.png", Texture.class);
    ass.load("knightD1.png", Texture.class);
    ass.load("knightD2.png", Texture.class);
    ass.load("knightD3.png", Texture.class);
    ass.load("knightD4.png", Texture.class);
    ass.load("knightD5.png", Texture.class);
    ass.load("knightD6.png", Texture.class);
    ass.load("portal1.png", Texture.class);
    ass.load("portal2.png", Texture.class);
    ass.load("portal3.png", Texture.class);
    ass.load("wizGoo1.png", Texture.class);
    ass.load("wizGoo2.png", Texture.class);
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
    ass.load("chest.png", Texture.class);
    ass.load("chestO.png", Texture.class);
    ass.load("lionF.png", Texture.class);
    ass.load("lionFF.png", Texture.class);
    ass.load("lionSF.png", Texture.class);
    ass.load("lionS.png", Texture.class);
    ass.load("lionB.png", Texture.class);
    init();
    //reset();
  }
  void removeObjs(){
    for (int i = objs.size-1; i >= 0; --i){
      Obj o = objs.get(i);
      if (o.remove){
        if (o.holder!=null){
          o.holder.fromInv(o,null);
        }
        stopLoop(o);
        objs.removeIndex(i);
      }
    }
  }
  void forceRemoveObjs(){
    for (int i = objs.size-1; i >= 0; --i){
      Obj o = objs.get(i);
      stopLoop(o);
      objs.removeIndex(i);
    }
  }
  int level=0;
  void init(){
    ShaderProgram.pedantic = true;
    sp = new ShaderProgram(Gdx.files.internal("vert.glsl"), Gdx.files.internal("frag.glsl"));
    Gdx.app.log("icmm", "shader log:"+sp.getLog());
    uicam = new OrthographicCamera(1,1);
    uicam.near = .01f;
    uicam.position.set(0,0,1);
    uicam.direction.set(0,0,-1);
    uicam.update();// only do this here?
    float ratio = (float)Gdx.graphics.getHeight()/(float)Gdx.graphics.getWidth();
    cam = new PerspectiveCamera(100,1f,ratio);
    cam.near = .01f;
    cam.position.set(0,guyHeight,0);
    cam.direction.set(0,0,1);
    cam2 = new PerspectiveCamera(100,1f,ratio);
    cam2.near = .01f;
    cam2.position.set(0,.75f,0);
    cam2.direction.set(0,0,1);
    cam2.update();
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
    pitWall = new Mesh(true, 4, 6,
      new VertexAttribute(VertexAttributes.Usage.Position, 3, "a_position"),
      new VertexAttribute(VertexAttributes.Usage.TextureCoordinates, 2, "a_uv"));
    pitWall.setVertices(new float[]{
      0-.5f,-wh,0-.5f,0,wh,
      0-.5f,-wh,0+.5f,1,wh,
      0-.5f,0,0+.5f,1,0,
      0-.5f,0,0-.5f,0,0
    });
    pitWall.setIndices(new short[]{
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
    fullui = new Mesh(true, 4, 6,
      new VertexAttribute(VertexAttributes.Usage.Position, 3, "a_position"),
      new VertexAttribute(VertexAttributes.Usage.TextureCoordinates, 2, "a_uv"));
    fullui.setVertices(new float[]{
      -.5f,.5f,0, 0,0,
      -.5f,-.5f,0,0,1,
      .5f,-.5f,0, 1,1,
      .5f,.5f,0,  1,0
    });
    fullui.setIndices(new short[]{
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
    {
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
    }
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
  }
  void reset(){
    init();
    possess=null;
    tiles=new Tile[endx][endy];
    for (int x=srtx;x<endx;++x)
      for (int y=srty;y<endy;++y)
        tiles[x][y] = new Tile().setPos(x,y);
    swiveling=false;
    forceRemoveObjs();
    Gdx.app.log("icmm", "reset");
    anim=null;
    dying=false;
    winning=false;
    deathTimer=0;
    objs = new Array<Obj>();
    inv = new Array<Obj>();
    guy=new Guy();
    addObj(guy);
    held=new Hand();
    addObj(held);
    toInv(held);
    {
      Sword o = new Sword();
      addObj(o);
      toInv(o);
    }
    if (level==0)
    {
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
        //addObj(o);
      }
      {
        Obj o = new Wiz();
        o.setPos(7,15);
        //addObj(o);
      }
      {
        Obj w=new Knight();
        w.setAI(new WanderFightAI());
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
        o.setAI(new FleeAI());
        o.setPos(1,3);
        addObj(o);
      }
      {
        Obj o=new Dog();
        o.setPos(2,3);
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
      {
        Obj o = new Portal();
        o.setPos(7,14);
        addObj(o);
      }
      tileAt(1,2).type = Tile.PIT;
      }
      {
        Obj o=new Dog();
        o.setPos(8,15);
        //addObj(o);
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
    }else
    if (level==1){
      for (int y = 0; y < 10; ++y)
        tileAt(0,y).setExists(true).type |= Tile.TUNNEL;
      for (int x = 0; x < 5; ++x)
        tileAt(x,10).setExists(true).type |= Tile.TUNNEL;
      {
        Obj o = new Knight();
        o.setPos(0,4);
        {
          Flask f = new Flask();
          f.setFlaskType(Flask.HEALTH);
          addObj(f);
          o.toInv(f,null);
        }
        addObj(o);
      }
      for (int x = 5; x < 10; ++x)
        for (int y = 3; y < 7; ++y)
          tileAt(x,y).setExists(true);
      {
        Obj o = new Pedestal();
        o.setPos(7,4);
        addObj(o);
      }
      {
        Obj o = new LionHead();
        o.setPos(7,4);
        o.angle=90;
        addObj(o);
      }
      for (int x = 10; x < 14; ++x)
        tileAt(x,5).setExists(true).type |= Tile.TUNNEL;
      for (int x = 14; x < 17; ++x)
        for (int y = 4; y < 9; ++y)
          tileAt(x,y).setExists(true);
      {
        Obj w = new Knight();
        w.setPos(15,5);
        w.setAI(new WanderFightAI());
        addObj(w);
        {
          Obj o=new Crystal();
          addObj(o);
          w.toInv(o,null);
        }
      }
      tileAt(7,7).setExists(true).type |= Tile.TUNNEL;
      {
        Obj o = new Pedestal();
        o.setPos(6,6);
        addObj(o);
      }
      {
        CrystalDoor c1 = new CrystalDoor();
        c1.setPos(7,7);
        c1.angle=90;
        addObj(c1);
        Reliquary r = new Reliquary();
        r.addDoor(c1);
        r.setPos(6,6);
        addObj(r);
      }
      for (int x = 5; x < 10; ++x)
        for (int y = 8; y < 12; ++y)
          tileAt(x,y).setExists(true);
      {
        Obj o = new Rat();
        o.setAI(new FleeAI());
        o.setPos(6,11);
        addObj(o);
      }
      {
        LockedDoor l1 = new LockedDoor();
        l1.setDoorType(LockedDoor.FIRE);
        l1.setPos(6,12);
        l1.angle=90;
        addObj(l1);
        Torch t1 = new Torch();
        t1.addDoor(l1);
        t1.setPos(7,11);
        addObj(t1);
        Torch t2 = new Torch();
        t2.addDoor(l1);
        t2.setPos(5,11);
        t1.addTorch(t2);
        t2.addTorch(t1);
        t2.onFire=true;
        addObj(t2);
      }
      for (int x = 9; x < 14; ++x)
        tileAt(x,11).setExists(true).type |= Tile.TUNNEL;
      for (int y = 9; y < 12; ++y)
        tileAt(14,y).setExists(true).type |= Tile.TUNNEL;
      for (int y = 12; y < 15; ++y)
        tileAt(6,y).setExists(true).type |= Tile.TUNNEL;
      for (int x = 4; x < 9; ++x)
        for (int y = 15; y < 20; ++y)
          tileAt(x,y).setExists(true);
      {
        Obj p = new Pedestal();
        p.setPos(6,16);
        addObj(p);
        Obj o = new LionHead();
        o.setPos(6,16);
        o.angle=0;
        addObj(o);
      }
      for (int x = 9; x < 27; ++x)
        tileAt(x,16).setExists(true).type |= Tile.TUNNEL;
      for (int x = 9; x < 27; ++x)
        if (x < 13){
          tileAt(x,18).setExists(true).type |= Tile.TUNNEL;
        }else
        if (x < 19){
          tileAt(x,20).setExists(true).type |= Tile.TUNNEL;
        }else{
          tileAt(x,18).setExists(true).type |= Tile.TUNNEL;
        }
      for (int xi = 11; xi < 24; xi+=6)
        for (int x = xi; x < xi+4; ++x)
          for (int y = 15; y < 20; ++y)
            tileAt(x,y).setExists(true).type &= ~Tile.TUNNEL;
      for (int x = 27; x < 30; ++x)
        tileAt(x,17).setExists(true).type |= Tile.TUNNEL;
      {
        int i=-1;
        for (int x = 11; x < 11+6*3; x += 6){
          ++i;
          {
            CrystalDoor d = new CrystalDoor();
            d.setPos(x-1,16);
            addObj(d);
            {
              Obj p = new Pedestal();
              p.setPos(x,15);
              addObj(p);
              Reliquary r = new Reliquary();
              r.addDoor(d);
              r.setPos(x,15);
              addObj(r);
            }
          }
          Obj w = new Chest();
          w.setPos(x,17);
          addObj(w);
          if(i==0)
          {
            Flask o = new Flask();
            o.setFlaskType(Flask.ANTIDOTE);
            addObj(o);
            w.toInv(o,null);
          }else
          if(i==1)
          {
            Obj o=new Spider();
            addObj(o);
            w.toInv(o,null);
          }else
          if(i==2)
          {
            Obj o = new Knight();
            o.setAI(new WanderFightAI());
            o.setPos(x+1,17);
            addObj(o);
          }
          {
            Obj o=new Crystal();
            addObj(o);
            w.toInv(o,null);
          }
        }
      }
      {
        LockedDoor l1 = new LockedDoor();
        l1.setDoorType(LockedDoor.FIRE);
        l1.setPos(27,17);
        addObj(l1);
        Torch t1 = new Torch();
        t1.addDoor(l1);
        t1.setPos(26,16);
        addObj(t1);
        Torch t2 = new Torch();
        t2.addDoor(l1);
        t2.setPos(26,18);
        t1.addTorch(t2);
        t2.addTorch(t1);
        t2.onFire=true;
        addObj(t2);
      }
      {
        Obj o = new Portal();
        o.setPos(28,17);
        addObj(o);
      }
    }else
    if (level==2){
      guy.setPos(1,1);
      for (int x = 0; x < 3; ++x)
        for (int y = 0; y < 4; ++y)
          tileAt(x,y).setExists(true);
      {
        Obj o = new BombHolder();
        o.setPos(2,2);
        addObj(o);
      }
      for (int y=4; y<6; ++y)
        tileAt(2,y).setExists(true).addType(Tile.TUNNEL);
      for (int x = 1; x < 4; ++x)
        for (int y = 6; y < 9; ++y)
          tileAt(x,y).setExists(true);
      {
        Obj o = new LionHead();
        o.setPos(3,7);
        o.angle=180;
        Obj p = new Pedestal();
        p.setPos(o.getPos());
        addObj(p);
        addObj(o);
      }
      for (int y=9; y<11; ++y)
        tileAt(2,y).setExists(true).addType(Tile.TUNNEL);
      {
        Obj o = new ExpBarrel();
        o.setPos(2,10);
        addObj(o);
      }
      for (int x = 2; x < 5; ++x)
        for (int y = 11; y < 14; ++y)
          tileAt(x,y).setExists(true).addType(Tile.DIRT);
      {
        Worm o = new Worm();
        o.setPos(2, 12);
        addObj(o);
      }
      for (int x=5; x<7; ++x)
        tileAt(x,12).setExists(true).addType(Tile.TUNNEL);
      for (int x = 7; x < 11; ++x)
        for (int y = 11; y < 14; ++y)
          tileAt(x,y).setExists(true);
      {
        Obj o = new Necro();
        Obj c = new Crystal();
        o.toInv(c,null);
        addObj(c);
        o.setPos(9, 13);
        addObj(o);
      }
      {
        Obj o = new Skeli();
        o.setPos(9, 12);
        o.setAI(new WanderFightAI());
        addObj(o);
      }
      {
        Obj o = new Skeli();
        o.setPos(10, 12);
        o.setAI(new WanderFightAI());
        addObj(o);
      }
      {
        Obj o = new Skeli();
        o.setPos(10, 13);
        o.setAI(new WanderFightAI());
        addObj(o);
      }
      {
        Obj o = new Phono();
        o.setPos(8, 12);
        Obj p = new Pedestal();
        p.setPos(o.getPos());
        addObj(p);
        addObj(o);
      }
      for (int y=14; y<16; ++y)
        tileAt(9,y).setExists(true).addType(Tile.TUNNEL);
      for (int x=8; x<11; ++x)
        for (int y=16; y<19; ++y)
          tileAt(x,y).setExists(true).addType(Tile.TUNNEL);
      for (int x=11; x<13; ++x)
        tileAt(x,12).setExists(true).addType(Tile.TUNNEL);
      for (int x=13; x<16; ++x)
        for (int y=10; y<14; ++y)
          tileAt(x,y).setExists(true).addType(Tile.TUNNEL);
      for (int x=16; x<18; ++x)
        tileAt(x,12).setExists(true).addType(Tile.TUNNEL);
      {
        Obj o = new Pedestal();
        Reliquary r = new Reliquary();
        CrystalDoor cd = new CrystalDoor();
        r.addDoor(cd);
        cd.setPos(16,12);
        r.setPos(15,13);
        o.setPos(r.getPos());
        addObj(cd);
        addObj(r);
        addObj(o);
        Portal p = new Portal();
        p.setPos(17,12);
        addObj(p);
      }
      /* set all to dirt
      for (int x=srtx; x<endx;++x)
        for (int y=srty; y<endy;++y){
          if (tileAt(x,y).checkType(Tile.OPEN)){
            tileAt(x,y).addType(Tile.DIRT);
          }
        }
      */
    }else
    if(level==3){
      for (int x = 0; x < 3; ++x)
        tileAt(x,3).setExists(true).addType(Tile.DIRT|Tile.TUNNEL);
      for (int y = 0; y < 3; ++y)
        tileAt(0,y).setExists(true).addType(Tile.DIRT|Tile.TUNNEL);
      {
        Trigger t = new Trigger();
        t.setPos(3,3);
        Obj o = new MudWall();
        o.solid=false;
        o.setPos(2,3);
        t.addObjToToggle(o);
        addObj(t);
        addObj(o);
      }
      {
        Worm w=null;
        Crystal c = new Crystal();
        addObj(c);
        for (int x = 3; x < 11; ++x)
          for (int y = 1; y < 9; ++y){
            tileAt(x,y).setExists(true).addType(Tile.DIRT);
            if((y+x)%13==0){
              Worm o = new Worm();
              o.toInv(c,null);
              if(w==null)
                w=o;
              else
                w.addWorm(o);
              o.setPos(x,y);
              addObj(o);
            }
          }
      }
      for(int y = 9;y<12;++y)
        tileAt(7,y).setExists(true).addType(Tile.DIRT|Tile.TUNNEL);
      tileAt(6,9).setExists(true).addType(Tile.DIRT|Tile.TUNNEL);
      {
        CrystalDoor cd = new CrystalDoor();
        cd.setPos(7,10);
        cd.angle=90;
        Reliquary r = new Reliquary();
        r.setPos(6,9);
        r.addDoor(cd);
        Pedestal p = new Pedestal();
        p.setPos(r.getPos());
        addObj(p);
        addObj(r);
        addObj(cd);
      }
      {
        Obj p = new Portal();
        p.setPos(7,11);
        addObj(p);
      }
    }else
    if(level==4){
      for(int x=0;x<4;++x)
        for(int y=0;y<4;++y)
          tileAt(x,y).setExists(true);
      {
        Obj o = new GoldKnight();
        o.setPos(3,3);
        addObj(o);
      }
      for(int x=4;x<6;++x)
        tileAt(x,3).setExists(true).addType(Tile.TUNNEL);
      for(int x=6;x<10;++x)
        for(int y=1;y<5;++y)
          tileAt(x,y).setExists(true);
      for(int y=4;y<7;++y)
        tileAt(10,y).setExists(true).addType(Tile.TUNNEL);
      {
        Worm w=null;
        Flask f = new Flask();
        f.setFlaskType(Flask.HEALTH);
        addObj(f);
        for(int x=9;x<13;++x)
          for(int y=7;y<11;++y){
            tileAt(x,y).setExists(true).addType(Tile.DIRT);
            if((x+y)%4==0){
              Worm o = new Worm();
              if(w==null)
                w=o;
              else
                w.addWorm(o);
              o.setPos(x,y);
              o.toInv(f,null);
              addObj(o);
            }
          }
      }
      for(int y=11;y<14;++y)
        tileAt(12,y).setExists(true).addType(Tile.TUNNEL);
      tileAt(13,13).setExists(true).addType(Tile.TUNNEL);
      for(int x=14;x<18;++x)
        for(int y=13;y<16;++y)
          tileAt(x,y).setExists(true);
      {
        Book o = new Book();
        o.setPos(15,14);
        o.setBookType(Book.FB);
        addObj(o);
      }
      for(int x=18;x<20;++x)
        tileAt(x,14).setExists(true).addType(Tile.TUNNEL);
      {
        ExpBarrel o = new ExpBarrel();
        o.setPos(18,14);
        addObj(o);
      }
      for(int x=20;x<23;++x)
        for(int y=13;y<16;++y)
          tileAt(x,y).setExists(true);
      for(int y=16;y<18;++y)
        tileAt(21,y).setExists(true).addType(Tile.TUNNEL);
      {
        Door d = new Door();
        d.angle=90;
        d.setPos(21,16);
        addObj(d);
      }
      for(int x=20;x<23;++x)
        for(int y=18;y<21;++y)
          tileAt(x,y).setExists(true);
      {
        Obj o = new Goblin();
        o.setPos(22,19);
        addObj(o);
        Obj c = new Crystal();
        addObj(c);
        o.toInv(c,null);
      }
      for(int x=23;x<26;++x)
        tileAt(x,14).setExists(true).addType(Tile.TUNNEL);
      {
        CrystalDoor cd = new CrystalDoor();
        cd.setPos(23,14);
        Reliquary r = new Reliquary();
        r.setPos(22,13);
        r.addDoor(cd);
        Pedestal p = new Pedestal();
        p.setPos(r.getPos());
        addObj(p);
        addObj(r);
        addObj(cd);
      }
      {
        Portal p = new Portal();
        p.setPos(25,14);
        addObj(p);
      }
    }else
    if(level==5){
      // this code makes 4 rooms
      for(int r=0;r<4;++r){
        int xp=r%2*6;
        int yp=r/2*6;
        for(int x=0;x<4;++x)
          for(int y=0;y<4;++y)
            tileAt(x+xp,y+yp).setExists(true);
      }
      for(int x=4;x<6;++x){
        tileAt(x,1).setExists(true).addType(Tile.TUNNEL);
        tileAt(x,7).setExists(true).addType(Tile.TUNNEL);
      }
      for(int y=4;y<6;++y){
        tileAt(1,y).setExists(true).addType(Tile.TUNNEL);
        tileAt(7,y).setExists(true).addType(Tile.TUNNEL);
      }
      // getting out
      
      // add the goop/crystal
      {Obj o = new Goop();
        o.setPos(2,2);
        addObj(o);
      }
      {Obj o = new Knight();
        o.setPos(2,8);
        addObj(o);
      }
      {Obj o = new Knight();
        o.setPos(8,2);
        addObj(o);
      }
      {Obj o = new GoldKnight();
        o.setPos(8,2);
        Obj c = new Crystal();
        o.toInv(c,null);
        addObj(c);
        addObj(o);
      }
      for(int x=10;x<16;++x)
        tileAt(x,8).setExists(true).addType(Tile.TUNNEL);
      {
        Pedestal p = new Pedestal();
        Reliquary r = new Reliquary();
        r.setPos(9,9);
        p.setPos(r.getPos());
        CrystalDoor cd = new CrystalDoor();
        cd.setPos(10,8);
        r.addDoor(cd);
        addObj(p);
        addObj(r);
        addObj(cd);
      }
      {
        Portal p = new Portal();
        p.setPos(12,8);
        addObj(p);
      }
    }else
    if(level==6){
      guy.setPos(7,5);
      for(int x=5;x<9;++x)
        for(int y=5;y<9;++y)
          tileAt(x,y).setExists(true);
      {
        Book o = new Book();
        o.setPos(7,6);
        o.setBookType(Book.FB);
        addObj(o);
      }
      {
        Dwarf o = new Dwarf();
        o.setPos(6, 14);
        addObj(o);
      }
      {
        Wiz o = new Wiz();
        o.setPos(6, 7);
        //addObj(o);
      }
      for(int y=9;y<12;++y)
        tileAt(6,y).setExists(true).addType(Tile.TUNNEL);
      for(int x=6;x<10;++x)
        for(int y=12;y<15;++y)
          tileAt(x,y).setExists(true);
      for(int y=15;y<17;++y)
        tileAt(6,y).setExists(true).addType(Tile.TUNNEL);
      {
        Door d = new Door();
        d.setPos(6,15);
        d.angle=90;
        addObj(d);
      }
      for(int x=4;x<8;++x)
        for(int y=17;y<20;++y)
          tileAt(x,y).setExists(true);
      {
        Book o = new Book();
        o.bookType=Book.POSSESS;
        o.setPos(6, 13);
        addObj(o);
      }
      {
        Rat o = new Rat();
        o.setPos(5, 18);
        addObj(o);
      }
      for(int x=10;x<14;++x)
        tileAt(x,13).setExists(true).addType(Tile.TUNNEL);
      for(int x=2;x<6;++x)
        tileAt(x,13).setExists(true).addType(Tile.TUNNEL);
      {Obj o = new Door();o.setPos(5,13);addObj(o);}
      {Obj o = new Door();o.setPos(10,13);addObj(o);}
      {Obj o = new Door();o.setPos(2,13);addObj(o);}
      {Obj o = new Door();o.setPos(13,13);addObj(o);}
      for(int x=0;x<2;++x)
        for(int y=12;y<15;++y)
          tileAt(x,y).setExists(true);
      for(int x=14;x<16;++x)
        for(int y=12;y<15;++y)
          tileAt(x,y).setExists(true);
      int pos=(Math.random()<.5f?1:0);
      for(int y=12;y<15;++y){
        int x=(pos==1?1:14);
        Dwarf d = new Dwarf();
        d.setPos(x,y);
        addObj(d);
      }
      {
        int x=(pos==1?15:0);
        Portal p = new Portal();
        p.setPos(x,13);
        addObj(p);
      }
      {
        Obj[] os=new Obj[]{new GoldKnight(), new Wiz(), new Goblin(), new Dog()};
        Obj o = os[(int)Math.floor(Math.random()*os.length)];
        o.setPos(5, 18);
        //addObj(o);
      }
    }
	}
  Obj anim = null;
  Obj held;
  Array<Obj> inv = new Array<Obj>();
  Array<Obj> objs=new Array<Obj>();
  Guy guy;
  Tile[][] tiles=new Tile[endx][endy];
  void toInv(Obj o){
    o.holder=guy;
    o.inWorld=false;
    inv.add(o);
    switchTo(o);
  }
  void fromInv(Obj o){
    if (o==anim)
      anim=null;
    o.holder=null;
    switchInv(-1);
    o.inWorld=true;
    inv.removeValue(o, true);
    held=inv.get(0);
  }
  void switchTo(Obj o){
    held=o;
  }
  static int HAND=1;
  static int WEAP=2;
  static int MAGIC=3;
  static int POTS=4;
  static int MISC=5;
  long invNumToItemType(int num){
    if(num==HAND)return Obj.HAND;
    if(num==WEAP)return Obj.WEAP;
    if(num==MAGIC)return Obj.MAGIC;
    if(num==POTS)return Obj.POTS;
    if(num==MISC)return Obj.MISC;
    return 0;// nothing
  }
  void switchInvTo(int num){
    /*old and simple
    if(num-1<inv.size)
      held=inv.get(num-1);
    */
    // category method:
    int pastHeld=0;
    Obj mayb=null;// first item of cat (will switch to if we're not holding a further cat, or theres none past that one)
    long itemType=invNumToItemType(num);
    for (int i = 0; i < inv.size; ++i){
      Obj o = inv.get(i);
      if (o==held){
        pastHeld=1;
      }else
      if(o.checkItemType(itemType)){
        if(pastHeld==1||mayb==null)//only choose it if we're past held or its the first
          mayb=o;
        if(pastHeld==1){// first since found held in cat
          break;
        }
      }
    }
    // mayb is now defs the right one
    if(mayb!=null)
      held=mayb;
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
  Tile tileAtNE(Vector2 pos){
    int x = (int)Math.round(pos.x);
    int y = (int)Math.round(pos.y);
    return tiles[x][y];
  }
  Tile tileAt(Vector2 pos){
    int x = (int)Math.round(pos.x);
    int y = (int)Math.round(pos.y);
    if (tileExists(x,y)){
      return tiles[x][y];
    }
    return null;
  }
  Tile tileAt(int x, int y){
    if (x<srtx)return null;
    if (x>endx-1)return null;
    if (y<srty)return null;
    if (y>endy-1)return null;
    return tiles[x][y];
  }
  boolean tileExists(int x, int y){
    if (x<srtx)return false;
    if (x>endx-1)return false;
    if (y<srty)return false;
    if (y>endy-1)return false;
    return tiles[x][y].exists;
  }
  boolean tileWalkable(int x, int y){
    Tile t = tileAt(x,y);
    if (t!=null&&t.exists&&(t.type&~Tile.PIT)>0)
      return true;
    return false;
  }
  static class RectRTN{
    boolean pushRad=false;
    boolean hit=false;
    Obj obj=null;
    TileTester tt=new TileTester();
    Array<Obj> dontHit;
  }
  Vector3 solidRectify(Vector3 pos, Obj obj, RectRTN rr){
    Vector2 rtn = new Vector2(pos.x, pos.z);
    int x = (int)Math.round(rtn.x);
    int y = (int)Math.round(rtn.y);
    if (rr.tt.works(tileAt(x,y))){
      float margin = .25f;
      //shove out of objs
      for (int i = 0; i < objs.size; ++i){
        Obj o = objs.get(i);
        if (o.inWorld&&o!=obj&&o!=null)
        {
          boolean inDontHit = false;
          if (rr.dontHit!=null)
            for(Obj odh : rr.dontHit){
              if (odh==o)
                inDontHit=true;
            }
          if (o.solid&&!inDontHit){
            float shoveX=o.shoveX;
            float shoveY=o.shoveY;
            float dx = rtn.x-o.pos.x;
            float dy = rtn.y-o.pos.z;
            if (Math.abs(dx) < shoveX &&
              Math.abs(dy) < shoveY){// needs push
              rr.hit=true;
              obj.getHit(o);
              if(o.testRect(obj)&&obj.testRect(o)){
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
      }
      {
        float dx = rtn.x-x;
        float dy = rtn.y-y;
        if (!rr.tt.works(tileAt(x-1,y))&&dx<-margin){
          rr.hit=true;
          obj.getHit(null);
          rtn.add(-dx-margin,0);
        }
        if (!rr.tt.works(tileAt(x+1,y))&&dx>margin){
          rr.hit=true;
          obj.getHit(null);
          rtn.add(-dx+margin,0);
        }
        if (!rr.tt.works(tileAt(x,y-1))&&dy<-margin){
          rr.hit=true;
          obj.getHit(null);
          rtn.add(0,-dy-margin);
        }
        if (!rr.tt.works(tileAt(x,y+1))&&dy>margin){
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
  Vector3 rectify(Vector3 pos, Obj obj){return rectify(pos, obj, new RectRTN());}
  Vector3 rectify(Vector3 pos, Obj obj, RectRTN rr){
    Vector2 rtn = new Vector2(pos.x, pos.z);
    int x = (int)Math.round(rtn.x);
    int y = (int)Math.round(rtn.y);
    if (tileExists(x,y)){
      float margin = .25f;
      //shove out of objs
      if (obj.inWorld){
        for (int i = 0; i < objs.size; ++i){
          Obj o = objs.get(i);
          if (o!=obj&&o.inWorld)
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
                if(obj.testRect(o)&&o.testRect(obj)){//!o.checkType(Obj.NOCLIP)&&!obj.checkType(Obj.NOCLIP)){
                  float totWeight=(!obj.stationary?o.weight:0)+(!o.stationary?obj.weight:0);
                  if (!obj.stationary)
                    rtn.add(diff.nor().scl(-dist*o.weight/totWeight));
                  if (!o.stationary)
                  {
                    Vector2 newOPos = o.getPos().cpy().add(diff.nor().scl(dist*obj.weight/totWeight));
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
      }
    }
    return solidRectify(new Vector3(rtn.x,pos.y,rtn.y),obj,rr);
  }

  // set the opengl color for this obj (green if poisoned)
  void setColor(ShaderProgram sp, Obj obj){
    if (obj.checkStatus(Obj.PSN)){
      float psnW=1-(vizShrinkTime)*.5f;
      sp.setUniformf("u_color", psnW,1,(float)Math.pow(psnW,5f),1);
    }else
      sp.setUniformf("u_color", 1,1,1,1);
  }
  float lsm = 0;// look speed momentum
  float msm = 0;// move speed momentum
  float maxStep = .05f;
  float totTime=0;// for seeding randomness
  float vizShrinkTime=0;// for global vizshrink functs
  Obj possess=null;
  Obj lastPossess=null;
  int tutorial=0;
  int intro=1;
  int introDirection=1;
  float introTimer=-1;
  float tutTimer=0;
  boolean tutI,tutJ,tutK,tutL,tutE,tutQ,tutSPACE;
	@Override
	public void render () {
    float dt = Gdx.graphics.getDeltaTime();
    dt=dt<.1f?dt:.1f;
    totTime+=dt;
    // logic
    boolean needsReset=false;
    if (dt>maxStep){
      Gdx.app.log("over", "flow");
      dt=maxStep;
    }
    // make sure it's loaded first
    if (ass.update()||true)
    {
      if (intro>0){
        float nextTimer=introTimer+dt*(Icmm.dev==1?7f:1.4f);
        boolean justBefore0=introTimer<0&&nextTimer>=0;
        if(intro>1&&Gdx.input.isKeyJustPressed(Input.Keys.Q)){
          introDirection=-1;
          justBefore0=false;
        }else
        if(Gdx.input.isKeyJustPressed(Input.Keys.E)){
          introDirection=1;
          justBefore0=false;
        }
        if (!justBefore0){
          introTimer=nextTimer;
        }
        // fade music
        if(introTimer>0f&&intro==3&&introDirection==1){
          introMusic.setVolume(introMusicMaxVol*(1-introTimer));
        }
        if(introTimer>1f){
          introTimer=-1f;
          if(intro==3&&introDirection==1){
            intro=0;
            introMusic.stop();
            tutorial=1;
            reset();
          }else{
            intro+=introDirection;
          }
        }
      }
      // check for dev code
      Character addPressed=' ';
      if (Gdx.input.isKeyJustPressed(Input.Keys.Z)){addPressed='Z';}
      if (Gdx.input.isKeyJustPressed(Input.Keys.V)){addPressed='V';}
      if (Gdx.input.isKeyJustPressed(Input.Keys.C)){addPressed='C';}
      if (Gdx.input.isKeyJustPressed(Input.Keys.X)){addPressed='X';}
      int maxCharBuf=9;if (addPressed!=' '){
        devPressed.add(addPressed);if(devPressed.size>maxCharBuf){devPressed.removeIndex(0);}
        String devStr="ZCXV";
        if(devPressed.size>=devStr.length()){
          String testDev="";
          for(int i=0;i<devStr.length();++i){
            testDev+=devPressed.get(devPressed.size-devStr.length()+i);}
          if(testDev.equals(devStr)){
            if(Icmm.dev==0){Icmm.dev=1;Gdx.app.log("icmm","dev mode on");}else{
            Icmm.dev=0;Gdx.app.log("icmm","dev mode off");}}
        }
      }
      if (guy!=null)
      { // controls
        if(possess!=null&&possess.remove)
          possess=null;
        guy.pos.set(cam.position);
        held.pos.set(cam.position);
        guy.angle=held.angle=new Vector2(cam.direction.x,cam.direction.z).angle();
        //held.angle=guy.getDir().angle();
        if (!dying){
          float ls = 240f;//lookspeed max
          float lsr = 670f;//lookspeed for reactions
          float lsp = 770f;//lookspeed add per sec
          float lsd = 1270f;//lookspeed deg per sec
          float ms = 5f*guy.ms;//movespeed max
          float msp = 15f;//movespeed add per sec
          float msd = 15f;//movespeed add per sec
          Vector3 newPos = cam.position.cpy();
          Vector3 lookDir = cam.direction.cpy();
          if(possess!=null){
            newPos=possess.pos.cpy();
            lookDir=new Vector3(possess.getDir().x,0,possess.getDir().y).nor();
          }
          // check for tutorial completion
          float tutTimeWait=Icmm.dev==1?0:2.5f;
          if(Icmm.dev==1)
            tutorial=0;
          if (Gdx.input.isKeyJustPressed(Input.Keys.I)){if(tutorial==1&&tutTimer>tutTimeWait){tutI=true;}}
          if (Gdx.input.isKeyJustPressed(Input.Keys.J)){if(tutorial==1&&tutTimer>tutTimeWait){tutJ=true;}}
          if (Gdx.input.isKeyJustPressed(Input.Keys.K)){if(tutorial==1&&tutTimer>tutTimeWait){tutK=true;}}
          if (Gdx.input.isKeyJustPressed(Input.Keys.L)){if(tutorial==1&&tutTimer>tutTimeWait){tutL=true;}}
          if (Gdx.input.isKeyJustPressed(Input.Keys.E)){if(tutorial==2&&tutTimer>tutTimeWait){tutE=true;}}
          if (Gdx.input.isKeyJustPressed(Input.Keys.Q)){if(tutorial==2&&tutTimer>tutTimeWait){tutQ=true;}}
          if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)){if(tutorial==3&&tutTimer>tutTimeWait){tutSPACE=true;}}
          // increment the tutorial accordingly
          if (tutorial==1&&tutI&&tutJ&&tutK&&tutL){
            tutTimer=0;
            tutorial++;
          }
          if (tutorial==2&&tutE&&tutQ){
            tutTimer=0;
            tutorial++;
          }
          if (tutorial==3&&tutSPACE){
            tutTimer=0;
            tutorial=0;
          }
          if (swiveling){
            Vector2 flatDir=new Vector2(cam.direction.x,cam.direction.z);
            float curAng=flatDir.angle();
            float diff=Icmm.normAngle(curAng-swivelAngle);
            if(Math.abs(diff)<30){
              swiveling=false;
            }else{
              float dir=diff/Math.abs(diff);
              if (dir*dt*lsr>Math.abs(diff)){
                swiveling=false;
                cam.rotate(Vector3.Y,diff);
              }else{
                cam.rotate(Vector3.Y,dir*dt*lsr);
              }
            }
          }else{
            if (Gdx.input.isKeyPressed(Input.Keys.L)){
              if(lsm>0)lsm=0;
              lsm-=dt*lsp;
              if (lsm<-ls)
                lsm=-ls;
            }
            if (Gdx.input.isKeyPressed(Input.Keys.J)){
              if(lsm<0)lsm=0;
              lsm+=dt*lsp;
              if (lsm>ls)
                lsm=ls;
            }
            if (!Gdx.input.isKeyPressed(Input.Keys.L)&&!Gdx.input.isKeyPressed(Input.Keys.J)){
              if (lsm>0){
                lsm-=dt*lsd;
                if (lsm<0)
                  lsm=0;
              }
              if (lsm<0){
                lsm+=dt*lsd;
                if (lsm>0)
                  lsm=0;
              }
            }
            if(possess!=null)
              possess.angle-=dt*lsm;
            else{
              guy.angle+=dt*lsm;
              cam.rotate(Vector3.Y, dt*lsm);
            }
          }
          if (Gdx.input.isKeyPressed(Input.Keys.I)){
            if (msm<0)
              msm=0;
            msm+=dt*msp;
            if (msm>ms)
              msm=ms;
          }
          if (Gdx.input.isKeyPressed(Input.Keys.K)){
            if (msm>0)
              msm=0;
            msm-=dt*msp;
            if (msm<-ms)
              msm=-ms;
          }
          if (!Gdx.input.isKeyPressed(Input.Keys.I)&&!Gdx.input.isKeyPressed(Input.Keys.K)){
            if (msm>0){
              msm-=dt*msd;
              if(msm<0)
                msm=0;
            }
            if (msm<0){
              msm+=dt*msd;
              if(msm>0)
                msm=0;
            }
          }
          newPos.add(lookDir.cpy().scl(dt*msm));
          if (Icmm.dev==1&&possess==null){
            cam.position.set(newPos);
          }else{
            newPos = rectify(newPos,possess==null?guy:possess);
            if (newPos != null&&possess==null){
              //guy.setPos(newPos.x,newPos.z);
              cam.position.set(newPos);
            }else
            if(newPos != null&&possess!=null)
              possess.setPos(newPos.x,newPos.z);
          }
          // numbers (maybe inv?)
          int keyHit=-1;
          if(Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)){keyHit=1;}
          if(Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)){keyHit=2;}
          if(Gdx.input.isKeyJustPressed(Input.Keys.NUM_3)){keyHit=3;}
          if(Gdx.input.isKeyJustPressed(Input.Keys.NUM_4)){keyHit=4;}
          if(Gdx.input.isKeyJustPressed(Input.Keys.NUM_5)){keyHit=5;}
          if(Gdx.input.isKeyJustPressed(Input.Keys.NUM_6)){keyHit=6;}
          if(Gdx.input.isKeyJustPressed(Input.Keys.NUM_7)){keyHit=7;}
          if(Gdx.input.isKeyJustPressed(Input.Keys.NUM_8)){keyHit=8;}
          if(Gdx.input.isKeyJustPressed(Input.Keys.NUM_9)){keyHit=9;}
          if(Gdx.input.isKeyJustPressed(Input.Keys.NUM_0)){keyHit=0;}
          if(Icmm.dev==1&&keyHit>=0&&Gdx.input.isKeyPressed(Input.Keys.ENTER)){
            // code this later
            //devPressed.add(addPressed);if(devPressed.size>maxCharBuf){devPressed.removeIndex(0);}
            // go back and get nums
            //Character.isDigit
            level=keyHit;
            needsReset=true;
          }else// not a level target, switch inv now
          if (anim==null&&possess==null){
            if(keyHit>0){//0 is awkward
              switchInvTo(keyHit);
            }
            if (anim==null&&Gdx.input.isKeyJustPressed(Input.Keys.E)){
              switchInv(1);
            }
            if (anim==null&&Gdx.input.isKeyJustPressed(Input.Keys.Q)){
              switchInv(-1);
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.R)){
              if (Icmm.dev==1&&Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)){
                level++;
                needsReset=true;
              }
              if (Icmm.dev==1&&Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT)){
                level--;
                needsReset=true;
              }
              if (Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT)){
                needsReset=true;
              }
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.D)&&anim==null){
              if (!(held instanceof Hand))
                held.drop();
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)&&anim==null&&!guy.checkStatus(Obj.STUN)){
              held.act();
            }
          }else{
            if(possess!=null&&Gdx.input.isKeyJustPressed(Input.Keys.SPACE)){
              possess.ai=new FleeAI();
              possess=null;
            }
          }
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
          cam.position.set(cam.position.x, cam.position.y+(winning?1:-1)*dt*.25f, cam.position.z);
          float rotSpeed=1f;
          if (winning)
            rotSpeed = .05f+deathTimer*.75f;
          cam.direction.rotate(Vector3.Y, rotSpeed*dt*180f);
          if (deathTimer > 5f){
            if (winning)// increase the level number
              level++;
            needsReset=true;
          }
        }
        removeObjs();
      }
      // rendering (drawing)
      float w = Gdx.graphics.getWidth();
      float h = Gdx.graphics.getHeight();
      // for poison:
      if(possess!=null){
        vizShrinkTime+=dt;
        if(vizShrinkTime>1)
          vizShrinkTime=1;
      }
      if(lastPossess!=null){
        vizShrinkTime-=dt;
        if(vizShrinkTime<=0){
          vizShrinkTime=0;
        }
      }
      if(guy!=null&&guy.checkStatus(Obj.PSN)){
        if (guy.psnTimeC<1.0f){
          vizShrinkTime=1-(float)Math.cos(guy.psnTimeC*Math.PI/2f);
        }else
        if (guy.psnTime<1.0f){
          vizShrinkTime=1-(float)Math.cos(guy.psnTime*Math.PI/2f);
        }else
          vizShrinkTime=1;
      }else
        vizShrinkTime=0;
      Gdx.gl.glClearColor(0, .5f*vizShrinkTime, 0, 1);
      Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
      Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
      Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
      Gdx.gl.glEnable(GL20.GL_BLEND);
      sp.begin();
      cam.update();
      // seed randomness (for static)
      sp.setUniformf("u_seed", totTime);
      //float staticPeriodic=.5f+.5f*(float)Math.sin(2f*totTime);
      //sp.setUniformf("u_static", .25f+.70f*staticPeriodic);
      sp.setUniformf("u_static", 0);
      // code for visions:
      sp.setUniformf("u_color", 1,1,1,1);
      // for circ:
      if(guy!=null){
        float circR=(w>h?w:h);
        float outerCirc=(float)Math.sqrt(2);
        float innerCirc=-.1f+(float)Math.sqrt(2);
        float shrink=(float)Math.sqrt(2)-.5f;
        for(int j = 0; j < 2; ++j){
          if (j==0){
            sp.setUniformMatrix("u_projectionViewMatrix", cam.combined);
            if (!guy.checkStatus(Obj.PSN)){
              sp.setUniformf("u_circ", (float)Gdx.graphics.getWidth()/2f,(float)Gdx.graphics.getHeight()/2f,w*outerCirc,w*innerCirc);
            }else{
              sp.setUniformf("u_circ", (float)Gdx.graphics.getWidth()/2f,(float)Gdx.graphics.getHeight()/2f,w*(outerCirc-shrink*vizShrinkTime),w*(innerCirc-shrink*vizShrinkTime));
            }
            setColor(sp,guy);
            currCam = cam;
          }else{
            if(possess!=null){
              cam2.position.set(possess.pos).add(0,guyHeight,0);
              Vector2 diff = possess.getDir();
              cam2.direction.set(diff.x,0,diff.y);
              cam2.update();
              sp.setUniformMatrix("u_projectionViewMatrix", cam2.combined);
              sp.setUniformf("u_circ", (float)Gdx.graphics.getWidth()/2f,(float)Gdx.graphics.getHeight()/2f,w*.35f,w*.3f);
              sp.setUniformf("u_color", 1,1,1,1f);
              currCam = cam2;
            }else{
              sp.setUniformMatrix("u_projectionViewMatrix", cam.combined);
              sp.setUniformf("u_circ", (float)Gdx.graphics.getWidth()/2f,(float)Gdx.graphics.getHeight()/2f,w*.5f,w*.4f);
              sp.setUniformf("u_color", 1,1,1,.5f);
              currCam = cam;
            }
          }
          Gdx.gl.glClear(GL20.GL_DEPTH_BUFFER_BIT);
          sp.setUniformf("u_brightness", 10.0f);
          sp.setUniformf("u_light", currCam.position.cpy().add(0,0,0));
          if (dying&&!winning)
            sp.setUniformf("u_brightness", 10*(1-(deathTimer/5f)));
            //sp.setUniformf("u_light", currCam.position.cpy().add(0,-1*deathTimer*2f,0));
          boolean shouldDrawWalls=true;
          boolean shouldDrawGuy=false;
          boolean shouldDrawObjs=true;
          if (j==1){
            if (Icmm.dev==0){
              shouldDrawWalls=false;
              shouldDrawObjs=false;
            }else{
              shouldDrawWalls=false;
            }
            if(possess!=null){
              shouldDrawWalls=true;
              shouldDrawGuy=true;
              shouldDrawObjs=true;
            }
          }
          // draw walls:
          if (shouldDrawWalls)
          {
            sp.setUniformf("u_texCoords", 0, 0, 1, 1);
            Texture stone  = ass.get("stone.png", Texture.class);
            stone.setWrap(Texture.TextureWrap.Repeat,Texture.TextureWrap.Repeat);
            Texture dirt = ass.get("dirt.png", Texture.class);
            dirt.setWrap(Texture.TextureWrap.Repeat,Texture.TextureWrap.Repeat);
            for (int x = srtx; x < endx; ++x){
              for (int y = srty; y < endy; ++y){
                if (tileExists(x,y)){
                  if (tileAt(x,y).checkType(Tile.DIRT)){
                    dirt.bind();
                  }else{
                    //default color
                    stone.bind();
                  }
                  if ((tileAt(x,y).type&~Tile.PIT)>0)
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
                    if ((t.type&(~Tile.OPEN))>0){
                      Matrix4 mat = new Matrix4();
                      mat.setTranslation(x,0,y);
                      sp.setUniformMatrix("u_objectMatrix", mat);
                      if ((t.type&Tile.PIT)>0)
                        pitWall.render(sp, GL20.GL_TRIANGLES);
                      else
                      if ((t.type&Tile.TUNNEL)>0)
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
                    if ((t.type&(~Tile.OPEN))>0){
                      Matrix4 mat = new Matrix4();
                      mat.translate(x,0,y);
                      mat.rotate(Vector3.Y, 180);
                      sp.setUniformMatrix("u_objectMatrix", mat);
                      if ((t.type&Tile.PIT)>0)
                        pitWall.render(sp, GL20.GL_TRIANGLES);
                      else
                      if ((t.type&Tile.TUNNEL)>0)
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
                    if ((t.type&~Tile.OPEN)>0){
                      Matrix4 mat = new Matrix4();
                      mat.translate(x,0,y);
                      mat.rotate(Vector3.Y, -90);
                      sp.setUniformMatrix("u_objectMatrix", mat);
                      if ((t.type&Tile.PIT)>0)
                        pitWall.render(sp, GL20.GL_TRIANGLES);
                      else
                      if ((t.type&Tile.TUNNEL)>0)
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
                    if ((t.type&~Tile.OPEN)>0){
                      Matrix4 mat = new Matrix4();
                      mat.translate(x,0,y);
                      mat.rotate(Vector3.Y, 90);
                      sp.setUniformMatrix("u_objectMatrix", mat);
                      if ((t.type&Tile.PIT)>0)
                        pitWall.render(sp, GL20.GL_TRIANGLES);
                      else
                      if ((t.type&Tile.TUNNEL)>0)
                        tunnelWall.render(sp, GL20.GL_TRIANGLES);
                    }
                  }
                }
              }
            }
          }
          // draw stuff
          if (shouldDrawObjs)
          {
            // order objs
            Array<Obj> orderedObjs = new Array<Obj>();
            Vector2 thisp = getCurrCamPos2();
            for (int i = 0; i < objs.size; ++i){
              Obj o = objs.get(i);
              if(o!=guy||shouldDrawGuy){// only draw the guy if we're supposed to
                o.renderDist = o.getPos().cpy().sub(thisp).len();
                if (orderedObjs.size==0)
                  orderedObjs.add(o);
                else
                  for (int c = 0; c < orderedObjs.size; ++c){
                    if (o.renderDist > orderedObjs.get(c).renderDist){
                      // items always are infront of chests
                      //if(!(orderedObjs.get(c) instanceof Chest&&o.checkType(Obj.ITEM)))
                      orderedObjs.insert(c,o);
                      break;
                    }else
                    if (c==orderedObjs.size-1){
                      orderedObjs.add(o);
                      break;
                    }
                  }
              }
            }/* leave this for any extra junk?
            if(shouldDrawGuy){
              float renderDist = guy.getPos().cpy().sub(thisp).len();
              for (int i = 0; i <= orderedObjs.size; ++i){
                if(i==orderedObjs.size){
                  orderedObjs.add(guy);
                }else{
                  Obj o = objs.get(i);
                  if(renderDist > o.renderDist){
                    orderedObjs.insert(i,guy);
                    break;
                  }
                }
              }
            }*/
            for (int i = 0; i < orderedObjs.size; ++i){
              Obj o = orderedObjs.get(i);
              if (Icmm.dev==1&&j==1){
                o = orderedObjs.get(orderedObjs.size-i-1);
                Gdx.gl.glClear(GL20.GL_DEPTH_BUFFER_BIT);
              }
              setColor(sp,guy);
              o.draw(sp);
            }
          }
        }
      }
      // draw ui
      {
        Gdx.gl.glClear(GL20.GL_DEPTH_BUFFER_BIT);
        if(guy!=null){
          setColor(sp,guy);
          sp.setUniformf("u_circ", (float)Gdx.graphics.getWidth()/2f,(float)Gdx.graphics.getHeight()/2f,600f,400f);
          if (!dying&&possess==null)
          {
            sp.setUniformf("u_texCoords", 0, 0, 1, 1);
            sp.setUniformMatrix("u_projectionViewMatrix", uicam.combined);
            sp.setUniformf("u_light", uicam.position);
            // held
            sp.setUniformf("u_color", 1f,1f,1f,1f);
            {
              Matrix4 mat = new Matrix4();
              mat.translate(0, 0, .01f);
              sp.setUniformMatrix("u_objectMatrix", mat);
              ass.get(held.getSel(), Texture.class).bind();
              uiitem.render(sp, GL20.GL_TRIANGLES);
            }
            // extra held graphic
            sp.setUniformf("u_color", 1f,1f,1f,1f);
            {
              String sel2=held.getSel2();
              if(sel2!=null){
                Matrix4 mat = new Matrix4();
                mat.translate(-.25f, -.25f, .01f);
                mat.scl(.5f,.5f,1);
                if(held.flipSel2X)
                  mat.scl(-1,1,1);
                sp.setUniformMatrix("u_objectMatrix", mat);
                ass.get(sel2, Texture.class).bind();
                fullui.render(sp, GL20.GL_TRIANGLES);
              }
            }
            // stun bursts
            if(guy.checkStatus(Obj.STUN)){
              float burstTime=.5f;
              for(int i = 0; i < 2;++i){
                String sbt="starBurst1.png";
                float effStunTime=guy.stunTimeC+(i==1?burstTime/2f:0);
                while(effStunTime>burstTime)
                  effStunTime-=burstTime;
                long seed2=(long)((totTime-effStunTime)*100000);
                Random r2=new Random(seed2);
                if(effStunTime>burstTime/3f)
                  sbt="starBurst2.png";
                if(effStunTime>burstTime*2f/3f)
                  sbt="starBurst3.png";
                Matrix4 mat = new Matrix4();
                float x = (r2.nextFloat()-.5f)*2f;
                float y = (r2.nextFloat()-.5f)*2f;
                mat.translate(x*.4f, y*.4f, .011f+.001f*(float)i);
                mat.scl(.5f,.5f,1);
                //if(held.flipSel2X)
                //  mat.scl(-1,1,1);
                sp.setUniformMatrix("u_objectMatrix", mat);
                ass.get(sbt, Texture.class).bind();
                fullui.render(sp, GL20.GL_TRIANGLES);
              }
            }
            sp.setUniformf("u_color", 1f,1f,1f,1f);
            // healthbar
            {
              sp.setUniformf("u_texCoords", 0, 0, 1, 1);
              float barW=.2f;
              float barH=.05f;
              {
                sp.setUniformf("u_color", 1,1,1,1);
                Matrix4 mat = new Matrix4();
                mat.translate(.5f-barW/2f+barW/2f*(1-guy.getHP()), .5f-barH/2f, .02f);
                mat.scl(barW*guy.getHP(), barH, 1);
                sp.setUniformMatrix("u_objectMatrix", mat);
                red.bind();
                fullui.render(sp, GL20.GL_TRIANGLES);
              }
              {
                sp.setUniformf("u_color", .3f,.3f,.3f,1f);
                Matrix4 mat = new Matrix4();
                mat.translate(.5f-barW/2f, .5f-barH/2f, .02f);
                mat.scl(barW, barH, 1);
                sp.setUniformMatrix("u_objectMatrix", mat);
                red.bind();
                fullui.render(sp, GL20.GL_TRIANGLES);
              }
            }
          }
          // tutorial
          if (tutorial>0)
          {
            tutTimer+=dt;
            if (tutTimer>2){
              sp.setUniformf("u_texCoords", 0, 0, 1, 1);
              {
                sp.setUniformf("u_color", 1,1,1,.5f);
                Matrix4 mat = new Matrix4();
                mat.translate(0, 0, .02f);
                mat.scl(1f, 1f, 1);
                sp.setUniformMatrix("u_objectMatrix", mat);
                if(tutorial==1){
                  if(((int)tutTimer)%2==0)
                    ass.get("moveTut1.png",Texture.class).bind();
                  else
                    ass.get("moveTut2.png",Texture.class).bind();
                }else
                if(tutorial==2){
                  if(((int)tutTimer)%2==0)
                    ass.get("switchTut1.png",Texture.class).bind();
                  else
                    ass.get("switchTut2.png",Texture.class).bind();
                }else
                if(tutorial==3){
                  if(((int)tutTimer)%2==0)
                    ass.get("spaceTut1.png",Texture.class).bind();
                  else
                    ass.get("spaceTut2.png",Texture.class).bind();
                }
                fullui.render(sp, GL20.GL_TRIANGLES);
              }
            }
          }
        }
        // intro
        if (intro>0)
        {
          // this took a while to get right:
          Gdx.gl.glClear(GL20.GL_DEPTH_BUFFER_BIT);
          sp.setUniformf("u_circ", (float)Gdx.graphics.getWidth()/2f,(float)Gdx.graphics.getHeight()/2f,600f,400f);
          sp.setUniformf("u_light", uicam.position);
          sp.setUniformf("u_brightness", .03f+6*(1-(float)Math.abs(introTimer)));
          sp.setUniformMatrix("u_projectionViewMatrix", uicam.combined);
          // because I forgot all the glsl stuff I wrote
          sp.setUniformf("u_texCoords", 0, 0, 1, 1);
          {
            sp.setUniformf("u_color", 1,1,1,1);
            Matrix4 mat = new Matrix4();
            mat.translate(0, 0, .02f);
            mat.scl(1f, 1f, 1);
            sp.setUniformMatrix("u_objectMatrix", mat);
            if(intro==1){
              ass.get("intro1.png",Texture.class).bind();
            }else
            if(intro==2){
              ass.get("intro2.png",Texture.class).bind();
            }else
            if(intro==3){
              ass.get("intro3.png",Texture.class).bind();
            }
            fullui.render(sp, GL20.GL_TRIANGLES);
          }
          /* blending wasn't working?
          {
            sp.setUniformf("u_color", 0,0,0,(float)Math.abs(introTimer));
            Matrix4 mat = new Matrix4();
            mat.translate(0, 0, .02f);
            mat.scl(1f, 1f, 1);
            sp.setUniformMatrix("u_objectMatrix", mat);
            white.bind();
            fullui.render(sp, GL20.GL_TRIANGLES);
          }
          */
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
