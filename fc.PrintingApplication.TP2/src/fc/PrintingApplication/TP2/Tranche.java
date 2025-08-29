package fc.PrintingApplication.TP2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import fc.Math.Vec2f;
import fc.PrintingApplication.clipper2.core.Path64;
import fc.PrintingApplication.clipper2.core.Paths64;
import fc.PrintingApplication.clipper2.core.Point64;
import fc.PrintingApplication.clipper2.offset.ClipperOffset;
import fc.PrintingApplication.clipper2.offset.EndType;
import fc.PrintingApplication.clipper2.offset.JoinType;
import fc.Math.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import fc.PrintingApplication.clipper2.offset.*;

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
        double area2 = 0;
        for (int i=0, j=listPoints.size()-1; i<listPoints.size(); j=i++) {
            Vec2f a = listPoints.get(j), b = listPoints.get(i);
            area2 += (a.x * b.y - b.x * a.y);
        }
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

    // si trop loin -> on coupe et on applique récursivement
    if (dmax2 > eps2) {
        List<Vec2f> left  = DivideAndConquer(pts.subList(0, indexMax + 1), epsilon);
        List<Vec2f> right = DivideAndConquer(pts.subList(indexMax, end + 1), epsilon);

        // fusion sans dupliquer le point pivot
        ArrayList<Vec2f> out = new ArrayList<>(left.size() + right.size() - 1);
        out.addAll(left);
        out.addAll(right.subList(1, right.size()));
        return out;
    } else {
        // sinon on garde seulement les extrémités
        ArrayList<Vec2f> out = new ArrayList<>(2);
        out.add(pts.get(start));
        out.add(pts.get(end));
        return out;
    }
}

}


class Tranche
{
    ArrayList<Arete> listAretes = new ArrayList<>();
    ArrayList<Contour> listContours = new ArrayList<>();
    ArrayList<Path64> listPath = new ArrayList<>();
    
    public Tranche(ArrayList<Arete> l)
    {
        listAretes = l;

        findAllContours2();
		JordanTheorem();

        
        for(int i = 0; i < listContours.size(); i++) listContours.get(i).getUniquePoints();
        for(int i = 0; i < listContours.size(); i++)listContours.get(i).determineOrientation();
        
        
        float epsilon = (float) 2 / 20;
        
        for(Contour contour : listContours)
        {
            contour.listPoints = contour.DivideAndConquer(contour.listPoints, epsilon);

            listAretes = new ArrayList<>();
            if (contour.listPoints != null && contour.listPoints.size() >= 2) 
            {
                for (int i = 0; i < contour.listPoints.size(); i++) {
                    Vec2f a = contour.listPoints.get(i);
                    Vec2f b = contour.listPoints.get((i + 1) % contour.listPoints.size());
                    Arete arete = new Arete();
                    arete.First = a;
                    arete.Second = b;
                    listAretes.add(arete);
                }
            }
        }
        //reverseHole();
        
        

        clip();

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

                    if(lastArete.Second.equals(areteTested.First) || lastArete.First.equals(areteTested.Second))     //ok
                    {
                        Aretes.add(areteTested);
                        ToRemove.add(areteTested);
                        add = true;
                    }

                    else if(lastArete.First.equals(areteTested.First) || lastArete.Second.equals(areteTested.Second))   // reverse
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

public BufferedImage dessinerContoursImage(int width, int height, double pxPerUnit, int cx, int cy, int nb) {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = img.createGraphics();
        // fond blanc
        g2d.setColor(Color.BLACK);
        g2d.fillRect(0, 0, width, height);

        // rendu lisse et épaisseur
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setStroke(new BasicStroke(2f));

        // palette de base (10 couleurs)
        Color[] base = new Color[] {
            Color.RED, Color.GREEN, Color.BLUE, Color.MAGENTA, Color.ORANGE,
            Color.CYAN, Color.PINK, Color.YELLOW, new Color(128, 0, 128), new Color(0, 128, 128)
        };

        // translation du repère
        g2d.translate(cx, cy);
        
        
        

        for(int i = 0; i < listContours.size(); i++)        // listContours.size()
        {
            if(listContours.get(i).type == Type.HOLE) g2d.setColor(Color.RED);
            if(listContours.get(i).type == Type.ISLAND) g2d.setColor(Color.GREEN);

            if (nb > listContours.get(i).listAretes.size()) nb = listContours.get(i).listAretes.size();


            boolean usePoint = false;

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



public BufferedImage dessinerContoursEtOffsetsImage(
        int width, int height,
        double pxPerUnit, int cx, int cy
) {
    BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g2d = img.createGraphics();

    g2d.setColor(Color.BLACK);
    g2d.fillRect(0, 0, width, height);

    // rendu lisse + épaisseur
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


}



