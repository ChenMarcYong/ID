// Copyright (c) 2016,2017 Fr�d�ric Claux, Universit� de Limoges. Tous droits r�serv�s.

package fc.Math;

public class Plane
{
	public Vec3f m_Point;
	public Vec3f m_Normal;
	
	public Plane(Vec3f point, Vec3f normal)
	{
		m_Point = point;
		m_Normal = normal;
    }
	
	public float distanceToPoint(Vec3f p)
	{
		return 0;
    }

	public boolean isAbove(float z)	// méthode pour savoir si un point est au-dessus ou au dessous du plan
	{
		return m_Point.z > z;
	}

	public boolean crossPlane(float z1, float z2)	// détermine s'il y a intersection entre une arête et le plan.
	{
		return (isAbove(z1) != isAbove(z2));
	}

	public Vec2f intersectionPoint(Vec3f v1, Vec3f v2)	// calcule le point d'intersection entre le plan et l'arête
	{
		float t = (m_Point.z - v1.z) / (v2.z - v1.z);  // calcule de proportion t (valeur entre 0 et 1) qui indique la proportion de la position de l'intersection le long de l'arête.
		float xIntersect = v1.x + t * (v2.x - v1.x);
		float yIntersect = v1.y + t * (v2.y - v1.y);

		return new Vec2f(xIntersect, yIntersect);
	}
}
