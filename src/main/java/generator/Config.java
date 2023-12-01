package generator;

import java.util.HashMap;
import org.json.JSONObject;
import org.json.JSONTokener;
import java.io.FileInputStream;


public class Config
{
	public final String usr;
	public final String pwd;
	public final String url;
	public final HashMap<String,String> mapper;

	public Config(String conf)
	{
		JSONObject config = null;
		this.mapper = new HashMap<String,String>();

		this.mapper.put("string","string");
		this.mapper.put("varchar","string");
		this.mapper.put("varchar2","string");

		this.mapper.put("date","date");
		this.mapper.put("datetime","date");

		this.mapper.put("int","integer");
		this.mapper.put("short","integer");
		this.mapper.put("integer","integer");
		this.mapper.put("smallint","integer");

		this.mapper.put("float","decimal");
		this.mapper.put("double","decimal");

		this.mapper.put("number","number*");
		this.mapper.put("numeric","number*");

		try
		{
			FileInputStream in = new FileInputStream(conf);
			config  = new JSONObject(new JSONTokener(in));
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(-1);
		}

		this.usr = config.getString("usr");
		this.pwd = config.getString("pwd");
		this.url = config.getString("url");

		JSONObject map = config.getJSONObject("mapper");

		String[] types = JSONObject.getNames(map);

		for (int i = 0; i < types.length; i++)
			this.mapper.put(types[i],map.getString(types[i]));
	}
}
