// Copyright (c) 2016,2017 Fr�d�ric Claux, Universit� de Limoges. Tous droits r�serv�s.

package fc.GLObjects;

import org.lwjgl.opengl.GL20;

public class GLShaderFloatParameter extends GLShaderParameter
{
	public GLShaderFloatParameter(String name)
	{
		super(name);
	}
	
	public void set(float f)
	{
		if (m_Location != -1)
			GL20.glUniform1f(m_Location, f);
	}
}
