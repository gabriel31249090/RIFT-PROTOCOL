package rift;

import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/** Shared quiet surfaces; color is reserved for status and deliberate actions. */
final class UiTheme {
    static final Color BACKGROUND=new Color(0x12181A),SURFACE=new Color(0x1C2528),
        RAISED=new Color(0x293438),BORDER=new Color(0x3B4A4F),
        OVERLAY=new Color(18,24,27,226),WHITE=new Color(0xF0F3F1),
        MUTED=new Color(0xA8B5B7),CORAL=new Color(0xFF786E),
        MINT=new Color(0xA2DFC6),GOLD=new Color(0xE1C391),INK=new Color(0x142022);
    private UiTheme() {}
    static void panel(Graphics2D g,double x,double y,double w,double h,Color accent) {
        View.rect(g,x,y,w,h,OVERLAY);
        View.line(g,x,y,x+w,y,accent==null?BORDER:accent,1);
    }
    static void control(Graphics2D g,double x,double y,double w,double h,boolean selected,boolean hover) {
        var shape=new RoundRectangle2D.Double(x+.5,y+.5,w-1,h-1,6,6);
        g.setColor(selected?(hover?new Color(0xFF9388):CORAL):hover?RAISED:SURFACE);
        g.fill(shape);g.setStroke(new BasicStroke(1));
        g.setColor(selected?CORAL:hover?MUTED:BORDER);g.draw(shape);
    }
    static int fit(Graphics2D g,String text,int preferred,double maxWidth,boolean bold) {
        int size=preferred;
        while(size>8&&g.getFontMetrics(View.font(size,bold)).stringWidth(text)>maxWidth)size--;
        return size;
    }
    static void bar(Graphics2D g,double x,double y,double w,double h,double amount,Color color) {
        View.rect(g,x,y,w,h,BORDER);View.rect(g,x,y,w*Settings.clamp(amount,0,1),h,color);
    }
}
