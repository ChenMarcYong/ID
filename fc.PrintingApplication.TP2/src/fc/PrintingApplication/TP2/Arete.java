package fc.PrintingApplication.TP2;

import fc.Math.Vec2f;
import fc.Math.Vec2i;

public class Arete implements Comparable<Arete>  
{
    Vec2f First;
    Vec2f Second;

    public Arete(Vec2f v1, Vec2f v2)
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
    }


    public boolean Intersect(Vec2f point)
    {
        
        float x = point.x;
        float y = point.y;


        float ymax = Math.max(First.y, Second.y);
        float ymin = Math.min(First.y, Second.y);




        if((x <= First.x || x <= Second.x) && (y <= ymax && y >= ymin))
        {
            return true;
        } 
        


        return false;
    }

    public Arete()
    {
        
    }

    // Méthode compareTo pour définir l'ordre de tri
    @Override
    public int compareTo(Arete other) {
        // Comparer d'abord par yMin (First.y)
        if (this.First.y != other.First.y) {
            return Float.compare(this.First.y, other.First.y);
        }
        // Si yMin est le même, comparer par yMax (Second.y)
        return Float.compare(this.Second.y, other.Second.y);
    }

    public boolean isTheSame(Arete other)
    {
        //return (First.x == other.First.x || First.x == other.Second.x) && (First.y == other.First.y || First.y == other.Second.y) && (Second.x == other.First.x || Second.x == other.Second.x) && (Second.y == other.First.y || Second.y == other.Second.y);
        
        //lcoord.add(new Arete(new Vec2f((int) (arete.First.x * 20), (int) (arete.First.y * 20)), new Vec2f((int) (arete.Second.x * 20), (int) (arete.Second.y * 20))));
        

        Vec2i[] t = new Vec2i[2];
        Vec2i[] t2 = new Vec2i[2];
        t[0] = new Vec2i((int) (First.x * 20), (int) (First.y * 20));
        t[1] = new Vec2i((int) (Second.x * 20), (int) (Second.y * 20));

        t2[0] = new Vec2i((int) (other.First.x * 20), (int) (other.First.y * 20));
        t2[1] = new Vec2i((int) (other.Second.x * 20), (int) (other.Second.y * 20));

        // return (t[0] == t2[0] || t[0] == t2[1]) && (t[1] == t2[0] || t[1] == t2[1]);
        /*System.out.println(t[0].x + " " + t[0].y);
        System.out.println(t[1].x + " " + t[1].y);
        System.out.println(t2[0].x + " " + t2[0].y);
        System.out.println(t2[1].x + " " + t2[1].y);*/
        return ((t[0].x == t2[1].x || t[0].x == t2[0].x) && (t[0].y == t2[1].y || t[0].y == t2[0].y) && (t[1].x == t2[1].x || t[1].x == t2[0].x) && (t[1].y == t2[1].y || t[1].y == t2[0].y));
        //return((First == other.First || First == other.Second) &&  (Second == other.First || Second ==  other.Second));
    }

    public boolean containsPoint(Vec2f other)
    {
        
        
        return (First == other  || Second == other);
    }


    
    
}
