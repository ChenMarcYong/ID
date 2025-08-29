// Copyright (c) 2016,2017 Fr�d�ric Claux, Universit� de Limoges. Tous droits r�serv�s.

package fc.Math;

import java.util.Objects;

public class Vec2i
{
	public int x;
	public int y;
	
	public Vec2i(int x, int y)
	{
		this.x = x;
		this.y = y;
	}
	


	public Vec2i mul(int d)
	{
		return new Vec2i(x*d, y*d);
	}
	
	public Vec2i add(Vec2i rhs)
	{
		return new Vec2i(x + rhs.x, y + rhs.y);
	}
	
	public Vec2i sub(Vec2i rhs)
	{
		return new Vec2i(x - rhs.x, y - rhs.y);
	}
	
	public float length()
	{
		return (float)Math.sqrt(x*x + y*y);
	}
	
	public int dot(Vec2i rhs)
	{
		return x*rhs.x + y*rhs.y;
	}
	



	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null || getClass() != obj.getClass()) return false;
		Vec2i vec2i = (Vec2i) obj;
		int epsilon = 0;
		return Math.abs(x - vec2i.x) <= epsilon && Math.abs(y - vec2i.y) <= epsilon;
	}
    @Override
    public int hashCode() {
        return Objects.hash(x, y); // Utilise une fonction de hachage standard de la classe Objects
    }
}
