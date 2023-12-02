package generator;

import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.BodyPublisher;


public class Table
{
	private String file;
	private Config config;
	private JSONObject def;
	private Column[] columns;


	public static boolean exists(String file) throws Exception
	{
		return(Utils.exists(Generator.tables + file));
	}


	public Table(Config config, String table, String file, boolean update) throws Exception
	{
		this.config = config;
		this.file = Generator.tables + file;

      Utils.delete(this.file);
      System.out.println("deleting file");
      String existing = Utils.load(this.file,true);
		if (existing != null) this.def = new JSONObject(existing);

		if (update)
		{
			this.describe(table);
			this.merge(table);
		}

		IDFactory.create(this.def);
	}


	public Column[] columns()
	{
		return(this.columns);
	}


	public JSONObject definition()
	{
		return(def);
	}


	private void merge(String table) throws Exception
	{
		JSONArray map = null;
		JSONObject def = null;
		HashSet<String> ignore = new HashSet<String>();

		if (this.def == null)
		{
			map = new JSONArray();
			def = new JSONObject();

			def.put("from",table);
			def.put("mapping",map);
			def.put("alias",Column.shortname(table));
		}
		else
		{
			def = this.def;
			map = def.getJSONArray("mapping");

			for (int i = 0; i < map.length(); i++)
				ignore.add(map.getJSONObject(i).getString("name"));
		}

		for (int i = 0; i < columns.length; i++)
		{
			if (!ignore.contains(this.columns[i].name.toLowerCase()))
			{
				JSONObject entry = new JSONObject();
				entry.put("group",0);
				entry.put("pkey",false);
				entry.put("excl",false);
				entry.put("size",this.columns[i].size);
				entry.put("name",this.columns[i].name.toLowerCase());
				entry.put("type",this.columns[i].jtype(config.mapper));
				entry.put("label",initcap(this.columns[i].name));
				entry.put("abbr",this.columns[i].shrt);
				map.put(entry);
			}
		}

		Utils.save(def.toString(2),this.file);
		this.def = def;
	}


	private void describe(String table) throws Exception
	{
		String data =
		"{\n"+
		"	\"disconnect\": false," +
		"  \"batch\": \n" +
		"   [\n" +
		"     {\n" +
		"       \"path\": \"/connect\",\n" +
		"       \"payload\":\n" +
		"        {\n" +
		"           \"auth.method\": \"database\",\n" +
		"           \"username\" : \"" + config.usr+"\",\n" +
		"           \"auth.secret\" : \"" + config.pwd+"\",\n" +
		"           \"scope\": \"dedicated\"\n" +
		"        }\n" +
		"      }\n" +
		"      ,\n" +
		"      {\n" +
		"       \"path\": \"/select\",\n" +
		"       \"payload\":\n" +
		"        {\n" +
		"           \"compact\": \"true\",\n" +
		"           \"describe\" : \"true\",\n" +
		"           \"sql\" : \"select * from "+table+" where 1 = 2\"\n" +
		"        }\n" +
		"      }\n" +
		"   ]\n" +
		"}\n";

		JSONObject json = callORDB("batch",data);

		JSONArray steps = json.getJSONArray("steps");
		JSONObject connect = steps.getJSONObject(0);

		if (!connect.getBoolean("success"))
		{
			System.out.println(json.toString(2));
			throw new Exception(json.getString("message"));
		}

		String dbtype = connect.getString("type");
		String sesid = connect.getString("session");

		json = steps.getJSONObject(1);

		if (!json.getBoolean("success"))
		{
			System.out.println(json.toString(2));
			throw new Exception(json.getString("message"));
		}

		JSONArray typs = json.getJSONArray("types");
		JSONArray cols = json.getJSONArray("columns");
		JSONArray prcs = json.getJSONArray("precision");

		String[] columns = new String[cols.length()];
		for (int i = 0; i < cols.length(); i++) columns[i] = cols.getString(i).toLowerCase();

		String[] types = new String[typs.length()];
		for (int i = 0; i < typs.length(); i++) types[i] = typs.getString(i).toLowerCase();

		int[][] precision = new int[prcs.length()][];
		for (int i = 0; i < prcs.length(); i++)
		{
			JSONArray entry = prcs.getJSONArray(i);

			precision[i] = new int[2];
			precision[i][0] = entry.getInt(0);
			precision[i][1] = entry.getInt(1);
		}

		this.columns = new Column[columns.length];

		for (int i = 0; i < this.columns.length; i++)
			this.columns[i] = new Column(columns[i],types[i],precision[i][0],precision[i][1]);

		getPrimaryKey(dbtype,sesid,table);
		disconnect(sesid);
	}


	private void getPrimaryKey(String type, String session, String table) throws Exception
	{
		String sql = "select :table";

		String data =
		"{\n"+
		"	\"session\": \"" + session + "\"," +
		"	\"sql\": \"" + sql + "\"," +
		"	\"bindvalues\": [{\"name\": \"table\", \"value\": \"" + table + "\", \"type\": \"string\"}]" +
		"}\n";

		JSONObject json = callORDB("select",data);
		System.out.println(json);
	}


	private void disconnect(String session) throws Exception
	{
		String data =
		"{\n"+
		"	\"session\": \"" + session + "\"" +
		"}\n";

		JSONObject json = callORDB("disconnect",data);
		System.out.println(json);
	}


	private JSONObject callORDB(String path, String data) throws Exception
	{
		URI uri = new URI(config.url+"/"+path);
		BodyPublisher body = HttpRequest.BodyPublishers.ofString(data);

		HttpClient client = HttpClient.newHttpClient();
		HttpRequest request = HttpRequest.newBuilder(uri).POST(body).build();

		HttpResponse<String> response = client.send(request,HttpResponse.BodyHandlers.ofString());
		JSONObject json = new JSONObject(new JSONTokener(response.body()));

		return(json);
	}


	private String initcap(String str)
	{
		str = str.trim();

		str = str.substring(0,1).toUpperCase()+
				str.substring(1).toLowerCase();

		int pos = str.indexOf('_');

		while (pos > 0)
		{
			str = str.substring(0,pos)+" "+
					str.substring(pos+1,pos+2).toUpperCase()+
					str.substring(pos+2);

			pos = str.indexOf('_',pos+3);
		}

		return(str);
	}


	public static class IDFactory
	{
		private static String alias;

		private static HashMap<String,Sequence> ids =
			new HashMap<String,Sequence>();


		public static void create(JSONObject def)
		{
			if (def == null) return;

			alias = def.getString("alias");
			JSONArray columns = def.getJSONArray("mapping");

			for (int i = 0; i < columns.length(); i++)
			{
				JSONObject entry = columns.getJSONObject(i);
				String name = entry.getString("name");
				String abbr = entry.getString("abbr");
				ids.put(name,new Sequence(abbr));
			}
		}

		public static String curr(Object name, boolean row)
		{
			Sequence seq = ids.get(name);
			if (seq == null) return("unknown-column-"+name);

			String id = alias+"."+seq.pref+"."+seq.next+".";
			if (row) id += "$row"; else id += "0";

			return(id);
		}

		public static String next(Object name, boolean row)
		{
			Sequence seq = ids.get(name);
			if (seq == null) return("unknown-column-"+name);

			String id = alias+"."+seq.pref+"."+seq.next+".";
			if (row) id += "$row"; else id += "0";

			return(id);
		}

		private static class Sequence
		{
			int next;
			String pref;

			Sequence(String pref)
			{
				this.next = 0;
				this.pref = pref;
			}
		}
	}
}
