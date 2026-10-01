package rift;

/** Screen-space spans cover only the triangle, not its whole bounding box. */
final class TriangleSpan {
    double ax,ay,bx,by,cx,cy,longSlope,upperSlope,lowerSlope;
    int left,right;
    void set(double x0,double y0,double x1,double y1,double x2,double y2){
        if(y1<y0){double t=x0;x0=x1;x1=t;t=y0;y0=y1;y1=t;}
        if(y2<y0){double t=x0;x0=x2;x2=t;t=y0;y0=y2;y2=t;}
        if(y2<y1){double t=x1;x1=x2;x2=t;t=y1;y1=y2;y2=t;}
        ax=x0;ay=y0;bx=x1;by=y1;cx=x2;cy=y2;
        longSlope=(cx-ax)/Math.max(1e-12,cy-ay);upperSlope=(bx-ax)/Math.max(1e-12,by-ay);lowerSlope=(cx-bx)/Math.max(1e-12,cy-by);
    }
    boolean row(int y,int width){
        double yy=y+.5;if(yy<ay-1e-8||yy>cy+1e-8)return false;
        double a=ax+(yy-ay)*longSlope,b=yy<by?ax+(yy-ay)*upperSlope:bx+(yy-by)*lowerSlope;
        left=Math.max(0,(int)Math.ceil(Math.min(a,b)-.50000001));right=Math.min(width-1,(int)Math.floor(Math.max(a,b)-.49999999));return left<=right;
    }
}
