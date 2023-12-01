package generator;

import java.util.HashMap;


public class Column
{
	public final String 	name;
	public final String 	type;
	public final int 		size;
	public final int 		scale;

	public Column(String name, String type, int size, int scale)
	{
		this.name = name;
		this.type = type;
		this.size = size;
		this.scale = scale;
	}

	public String jtype(HashMap<String,String> map)
	{
		String type = map.get(this.type);

		if (type == null)
		{
			System.out.println("No mapping for datatype '"+this.type+"'. Add mapping in config");
			type = "string";
		}

		if (type.equals("number*"))
		{
			type = "integer";
			if (scale > 0) type = "decimal";
		}

		return(type);
	}

	public String toString()
	{
		return(name+" "+type+"["+size+","+scale+"]");
	}
}
