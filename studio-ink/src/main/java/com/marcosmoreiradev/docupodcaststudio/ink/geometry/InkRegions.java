package com.marcosmoreiradev.docupodcaststudio.ink.geometry;

import com.marcosmoreiradev.docupodcaststudio.ink.model.*;
import java.util.*;

/** Bounded raster topology of ink only. No UI, image recognition or stage pixels participate. */
public final class InkRegions {
    private final int width, height;
    private final double scale;
    private final int[] labels;

    private InkRegions(int width, int height, double scale, int[] labels) {
        this.width=width; this.height=height; this.scale=scale; this.labels=labels;
    }
    public int width() { return width; }
    public int height() { return height; }
    public int regionAtPixel(int x,int y) { return labels[y*width+x]; }
    public int regionAt(double x,double y) {
        int px=(int)Math.floor(x*scale), py=(int)Math.floor(y*scale);
        return px<0||py<0||px>=width||py>=height ? 0 : Math.max(0,labels[py*width+px]);
    }

    public static InkRegions detect(List<InkStroke> strokes,double logicalWidth,double logicalHeight) {
        return detect(strokes, logicalWidth, logicalHeight, 1024);
    }

    /** Higher bounded density for final exports; interactive detection keeps its smaller budget. */
    public static InkRegions detect(List<InkStroke> strokes,double logicalWidth,double logicalHeight,int maxDimension) {
        if (!Double.isFinite(logicalWidth)||!Double.isFinite(logicalHeight)||logicalWidth<=0||logicalHeight<=0)
            throw new IllegalArgumentException("Invalid canvas dimensions");
        double scale=Math.min(1,Math.max(1, Math.min(4096, maxDimension))/(double)Math.max(logicalWidth,logicalHeight));
        int w=Math.max(1,(int)Math.ceil(logicalWidth*scale)), h=Math.max(1,(int)Math.ceil(logicalHeight*scale));
        boolean[] wall=new boolean[w*h];
        for(InkStroke stroke:strokes) {
            List<InkPoint> points=stroke.points();
            // At most one logical pixel on either side closes small accidental gaps.
            double radius=Math.max(0.6,stroke.width()*scale/2+(stroke.tool()==InkTool.DRAW?scale:0));
            for(int n=0;n<points.size();n++) {
                InkPoint a=points.get(Math.max(0,n-1)), b=points.get(n);
                double ax=a.x()*scale, ay=a.y()*scale, bx=b.x()*scale, by=b.y()*scale;
                if(!Double.isFinite(ax+ay+bx+by)) continue;
                int left=Math.max(0,(int)Math.floor(Math.min(ax,bx)-radius));
                int right=Math.min(w-1,(int)Math.ceil(Math.max(ax,bx)+radius));
                int top=Math.max(0,(int)Math.floor(Math.min(ay,by)-radius));
                int bottom=Math.min(h-1,(int)Math.ceil(Math.max(ay,by)+radius));
                double dx=bx-ax,dy=by-ay, length=dx*dx+dy*dy;
                for(int y=top;y<=bottom;y++) for(int x=left;x<=right;x++) {
                    double t=length==0?0:Math.max(0,Math.min(1,((x+.5-ax)*dx+(y+.5-ay)*dy)/length));
                    double ex=x+.5-ax-t*dx,ey=y+.5-ay-t*dy;
                    if(ex*ex+ey*ey<=radius*radius) wall[y*w+x]=stroke.tool()!=InkTool.ERASE;
                }
            }
        }
        int[] labels=new int[w*h], queue=new int[w*h];
        int region=0;
        for(int i=0;i<labels.length;i++) {
            if(wall[i]||labels[i]!=0) continue;
            int head=0,tail=0;
            queue[tail++]=i; labels[i]=-1;
            boolean outside=false;
            while(head<tail) {
                int p=queue[head++],x=p%w,y=p/w;
                if(x==0||y==0||x==w-1||y==h-1) outside=true;
                if(x>0) tail=enqueue(p-1,wall,labels,queue,tail);
                if(x<w-1) tail=enqueue(p+1,wall,labels,queue,tail);
                if(y>0) tail=enqueue(p-w,wall,labels,queue,tail);
                if(y<h-1) tail=enqueue(p+w,wall,labels,queue,tail);
            }
            int value=outside?-2:++region;
            for(int j=0;j<tail;j++) labels[queue[j]]=value;
        }
        return new InkRegions(w,h,scale,labels);
    }
    private static int enqueue(int p,boolean[] wall,int[] labels,int[] queue,int tail) {
        if(!wall[p]&&labels[p]==0) { labels[p]=-1; queue[tail++]=p; }
        return tail;
    }
}
