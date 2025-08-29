package fc.PrintingApplication.TP2;
import java.lang.Comparable;
import fc.Math.Vec2i;

public class Aretei  implements Comparable<Aretei> 
{
    Vec2i First;
    Vec2i Second;

    float pente;

    public Aretei(Vec2i v1, Vec2i v2)
    {

        if (v1.y >= v2.y)
        {
            First = v2;
            Second = v1;
        }

        else 
        {
            First = v1;
            Second = v2;            
        }

        int x1 = First.x;
        int y1 = First.y;

        int x2 = Second.x;
        int y2 = Second.y;
        if( y2 != y1) pente = (float) (x2 - x1) / (y2 - y1);
        else pente = 0;
        
    }

    public Aretei()
    {
        
    }
    @Override
    public int compareTo(Aretei other) {
        // Comparer d'abord par yMin (First.y)
        int comparison = Integer.compare(this.First.y, other.First.y);
        if (comparison != 0) {
            return comparison;
        }
        
        // Si yMin est le même, comparer par yMax (Second.y)
        return Integer.compare(this.Second.y, other.Second.y);
    }


    public boolean isTheSame(Aretei other) {
        return (First.equals(other.First) && Second.equals(other.Second)) || 
               (First.equals(other.Second) && Second.equals(other.First));
    }

    public boolean containsPoint(Vec2i other)
    {
        return (First.equals(other) || Second.equals(other));
    }

    public Boolean isIntersecting(Aretei arete)
    {
        Vec2i v1 = new Vec2i(Second.x - First.x, Second.y - First.y);
        Vec2i v2 = new Vec2i(arete.Second.x - arete.First.x, arete.Second.y - arete.First.y);
        
        float u, v;
        float a = ((arete.Second.x - arete.First.x) * (arete.First.y - First.y) - (arete.Second.y - arete.First.y) * (arete.First.x - First.x));
        float b = ((arete.Second.x - arete.First.x) * (Second.y - First.y) - (arete.Second.y - arete.First.y) * (Second.x - First.x));
        float c = ((Second.x - First.x) * (arete.First.y - First.y) - (Second.y - First.y) * (arete.First.x - First.x));
        
        if (b == 0) return false;

        u = a / b;
        v = c / b;
        return (u > 0 && u < 1 && v > 0 && v < 1);
        // int x = First.x +(int) (u * (Second.x - First.x));
        // int y = First.y +(int) (v * (Second.y - First.y));
        // Vec2i point = new Vec2i(x, y);
        // return point;
            
    }
    public Vec2i getMidPoint()
    {
        int x = (First.x + Second.x) / 2;
        int y = (First.y + Second.y) / 2;
        return new Vec2i(x, y);
    }
    
}
