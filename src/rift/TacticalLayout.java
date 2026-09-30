package rift;

import static rift.World.*;

/** Carved lanes, linked rotations and two controlled objective rooms, at human scale. */
final class TacticalLayout {
    final World w;final boolean[][] floor=new boolean[NH][NW];
    TacticalLayout(World w){this.w=w;}
    void room(int x,int z,int right,int bottom){for(int iz=z;iz<bottom;iz++)for(int ix=x;ix<right;ix++)floor[iz][ix]=true;}
    void build(){
        room(4,8,29,32);room(109,12,141,40); // Sites
        room(55,2,85,12);room(8,3,139,9);room(11,8,18,17);room(120,8,127,18);
        room(55,112,85,126);room(58,103,65,118);room(76,100,83,117);
        room(20,103,69,110);room(76,99,137,106);
        room(63,43,83,73);room(68,72,76,120);room(69,7,76,44);
        room(23,63,69,71);room(40,60,50,79);room(76,60,118,68);
        room(26,20,46,27);room(40,23,47,39);room(43,34,72,41);
        room(76,36,100,43);room(94,24,101,42);room(97,24,112,31);
        // Outside flank corridors meet the site through a single narrow door.
        room(3,28,10,82);room(4,76,27,83);room(133,38,141,103);room(116,75,140,82);
        if(w.mapIndex==1){
            room(20,30,27,51);room(20,46,39,59);room(32,56,40,83);room(23,77,39,85);room(20,81,27,107);
            room(113,39,121,54);room(104,49,120,62);room(104,59,112,81);room(106,76,121,84);room(113,81,121,103);
        }else{
            room(20,30,27,107);room(113,39,121,103);
        }
        if(w.mapIndex==2){room(59,49,65,67);room(82,48,88,63);}
        boolean[][] used=new boolean[NH][NW];int wall=w.mapIndex==1?0xACBDCE:w.mapIndex==2?0xB49C83:0xAFB7A5;
        // Merge unused cells into solid buildings, not invisible collision fences.
        for(int z=0;z<NH;z++)for(int x=0;x<NW;x++)if(!floor[z][x]&&!used[z][x]){
            int x2=x+1;while(x2<NW&&!floor[z][x2]&&!used[z][x2])x2++;
            int z2=z+1;outer:while(z2<NH){for(int k=x;k<x2;k++)if(floor[z2][k]||used[z2][k])break outer;z2++;}
            for(int iz=z;iz<z2;iz++)for(int ix=x;ix<x2;ix++)used[iz][ix]=true;
            w.building(x,z,x2,z2,7+(x+z)%3,wall);
        }
        w.panel(73,58,3,.20);w.panel(23,36,2,.20);w.panel(116,72,3,.20);
        // Cover breaks sightlines without closing either lane.
        w.crate(14,20,3,3,2.4);w.crate(23,13,2,3,1.25);
        w.container(117,w.mapIndex==2?24:27,124,w.mapIndex==2?27:30,2.7,w.mapIndex==1?0x7D99B0:0xAF8060);
        w.crate(110,17,2,3,1.3);w.crate(68,50,3,3,2.4);w.crate(77,64,3,3,1.3);
        w.crate(20,91,2,4,1.3);w.crate(118,86,2,4,1.3);w.crate(61,114,2,2,1.3);
        w.platform(4,9,10,18,2.4,wall);w.ramp(4,18,10,28,2.4,true,wall);
        w.platform(133,13,140,23,4.2,wall);w.ramp(133,23,140,40,4.2,true,wall);
        w.platform(63,44,68,52,2.4,wall);w.ramp(63,52,68,62,2.4,true,wall);
        EnvironmentArt.signZ(w,"A / "+(w.mapIndex==0?"DOCAS":w.mapIndex==1?"CÚPULA":"CARGA"),16,3,8.02,13,1.3,0xCAE5D3);
        EnvironmentArt.signZ(w,"B / CONTROLE",121,3,12.02,14,1.3,0xCFE1EA);
        EnvironmentArt.signZ(w,"MEIO",73,3,43.02,7,1.2,0xE7C69A);
    }
}
