// Copyright (c) 2016,2017 Fr�d�ric Claux, Universit� de Limoges. Tous droits r�serv�s.

package fc.Math;

public class Vec4f
{
	public float x;
	public float y;
	public float z;
	public float w;
	
	public Vec4f(float x, float y, float z, float w)
	{
		this.x = x;
		this.y = y;
		this.z = z;
		this.w = w;
	}
	
	public Vec4f(Vec3f v, float w)
	{
		this.x = v.x;
		this.y = v.y;
		this.z = v.z;
		this.w = w;
	}
	
	public Vec4f sub(Vec4f v)
	{
		return new Vec4f(x - v.x, y - v.y, z - v.z, w - v.w);
	}
	

	public Vec4f mul(float s)
	{
		return new Vec4f(x * s, y * s, z * s, w * s);
	}

	public Vec4f mul(Matrix m)
	{
		// Même calcul que Matrix.mul(Vec4f v), mais appelé depuis le vecteur.
		return new Vec4f(
			m.at(0,0) * x + m.at(0,1) * y + m.at(0,2) * z + m.at(0,3) * w,
			m.at(1,0) * x + m.at(1,1) * y + m.at(1,2) * z + m.at(1,3) * w,
			m.at(2,0) * x + m.at(2,1) * y + m.at(2,2) * z + m.at(2,3) * w,
			m.at(3,0) * x + m.at(3,1) * y + m.at(3,2) * z + m.at(3,3) * w
		);
	}

	public Vec3f toVec3f()
	{
		return new Vec3f(x,y,z);
	}
}
