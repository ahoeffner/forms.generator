package generator;

import java.util.HashMap;
import org.json.JSONObject;
import org.json.JSONTokener;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.FileInputStream;


public class Config
{
	public final String usr;
	public final String pwd;
	public final String url;
	public final HashMap<String,String> mapper;
	public final HashMap<String,String> primarykey;

	public Config()
	{
		JSONObject config = null;

		this.mapper = new HashMap<String,String>();
		this.primarykey = new HashMap<String,String>();

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
			FileInputStream in = new FileInputStream(Generator.configfile);
			config = new JSONObject(new JSONTokener(in));
			in.close();
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

		loadPrimaryKeySQL();
	}


	public String getPrimaryKeySQL(String type)
	{
		return(primarykey.get(type.toLowerCase()));
	}


	private void loadPrimaryKeySQL()
	{
		String content = null;

		try
		{
			byte[] data = new byte[4094];
			FileInputStream in = new FileInputStream(Generator.primarykey);

			int read = in.read(data);
			content = new String(data,0,read);

			in.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(-1);
		}

		getSections(content);
	}


	private void getSections(String content)
	{
		Pattern pattern = Pattern.compile("(.*?)\\[(.*?)\\]");
		Matcher matcher = pattern.matcher(content.replaceAll("\n"," ").trim());

		while(matcher.find())
		{
			String section = content.substring(matcher.start(),matcher.end()-1);

			int pos = section.indexOf("[");
			section = section.replaceAll("\n"," ");

			String type = section.substring(0,pos).trim();
			String stmt = section.substring(pos+1).trim();

			primarykey.put(type.toLowerCase(),stmt);
		}
	}
}
