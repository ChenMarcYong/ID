// Copyright (c) 2016,2017 Fr�d�ric Claux, Universit� de Limoges. Tous droits r�serv�s.

package fc.Serialization;

public interface ISerializable
{
	void serialize(ISerializer buffer);
	void deserialize(ISerializer buffer);
}
