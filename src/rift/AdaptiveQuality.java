package rift;

/** Fast reductions, slow recovery: a frame-time budget rather than a 120-frame wait. */
final class AdaptiveQuality {
    static final int[] WIDTHS={384,480,640,768,854,1066};
    int level=2,slow;double average=8,elapsed,headroom;
    int width(){return WIDTHS[level];}
    void reset(int level){this.level=Math.max(0,Math.min(WIDTHS.length-1,level));slow=0;average=8;elapsed=headroom=0;}
    void sample(double millis,int frameLimit){
        if(!Double.isFinite(millis)||millis<=0)return;
        double budget=1000./Math.min(120,Math.max(60,frameLimit));
        average+=(Math.min(millis,250)-average)*.16;double step=Math.max(millis,1000./frameLimit);elapsed+=step;
        slow=average>budget*1.12?slow+1:0;headroom=average<budget*.64?headroom+step:0;
        if(slow>=8&&elapsed>=200&&level>0){level--;slow=0;elapsed=headroom=0;}
        else if(headroom>=3500&&elapsed>=3500&&level<WIDTHS.length-1){level++;elapsed=headroom=0;}
    }
}
