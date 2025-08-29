// Copyright (c) 2016,2017 Fr�d�ric Claux, Universit� de Limoges. Tous droits r�serv�s.

package fc.Math;

public class Vec2f
{
	public float x;
	public float y;
	
	public Vec2f(float x, float y)
	{
		this.x = x;
		this.y = y;
	}

	
	
	public Vec2f mul(float d)
	{
		return new Vec2f(x*d, y*d);
	}
	
	public Vec2f add(Vec2f rhs)
	{
		return new Vec2f(x + rhs.x, y + rhs.y);
	}
	
	public Vec2f sub(Vec2f rhs)
	{
		return new Vec2f(x - rhs.x, y - rhs.y);
	}
	
	public float length()
	{
		return (float)Math.sqrt(x*x + y*y);
	}
	
	public float lengthSquared()
	{
		return (x*x + y*y);
	}

	public float distance(Vec2f other)
	{
		float x2 = other.x - x;
		float y2 = other.y - y;

		return (float)Math.sqrt((x2) * (x2) + (y2) * (y2));
	}
	
	public float distanceSquared(Vec2f other)
	{
		float x2 = other.x - x;
		float y2 = other.y - y;

		return (x2) * (x2) + (y2) * (y2);
	}

	public float dot(Vec2f rhs)
	{
		return x*rhs.x + y*rhs.y;
	}
	
	@Override
	public boolean equals(Object o)
	{
		if (!(o instanceof Vec2f))
			return false;
		Vec2f rhs = (Vec2f) o;
		double epsilon = 0.000001f; // Définissez l'epsilon   // 0.000001f pour cuteOcto
		return Math.abs(rhs.x - x) <= epsilon && Math.abs(rhs.y - y) <= epsilon;
	}

	public Vec2f subtract(Vec2f other) {
        return new Vec2f(this.x - other.x, this.y - other.y);
    }
}
