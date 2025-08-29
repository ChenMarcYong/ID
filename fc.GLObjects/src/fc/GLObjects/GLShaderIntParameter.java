// Copyright (c) 2016,2017 Fr�d�ric Claux, Universit� de Limoges. Tous droits r�serv�s.

package fc.GLObjects;

import org.lwjgl.opengl.GL20;

public class GLShaderIntParameter extends GLShaderParameter
{
	public GLShaderIntParameter(String name)
	{
		super(name);
	}
	
	public void set(int n)
	{
		if (m_Location != -1)
			GL20.glUniform1i(m_Location, n);
	}
}
