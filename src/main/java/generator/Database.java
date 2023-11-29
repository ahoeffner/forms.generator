package generator;

import java.net.URI;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.BodyPublisher;


public class Database
{
	private Config config;
	public Column[] columns;


	public Database(Config config)
	{
		this.config = config;
	}


	public void describe(String table) throws Exception
	{
		String data =
		"{\n"+
		"  \"script\": \n" +
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

		URI uri = new URI(config.url+"/exec");
		BodyPublisher body = HttpRequest.BodyPublishers.ofString(data);

		HttpClient client = HttpClient.newHttpClient();
		HttpRequest request = HttpRequest.newBuilder(uri).POST(body).build();

		HttpResponse<String> response = client.send(request,HttpResponse.BodyHandlers.ofString());
		JSONObject json = new JSONObject(new JSONTokener(response.body()));

		if (!json.getBoolean("success"))
		{
			System.out.println(json.toString(2));
			throw new Exception(json.getString("message"));
		}

		JSONArray typs = json.getJSONArray("types");
		JSONArray cols = json.getJSONArray("columns");
		JSONArray prcs = json.getJSONArray("precision");

		String[] columns = new String[cols.length()];
		for (int i = 0; i < cols.length(); i++) columns[i] = cols.getString(i);

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

		JSONArray map = new JSONArray();
		JSONObject def = new JSONObject();

		def.put("from",table);
		def.put("mapping",map);

		for (int i = 0; i < columns.length; i++)
		{
			JSONObject entry = new JSONObject();
			entry.put("name",this.columns[i].name);
			entry.put("size",this.columns[i].size);
			entry.put("type",this.columns[i].jtype(config.mapper));
			map.put(entry);
		}
	}
}
