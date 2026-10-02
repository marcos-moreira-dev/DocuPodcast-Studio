package com.marcosmoreiradev.docupodcaststudio.ink.geometry;
import com.marcosmoreiradev.docupodcaststudio.ink.model.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class InkRegionsTest {
    @Test void joinsSeparateStrokesButRejectsLargeOpeningAndErasedEdge() {
        var strokes=new ArrayList<>(List.of(line(20,20,80,20),line(80,20,80,80),line(80,80,20,80),line(20,80,20,20)));
        var closed=InkRegions.detect(strokes,100,100);
        assertTrue(closed.regionAt(50,50)>0);
        assertEquals(0,closed.regionAt(5,5));
        strokes.remove(3);
        assertEquals(0,InkRegions.detect(strokes,100,100).regionAt(50,50));
        strokes.add(line(20,80,20,20));
        strokes.add(InkStroke.erase(10,List.of(point(15,50),point(25,50))));
        assertEquals(0,InkRegions.detect(strokes,100,100).regionAt(50,50));
    }
    @Test void workingGridIsBoundedEvenForLargeCanvas() {
        var regions=InkRegions.detect(List.of(),16000,9000);
        assertTrue(regions.width()<=1024);
        assertTrue(regions.height()<=1024);
        assertEquals(0,regions.regionAt(500,500));
    }
    private static InkPoint point(double x,double y) { return InkPoint.of(x,y,1,1); }
    private static InkStroke line(double x,double y,double a,double b) { return InkStroke.draw("#000000",2,List.of(point(x,y),point(a,b))); }
}
