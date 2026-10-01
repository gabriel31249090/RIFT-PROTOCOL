package rift;

import static rift.World.*;

/** Top-down view in the camera's own right/forward basis, with an exact inverse for clicks. */
final class MapProjection {
    final double cx,cy,scale,sin,cos,originX,originZ;
    MapProjection(double x,double y,double width,double height,double yaw){
        originX=WIDTH/2;originZ=LENGTH/2;cx=x+width/2;cy=y+height/2;sin=Math.sin(yaw);cos=Math.cos(yaw);
        double spanX=WIDTH*Math.abs(cos)+LENGTH*Math.abs(sin),spanY=WIDTH*Math.abs(sin)+LENGTH*Math.abs(cos);
        scale=Math.min(width/spanX,height/spanY);
    }
    MapProjection(double cx,double cy,double radius,double range,double yaw,double worldX,double worldZ){this.cx=cx;this.cy=cy;scale=radius/range;sin=Math.sin(yaw);cos=Math.cos(yaw);originX=worldX;originZ=worldZ;}
    double x(double x,double z){return cx+((x-originX)*cos-(z-originZ)*sin)*scale;}
    double y(double x,double z){return cy-((x-originX)*sin+(z-originZ)*cos)*scale;}
    V world(double x,double y){double right=(x-cx)/scale,forward=-(y-cy)/scale;return new V(originX+right*cos+forward*sin,0,originZ-right*sin+forward*cos);}
}
