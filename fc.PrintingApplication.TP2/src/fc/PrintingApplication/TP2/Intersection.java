package fc.PrintingApplication.TP2;

public class Intersection implements Comparable<Intersection> 
{
    public int yScanline;
    public int xIntersect;
    public Arete arete;   

    public Intersection(int y, int x, Arete a)
    {
        yScanline = y;
        xIntersect = x;
        arete = a;
    }

    @Override
    public int compareTo(Intersection other) {
        // Comparer d'abord par yMin (First.y)
        return Integer.compare(this.xIntersect, other.xIntersect);
    }
}
