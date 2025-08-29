// Copyright (c) 2016,2017 Fr�d�ric Claux, Universit� de Limoges. Tous droits r�serv�s.

package fc.GLObjects;

import org.lwjgl.opengl.GL20;

public class GLShaderParameter
{
	protected GLProgram m_Shader;
	protected String m_Name;
	protected int m_Location;
	
	public GLShaderParameter(String name)
	{
		m_Name = name;
	}
	
	public void init(GLProgram shader)
	{
		m_Shader = shader;
		m_Location = GL20.glGetUniformLocation(shader.m_ProgramId, m_Name);
	}
}
