package fc.PrintingApplication.TP2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import fc.Math.Vec2f;
import fc.PrintingApplication.clipper2.Clipper;
import fc.PrintingApplication.clipper2.offset.ClipperOffset;
import fc.PrintingApplication.clipper2.offset.EndType;
import fc.PrintingApplication.clipper2.offset.JoinType;
import fc.Math.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;


import fc.PrintingApplication.clipper2.core.*;

enum Type
{
    ISLAND, HOLE
}

class Contour {
    // On stocke les points du contour dans l'ordre
    public ArrayList<Vec2f> listPoints = new ArrayList<>();
    public ArrayList<Arete> listAretes = new ArrayList<>();
    public int isInternal;  // 0 -> island / 1 -> hole
    
    public Type type;

    @Override public String toString() {
        return "Contour{" + listPoints.size() + " points}";
    }

    public Contour(){}

    public Contour(ArrayList<Arete> l)
    {
        listAretes = new ArrayList<>(l);
    }

    void determineOrientation()
    {
        int sum = 0;
        for(int i = 0; i < listPoints.size() - 1; i++)      //(x2-x1)*(y2+y1)
        {
            sum += (listPoints.get(i+1).x - listPoints.get(i).x) * (listPoints.get(i+1).y + listPoints.get(i).y);
        }

        if (type == null) {
            throw new IllegalArgumentException("type est null (determineOrientation)");
        }
        switch (type) {
            case HOLE:
                if(sum < 0)
                {
                    Collections.reverse(listAretes);
                    Collections.reverse(listPoints);
                }
                break;
            case ISLAND:
                if(sum >= 0)
                {
                    Collections.reverse(listAretes);
                    Collections.reverse(listPoints);
                    //System.out.println("list inversé");
                }
                break;
            default:
                break;
        }
    }


    void getUniquePoints()
    {

        if (listAretes == null || listAretes.isEmpty()) return;

        for(int i = 0; i < listAretes.size(); i++)
        {
            listPoints.add(listAretes.get(i).First);
        }
        Set<Vec2f> set = new LinkedHashSet<>(listPoints);
        listPoints = new ArrayList<>(set);


    }



public float projection(Vec2f start, Vec2f end, Vec2f p)
    {
        
        Vec2f ab = new Vec2f(end.x - start.x, end.y - start.y);
        Vec2f ap = new Vec2f(p.x - start.x, p.y - start.y);

        float scal = ap.dot(ab);
        float lengthSquared = ab.lengthSquared();

        float t = scal / lengthSquared;

        Vec2f newp;

        if(t > 1)
        {
            newp = end;
        }

        else if(t < 0)
        {
            newp = start;
        }

        else
        {
            Vec2f f = ab.mul(t);
            newp = start.add(f); 
        } 

        float distanceSquared = newp.distanceSquared(p);
        return distanceSquared;
    }


public ArrayList<Vec2f> DivideAndConquer(List<Vec2f> pts, float epsilon) {
    if (pts == null || pts.size() <= 2) return new ArrayList<>(pts);
    float eps2 = epsilon * epsilon;

    int start = 0, end = pts.size() - 1;
    int indexMax = -1;
    float dmax2 = -1f;

    for (int i = 1; i < end; i++) {
        float d2 = projection(pts.get(start), pts.get(end), pts.get(i));
        if (d2 > dmax2) {
            dmax2 = d2;
            indexMax = i;
        }
    }

    if (dmax2 > eps2) {
        List<Vec2f> left  = DivideAndConquer(pts.subList(0, indexMax + 1), epsilon);
        List<Vec2f> right = DivideAndConquer(pts.subList(indexMax, end + 1), epsilon);

        ArrayList<Vec2f> out = new ArrayList<>(left.size() + right.size() - 1);
        out.addAll(left);
        out.addAll(right.subList(1, right.size()));
        return out;
    } 
    else 
    {
        ArrayList<Vec2f> out = new ArrayList<>(2);
        out.add(pts.get(start));
        out.add(pts.get(end));
        return out;
    }
}


void recreateAretes()
{
    listAretes = new ArrayList<>();
    for(int i = 0; i < listPoints.size(); i++)
    {
        Arete a = new Arete();
        a.First = listPoints.get(i);
        a.Second = listPoints.get((i+1) % listPoints.size());
        listAretes.add(a);
    }
}

public void remplit(Graphics2D g2d) 
{
    if(type == Type.HOLE) g2d.setColor(Color.BLACK);
    if(type == Type.ISLAND) g2d.setColor(Color.WHITE);

    ArrayList<Arete> aretes = new ArrayList<>(listAretes);

    aretes.sort(Comparator.comparing(a -> a.First.y));	
    float ymin = aretes.get(0).First.y;
    float ymax = aretes.get(aretes.size() - 1).Second.y;


    for(int y = (int)(ymin / Main.resolution); y < (int)(int)(ymax / Main.resolution); y++)
    {
        ArrayList<Arete> intersectlistArete = new ArrayList<>();
        ArrayList<Intersection> intersections = new ArrayList<>();

        for(int i = 0; i < aretes.size(); i++)
        {
            if(aretes.get(i).First.y / Main.resolution > y) break;
            
            if(aretes.get(i).Second.y != aretes.get(i).First.y && aretes.get(i).Second.y / Main.resolution > y) intersectlistArete.add(aretes.get(i));
        }

        for(int i = 0; i < intersectlistArete.size(); i++)
        {
            float pente = (intersectlistArete.get(i).Second.x - intersectlistArete.get(i).First.x) / (intersectlistArete.get(i).Second.y - intersectlistArete.get(i).First.y);
            intersections.add(new Intersection(y, (int) (intersectlistArete.get(i).First.x / Main.resolution + pente * (y - intersectlistArete.get(i).First.y / Main.resolution)),intersectlistArete.get(i)));
        }
        intersections.sort(Comparator.comparing(a -> a.xIntersect));

        for (int i = 0; i < intersections.size(); i +=2)
        {
            g2d.drawLine((int)(intersections.get(i).xIntersect), y, (int)(intersections.get(i + 1).xIntersect), y);
        } 

    }
}

}


class Tranche
{
    ArrayList<Arete> listAretes = new ArrayList<>();
    ArrayList<Contour> listContours = new ArrayList<>();
    ArrayList<Path64> listPath = new ArrayList<>();
    Path64 perimetre;
    ArrayList<Path64> listPerimetre = new ArrayList<>();

    public Tranche(ArrayList<Arete> l)
    {
        listAretes = l;

        findAllContours2();
		JordanTheorem();

        
        for(int i = 0; i < listContours.size(); i++) listContours.get(i).getUniquePoints();
        for(int i = 0; i < listContours.size(); i++)listContours.get(i).determineOrientation();
        
        
        float epsilon = (float) 2 / 20;
        listAretes.clear();
        for(Contour contour : listContours)
        {
            // System.out.println("---------------");
            // System.out.println(contour.listAretes.size());

            contour.listPoints = contour.DivideAndConquer(contour.listPoints, epsilon);
            contour.recreateAretes();
            listAretes.addAll(contour.listAretes);
            // System.out.println(contour.listAretes.size());
        }

        clip3();

    }


    void findAllContours()
    {
        ArrayList<Arete> temp = new ArrayList<>(listAretes);
        
        while(temp.size() > 0)
        {
            
            ArrayList<Arete> Aretes = new ArrayList<>();
            Aretes.add(temp.get(0));
            temp.remove(0);

            boolean add = true;

            while(add)
            {
                add = false;
                ArrayList<Arete> ToRemove = new ArrayList<>();
                for(int i = 0; i < temp.size(); i++)
                {
                    Arete areteTested = temp.get(i);


                    Arete lastArete = Aretes.get(Aretes.size() - 1);

                    if(lastArete.Second.equals(areteTested.First) || lastArete.First.equals(areteTested.Second))
                    {
                        Aretes.add(areteTested);
                        ToRemove.add(areteTested);
                        add = true;
                    }

                    else if(lastArete.First.equals(areteTested.First) || lastArete.Second.equals(areteTested.Second))
                    {
                        Aretes.add(new Arete(areteTested.Second, areteTested.First));
                        ToRemove.add(areteTested);
                        add = true;
                    }
                }

                for(Arete a : ToRemove) temp.remove(a);
            }
            Contour contour = new Contour(Aretes);
            listContours.add(contour);
        }
    }

    void findAllContours2() {
        listContours.clear();
        if (listAretes == null || listAretes.isEmpty()) return;

        ArrayList<Arete> pool = new ArrayList<>(listAretes);

        while (!pool.isEmpty()) {

            Arete start = pool.remove(pool.size() - 1);
            ArrayList<Arete> chain = new ArrayList<>();
            chain.add(start);

            Vec2f head = start.First;   
            Vec2f tail = start.Second;  

            boolean progressed = true;
            while (progressed) {
                progressed = false;

                for (int i = 0; i < pool.size(); i++) {
                    Arete e = pool.get(i);

                    // 1) prolongé fin
                    if (e.First.equals(tail)) 
                    {
                        chain.add(e);
                        tail = e.Second;
                    } 
                    else if (e.Second.equals(tail)) 
                    {
                        chain.add(new Arete(e.Second, e.First)); 
                        tail = e.First;
                    }
                    // Prolongé début
                    else if 
                    (e.Second.equals(head)) 
                    {
                        chain.add(0, e);
                        head = e.First;
                    } 
                    else if 
                    (e.First.equals(head)) 
                    {
                        chain.add(0, new Arete(e.Second, e.First)); // inversée
                        head = e.Second;
                    } 
                    else 
                    {
                        continue;
                    }

                    pool.remove(i);
                    progressed = true;

                    if (head.equals(tail)) {
                        progressed = false;
                    }
                    break;
                }
            }

            listContours.add(new Contour(chain));
        }
    }





    void JordanTheorem()
    {
        for(int i = 0; i < listContours.size(); i++)
        {
            Vec2f firstPoint = listContours.get(i).listAretes.get(0).First;
            
            int nb_traverse = 0;

            for(int j = 0; j < listContours.size(); j++)
            {
                if(i == j) continue;

                ArrayList<Arete> aretes = listContours.get(j).listAretes;

               for(int k = 0; k < aretes.size(); k++)
               {
                    if(aretes.get(k).Intersect(firstPoint)) nb_traverse++;
               }
            }

            listContours.get(i).type = Type.ISLAND;
            if(nb_traverse%2 == 1)listContours.get(i).type = Type.HOLE;
        }
    }


    void determineOrientation()
    {
        for(Contour contour : listContours)
        {
            contour.determineOrientation();
        }
    }



    void reverseHole()
    {
        for(Contour contour : listContours)
        {
            if(contour.type == Type.HOLE)
            {
                
                Collections.reverse(contour.listPoints);

            }
        }
    }


    
ArrayList<Path64> clip() {



    ClipperOffset offset = new ClipperOffset();
    for (Contour contour : listContours) {
        //if (contour.type == Type.HOLE) continue;
        List<Vec2f> positions = contour.listPoints;
        if (positions.size() < 2) continue;

        Path64 path = new Path64(positions.size());
        for (int i = 0; i < positions.size(); i++) {
            long x = Main.width/2  + (long)Math.round(positions.get(i).x / Main.resolution);
            long y = Main.height/2 + (long)Math.round(positions.get(i).y / Main.resolution);
            path.add(new Point64(x, y));
        }
        offset.AddPath(path, JoinType.Miter, EndType.Polygon);
    }

    offset.setMergeGroups(false);

    double delta = -Main.k*( Main.buseDiameter / Main.resolution); 
    while (true) {
        Paths64 solution = new Paths64();
        offset.Execute(delta, solution);  
        if (solution.isEmpty()) break;

        listPath.addAll(solution);

        offset.Clear();
        offset.AddPaths(solution, JoinType.Miter, EndType.Polygon);
    }

    return listPath;
}

ArrayList<Path64> clip2() {

    ClipperOffset offset = new ClipperOffset();

    for (Contour contour : listContours) {
        List<Vec2f> positions = contour.listPoints;
        if (positions.size() < 2) continue;

        Path64 path = new Path64(positions.size());
        for (int i = 0; i < positions.size(); i++) {
            long x = Main.width  / 2 + Math.round(positions.get(i).x / Main.resolution);
            long y = Main.height / 2 + Math.round(positions.get(i).y / Main.resolution);
            path.add(new Point64(x, y));
        }

        if (perimetre == null) {
            perimetre = path;
        }

        offset.AddPath(path, JoinType.Miter, EndType.Polygon);
    }

    offset.setMergeGroups(false);

    double delta = -Main.k * (Main.buseDiameter / Main.resolution);

    while (true) {
        Paths64 solution = new Paths64();
        offset.Execute(delta, solution);
        if (solution.isEmpty()) break;

        listPath.addAll(solution);

        offset.Clear();
        offset.AddPaths(solution, JoinType.Miter, EndType.Polygon);
    }

    return listPath;
}


ArrayList<Path64> clip3() {

    listPath = new ArrayList<>();
    listPerimetre.clear();
    perimetre = null;

    Paths64 subject = new Paths64();
    for (Contour contour : listContours) {
        List<Vec2f> pts = contour.listPoints;
        if (pts == null || pts.size() < 2) continue;

        Path64 path = new Path64(pts.size());
        for (Vec2f v : pts) {
            long x = Main.width  / 2 + Math.round(v.x / Main.resolution);
            long y = Main.height / 2 + Math.round(v.y / Main.resolution);
            path.add(new Point64(x, y));
        }

        /*boolean pos = Clipper.IsPositive(path);
        if (contour.type == Type.ISLAND && !pos) Clipper.ReversePath(path);
        if (contour.type == Type.HOLE   &&  pos) Clipper.ReversePath(path);*/

        subject.add(path);
    }

    if (subject.isEmpty()) return listPath;

    double step = -Main.k * (Main.buseDiameter / Main.resolution);
    if (step >= 0) step = -Math.abs(step);

    Paths64 inset1 = new Paths64();
    {
        ClipperOffset co = new ClipperOffset();
        co.AddPaths(subject, JoinType.Miter, EndType.Polygon);
        co.Execute(step, inset1);
    }

    if (!inset1.isEmpty()) 
    {
        Paths64 ring0 = Clipper.Difference(subject, inset1, FillRule.NonZero);
        listPerimetre.addAll(ring0);
    } 
    else 
    {
        listPerimetre.addAll(subject);
        return listPath; 
    }

    Paths64 current = inset1;
    while (true) {
        Paths64 next = new Paths64();
        ClipperOffset co = new ClipperOffset();
        co.AddPaths(current, JoinType.Miter, EndType.Polygon);
        co.Execute(step, next);
        if (next.isEmpty()) break;

        Paths64 ring = Clipper.Difference(current, next, FillRule.NonZero);
        listPath.addAll(ring);

        current = next;
    }

    return listPath;
}


public BufferedImage dessinerContoursImage(int width, int height, double pxPerUnit, int cx, int cy, int nb) {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = img.createGraphics();
        g2d.setColor(Color.BLACK);
        g2d.fillRect(0, 0, width, height);

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setStroke(new BasicStroke(2f));

        Color[] base = new Color[] {
            Color.RED, Color.GREEN, Color.BLUE, Color.MAGENTA, Color.ORANGE,
            Color.CYAN, Color.PINK, Color.YELLOW, new Color(128, 0, 128), new Color(0, 128, 128)
        };

        g2d.translate(cx, cy);
        
        
        

        for(int i = 0; i < listContours.size(); i++) 
        {
            if(listContours.get(i).type == Type.HOLE) g2d.setColor(Color.RED);
            if(listContours.get(i).type == Type.ISLAND) g2d.setColor(Color.GREEN);

            if (nb > listContours.get(i).listAretes.size()) nb = listContours.get(i).listAretes.size();


            boolean usePoint = true;

            if(!usePoint)
            {
                for(int j = 0; j < listContours.get(i).listAretes.size(); j++) // listContours.get(i).listAretes.size();
                {
                    Arete a1 = listContours.get(i).listAretes.get(j);
                    g2d.drawLine((int) Math.round(a1.First.x / Main.resolution), (int) Math.round(a1.First.y / Main.resolution), (int) Math.round(a1.Second.x / Main.resolution), (int) Math.round(a1.Second.y / Main.resolution));
                    
                } 
            }



            if(usePoint)
            {
                for(int j = 0; j < listContours.get(i).listPoints.size(); j++) // listContours.get(i).listAretes.size();
                {

                    Vec2f p1, p2;

                    if(j == listContours.get(i).listPoints.size()-1)
                    {
                        p1 = listContours.get(i).listPoints.get(j);
                        p2 = listContours.get(i).listPoints.get(0);
                    }
                    else
                    {
                        p1 = listContours.get(i).listPoints.get(j);
                        p2 = listContours.get(i).listPoints.get(j + 1);
                    }

                    g2d.drawLine((int) Math.round(p1.x * pxPerUnit), (int) Math.round(p1.y * pxPerUnit), (int) Math.round(p2.x * pxPerUnit), (int) Math.round(p2.y * pxPerUnit));
                    
                }
            }

        }
        g2d.translate(-cx, -cy);
        g2d.dispose();
        return img;
    }



public BufferedImage dessinerRempit(int width, int height, double pxPerUnit, int cx, int cy) {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = img.createGraphics();

        g2d.setColor(Color.BLACK);
        g2d.fillRect(0, 0, width, height);

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setStroke(new BasicStroke(2f));

        g2d.translate(cx, cy);
        
        g2d.setColor(Color.WHITE);

        if(listAretes.size() < 2) return img;
        listAretes.sort(Comparator.comparing(a -> a.First.y));	

        if (Main.fill)
        {
            fillWithAretes(g2d);
        }
        g2d.setColor(Color.RED);

        for(Path64 per : listPerimetre)
        {
            if (per.size() >= 2)
            {
                for (int i = 0; i < per.size(); i++) 
                {
                    Point64 a = per.get(i);
                    Point64 b = per.get((i + 1) % per.size());

                    int x1 = (int)Math.round((a.x - cx));
                    int y1 = (int)Math.round((a.y - cy));
                    int x2 = (int)Math.round((b.x - cx));
                    int y2 = (int)Math.round((b.y - cy));

                    g2d.drawLine(x1, y1, x2, y2);
                }
            }
        }


        g2d.setStroke(new BasicStroke(2.5f));
            for (int k = 0; k < listPath.size(); k++) {
                Path64 path = listPath.get(k);
                if (path.size() < 2) continue;

                g2d.setColor(Color.BLUE);

                for (int i = 0; i < path.size(); i++) {
                    Point64 a = path.get(i);
                    Point64 b = path.get((i + 1) % path.size());

                    int x1 = (int)Math.round((a.x - cx));
                    int y1 = (int)Math.round((a.y - cy));
                    int x2 = (int)Math.round((b.x - cx));
                    int y2 = (int)Math.round((b.y - cy));

                    g2d.drawLine(x1, y1, x2, y2);
                }
            }


        g2d.translate(-cx, -cy);
        g2d.dispose();
        return img;
    }



public BufferedImage dessinerContoursEtOffsetsImage(
        int width, int height,
        double pxPerUnit, int cx, int cy
) {
    BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g2d = img.createGraphics();

    g2d.setColor(Color.BLACK);
    g2d.fillRect(0, 0, width, height);


    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g2d.setStroke(new BasicStroke(2f));

    Color[] palette = new Color[] {
            Color.RED, Color.GREEN, Color.BLUE, Color.MAGENTA, Color.ORANGE,
            Color.CYAN, Color.PINK, Color.YELLOW, new Color(128,0,128), new Color(0,128,128)
    };

    g2d.translate(cx, cy);

    for (int i = 0; i < listContours.size(); i++) {
        if (listContours.get(i).type == Type.HOLE)   g2d.setColor(Color.RED.darker());
        if (listContours.get(i).type == Type.ISLAND) g2d.setColor(Color.GREEN.darker());

        for (int j = 0; j < listContours.get(i).listPoints.size(); j++) {
            Vec2f p1 = listContours.get(i).listPoints.get(j);
            Vec2f p2 = (j == listContours.get(i).listPoints.size() - 1)
                    ? listContours.get(i).listPoints.get(0)
                    : listContours.get(i).listPoints.get(j + 1);

            int x1 = (int)Math.round(p1.x / pxPerUnit);
            int y1 = (int)Math.round(p1.y / pxPerUnit);
            int x2 = (int)Math.round(p2.x / pxPerUnit);
            int y2 = (int)Math.round(p2.y / pxPerUnit);
            g2d.drawLine(x1, y1, x2, y2);
        }
    }

    g2d.setStroke(new BasicStroke(2.5f));
    for (int k = 0; k < listPath.size(); k++) {
        Path64 path = listPath.get(k);
        if (path.size() < 2) continue;

        g2d.setColor(palette[k % palette.length]);

        for (int i = 0; i < path.size(); i++) {
            Point64 a = path.get(i);
            Point64 b = path.get((i + 1) % path.size());

            int x1 = (int)Math.round((a.x - cx));
            int y1 = (int)Math.round((a.y - cy));
            int x2 = (int)Math.round((b.x - cx));
            int y2 = (int)Math.round((b.y - cy));

            g2d.drawLine(x1, y1, x2, y2);
        }
    }

    g2d.translate(-cx, -cy);
    g2d.dispose();
    return img;
}



private void fillWithAretes(Graphics2D g2d) {
    if (listAretes == null || listAretes.size() < 2) return;

    class Edge {
        int x0, y0, x1, y1;
        float invPente;
    }

    ArrayList<Edge> edges = new ArrayList<>(listAretes.size());
    int yMin = Integer.MAX_VALUE, yMax = Integer.MIN_VALUE;

    for (Arete a : listAretes) {
        int xA = (int)Math.round(a.First.x  / Main.resolution);
        int yA = (int)Math.round(a.First.y  / Main.resolution);
        int xB = (int)Math.round(a.Second.x / Main.resolution);
        int yB = (int)Math.round(a.Second.y / Main.resolution);

        if (yA == yB) continue;

        Edge e = new Edge();
        if (yA < yB) { e.x0 = xA; e.y0 = yA; e.x1 = xB; e.y1 = yB; }
        else          { e.x0 = xB; e.y0 = yB; e.x1 = xA; e.y1 = yA; }

        e.invPente = (float)(e.x1 - e.x0) / (float)(e.y1 - e.y0);

        yMin = Math.min(yMin, e.y0);
        yMax = Math.max(yMax, e.y1);
        edges.add(e);
    }
    if (edges.isEmpty()) return;
    for (int y = yMin; y < yMax; y++) {
        ArrayList<Integer> xs = new ArrayList<>();

        for (Edge e : edges) {
            if (y >= e.y0 && y < e.y1) {
                float x = e.x0 + (y - e.y0) * e.invPente;
                xs.add(Math.round(x));
            }
        }
        if (xs.size() < 2) continue;
        Collections.sort(xs);

        for (int i = 0; i + 1 < xs.size(); i += 2) {
            g2d.drawLine(xs.get(i), y, xs.get(i + 1), y);
        }
    }
}


}



