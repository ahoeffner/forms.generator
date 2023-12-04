package generator;

import java.util.HashMap;


public class Column
{
	public String 	name;
	public String 	type;
	public String 	shrt;
	public boolean pkey;
	public int 	   size;
	public int 	   scale;

	public Column(String name, String type, boolean pkey, int size, int scale)
	{
		this.name = name;
		this.type = type;
		this.pkey = pkey;
		this.size = size;
		this.scale = scale;
		this.shrt = shortname(name);
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

	public static String shortname(String name)
	{
		String shrt = null;

		if (name.indexOf('_') > 0)
		{
			shrt = "";

			for (String word : name.split("_"))
			{
				shrt += word.substring(0,1);
				if (word.equals("id")) shrt += word.substring(1,2);
			}
		}
		else
		{
			shrt = name.substring(0,3);
		}

		return(shrt);
	}

	public String toString()
	{
		return(name+" "+type+"["+size+","+scale+"]");
	}
}
