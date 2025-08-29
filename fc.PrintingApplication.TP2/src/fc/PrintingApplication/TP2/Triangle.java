package fc.PrintingApplication.TP2;

import java.util.Objects;

import fc.Math.Vec2f;
import fc.Math.Vec3f;

public class Triangle 
{
    public Vec2f vertex1;
    public Vec2f vertex2;
    public Vec2f vertex3;

    public Arete a1;
    public Arete a2;
    public Arete a3;

    public Triangle(Vec2f v1, Vec2f v2, Vec2f v3)
    {
        vertex1 = v1;
        vertex2 = v2;
        vertex3 = v3;

        a1 = new Arete(v1, v2);
        a2 = new Arete(v2, v3);
        a3 = new Arete(v3, v1);
    }

    public Triangle()
    {

    }

    public boolean isInTriangle(Vec2f point)
    {
        float c1 = (point.x - vertex1.x) * (vertex2.y - vertex1.y) - (point.y - vertex1.y) * (vertex2.x - vertex1.x);
        float c2 = (point.x - vertex2.x) * (vertex3.y - vertex2.y) - (point.y - vertex2.y) * (vertex3.x - vertex2.x);
        float c3 = (point.x - vertex3.x) * (vertex1.y - vertex3.y) - (point.y - vertex3.y) * (vertex1.x - vertex3.x);
        return (Math.signum(c1) == Math.signum(c2) && Math.signum(c2) == Math.signum(c3)) || (c1 == 0 || c2 == 0 || c3 == 0); //
    }

    public boolean hasPoint(Vec2f point)
    {
        return(vertex1 == point || vertex2 == point || vertex3 == point);
    }

   /*  public boolean isNeighboor(Triangle other) {
        int compteur = 0;
    
        if (vertex1.equals(other.vertex1) || vertex1.equals(other.vertex2) || vertex1.equals(other.vertex3)) compteur++;
        if (vertex2.equals(other.vertex1) || vertex2.equals(other.vertex2) || vertex2.equals(other.vertex3)) compteur++;
        if (vertex3.equals(other.vertex1) || vertex3.equals(other.vertex2) || vertex3.equals(other.vertex3)) compteur++;
        return compteur == 2;
    }*/

    public boolean isNeighboor(Triangle other) {
        int commonEdges = 0;
        if (this.a1.isTheSame(other.a1) || this.a1.isTheSame(other.a2) || this.a1.isTheSame(other.a3)) commonEdges++;
        if (this.a2.isTheSame(other.a1) || this.a2.isTheSame(other.a2) || this.a2.isTheSame(other.a3)) commonEdges++;
        if (this.a3.isTheSame(other.a1) || this.a3.isTheSame(other.a2) || this.a3.isTheSame(other.a3)) commonEdges++;
        return commonEdges == 1;
    }

    public boolean containsArete(Arete other)
    {
        return a1.isTheSame(other) || a2.isTheSame(other) || a3.isTheSame(other);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        Triangle other = (Triangle) obj;

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
}
