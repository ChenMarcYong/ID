package fc.PrintingApplication.TP2;

import java.util.Objects;

import fc.Math.Vec2f;
import fc.Math.Vec2i;

public class Trianglei 
{
    public Vec2i vertex1;
    public Vec2i vertex2;
    public Vec2i vertex3;

    public Aretei a1;
    public Aretei a2;
    public Aretei a3;

    public Vec2f center;
    public float radius;
    public float area;

    public Vec2i barycentre;

    public Trianglei(Vec2i v1, Vec2i v2, Vec2i v3) {
        vertex1 = v1;
        vertex2 = v2;
        vertex3 = v3;
    
        a1 = new Aretei(v1, v2);
        a2 = new Aretei(v2, v3);
        a3 = new Aretei(v3, v1);
    
        float x1 = vertex1.x, y1 = vertex1.y;
        float x2 = vertex2.x, y2 = vertex2.y;
        float x3 = vertex3.x, y3 = vertex3.y;

        barycentre = new Vec2i((int) ((x1 + x2 + x3) / 3f), (int) ((y1 + y2 + y3) / 3f));
    
        float D = 2 * (x1 * (y2 - y3) + x2 * (y3 - y1) + x3 * (y1 - y2));
        
    
        float Ox = ((x1 * x1 + y1 * y1) * (y2 - y3) + (x2 * x2 + y2 * y2) * (y3 - y1) + (x3 * x3 + y3 * y3) * (y1 - y2)) / D;
        float Oy = ((x1 * x1 + y1 * y1) * (x3 - x2) + (x2 * x2 + y2 * y2) * (x1 - x3) + (x3 * x3 + y3 * y3) * (x2 - x1)) / D;
    
        center = new Vec2f(Ox, Oy);
    
        radius = (float) Math.sqrt((Ox - x1) * (Ox - x1) + (Oy - y1) * (Oy - y1));

    }

    
    public Trianglei()
    {

    }

    public boolean isInTriangle(Vec2i point)
    {
        float c1 = (point.x - vertex1.x) * (vertex2.y - vertex1.y) - (point.y - vertex1.y) * (vertex2.x - vertex1.x);
        float c2 = (point.x - vertex2.x) * (vertex3.y - vertex2.y) - (point.y - vertex2.y) * (vertex3.x - vertex2.x);
        float c3 = (point.x - vertex3.x) * (vertex1.y - vertex3.y) - (point.y - vertex3.y) * (vertex1.x - vertex3.x);
        return (Math.signum(c1) == Math.signum(c2) && Math.signum(c2) == Math.signum(c3)); //|| (c1 == 0 || c2 == 0 || c3 == 0); //
    }

    public boolean hasPoint(Vec2i point) 
    {
        return vertex1.equals(point) || vertex2.equals(point) || vertex3.equals(point);
    }

    public boolean isNeighboor(Trianglei other) {
        int commonEdges = 0;
        if (this.a1.isTheSame(other.a1) || this.a1.isTheSame(other.a2) || this.a1.isTheSame(other.a3)) commonEdges++;
        if (this.a2.isTheSame(other.a1) || this.a2.isTheSame(other.a2) || this.a2.isTheSame(other.a3)) commonEdges++;
        if (this.a3.isTheSame(other.a1) || this.a3.isTheSame(other.a2) || this.a3.isTheSame(other.a3)) commonEdges++;
        return commonEdges == 1;
    }
    
    public boolean containsArete(Aretei other)
    {
        return a1.isTheSame(other) || a2.isTheSame(other) || a3.isTheSame(other);
    }

    @Override
    public boolean equals(Object obj) 
    {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        Trianglei other = (Trianglei) obj;

        // Comparaison stricte des sommets sans tolérance en vérifiant toutes les permutations possibles
        return (vertex1.equals(other.vertex1) && vertex2.equals(other.vertex2) && vertex3.equals(other.vertex3)) ||
               (vertex1.equals(other.vertex2) && vertex2.equals(other.vertex3) && vertex3.equals(other.vertex1)) ||
               (vertex1.equals(other.vertex3) && vertex2.equals(other.vertex1) && vertex3.equals(other.vertex2));
    }

    
    @Override
    public int hashCode() {
        // Génère un hashcode unique en fonction des permutations des points
        int hash1 = Objects.hash(vertex1, vertex2, vertex3);
        int hash2 = Objects.hash(vertex2, vertex3, vertex1);
        int hash3 = Objects.hash(vertex3, vertex1, vertex2);
        return hash1 + hash2 + hash3;
    }

    // public boolean isInCercle(Vec2i p)
    // {
    //     float dist = (float)Math.sqrt((p.x - center.x) * (p.x - center.x) + (p.y - center.y) * (p.y - center.y));
    //     return dist < radius;
    // }

    public boolean isInCercle(Vec2i p) {
        float dx = vertex1.x - center.x;
        float dy = vertex1.y - center.y;
        float dist2 = dx * dx + dy * dy;
        float pointDist2 = (p.x - center.x) * (p.x - center.x) + (p.y - center.y) * (p.y - center.y);
        return pointDist2 < dist2;
    }

    public boolean validateTriangle() 
    {
    if (vertex1.equals(vertex2) || vertex2.equals(vertex3) || vertex3.equals(vertex1)) return false;

    int area = vertex1.x * (vertex2.y - vertex3.y) + vertex2.x * (vertex3.y - vertex1.y) + vertex3.x * (vertex1.y - vertex2.y);
    return area != 0;
    }

    public int isIntersectingEdge(Aretei other)
    {
        if(a1.isIntersecting(other)) return 1;
        if(a2.isIntersecting(other)) return 2;
        if(a3.isIntersecting(other)) return 3;
        return 0;
    }

}
