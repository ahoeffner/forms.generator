package generator;

import java.util.List;

import javax.swing.text.html.HTMLDocument;

import org.jsoup.Jsoup;
import java.util.HashMap;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;


public class Template
{
	private String file;
	private Document dom;
	private HashMap<String,Field> fields;
	private HashMap<String,Object> tabattrs;
	private HashMap<String,HashMap<String,Object>> colattrs;

	public Template(String file) throws Exception
	{
		this.load(file);
	}


	public void merge(Table table)
	{
		extractFieldTags();
		extractTableInfo(table);
		extractColumnInfo(table);

		Node[] cols = createFieldNodes();

		Document doc = new Document("");
		Element html = new Element("html");
		Element body = new Element("body");

		doc.appendChild(html);
		html.appendChild(body);

		for (Node col : cols) body.appendChild(col);

		doc.outputSettings().indentAmount(2);
		System.out.println(doc);
	}


	private Node[] createFieldNodes()
	{
		int col = 0;
		HashMap<String,Object> colattrs = null;
		Node[] nodes = new Node[this.colattrs.size()];

		for (String name : this.colattrs.keySet())
		{
			colattrs = this.colattrs.get(name);
			String type = (String) colattrs.get("type");
			Field field = this.fields.get(type);

			if (field == null)
			{
				System.out.println("No definition found for "+name+" of type "+type);
				System.exit(-1);
			}

			nodes[col] = field.node.clone();
			replace(nodes[col++],colattrs);
		}

		return(nodes);
	}


	private void replace(Node node, HashMap<String,Object> colattrs)
	{
		List<Attribute> attrs = node.attributes().asList();

		for (Attribute attr : attrs)
		{
			String name = attr.getKey();
			String value = attr.getValue();

			value = replace(value,colattrs);
			attr.setValue(value);

			if (name.indexOf("$") >= 0)
			{
				String key = replace(name,colattrs);
				node.attributes().remove(name);
				node.attributes().put(key,value);
			}
		}

		List<Node> childs = node.childNodes();

		for (Node child : childs)
		{
			if (child instanceof TextNode)
			{
				String value = ((TextNode) child).text();
				String baseuri = ((TextNode) child).baseUri();

				if (value.trim().length() > 0)
				{
					value = replace(value,colattrs);
					TextNode repl = new TextNode(value,baseuri);
					child.replaceWith(repl);
				}
			}
			else
			{
				replace(child,colattrs);
			}
		}
	}


	private String replace(String value, HashMap<String,Object> colattrs)
	{
		int pos1 = 0;
		int pos2 = 0;

		if (value == null)
			value = "";

		while(pos1 < value.length())
		{
			pos1 = value.indexOf("$",pos1);
			pos2 = value.indexOf("$",pos1+1);

			if (pos1 < 0 || pos2 < 0) break;

			String var = value.substring(pos1+1,pos2);
			Object val = colattrs.get(var.toLowerCase());

			if (val == null)
				val = tabattrs.get(var.toLowerCase());

			if (val != null)
			{
				value = value.substring(0,pos1) + val + value.substring(pos2+1);
				pos1 = pos2 + (val+"").length() - var.length() - 1;
			}
			else
			{
				pos1 = pos2 + 1;
			}
		}

		return(value.trim());
	}


	private void extractTableInfo(Table table)
	{
		JSONObject tabdef = table.definition();
		this.tabattrs = new HashMap<String,Object>();

		String[] entries = JSONObject.getNames(tabdef);
		for (String entry : entries)
		{
			String attr = entry.toLowerCase();
			if (!attr.equals("mapping")) tabattrs.put(attr,tabdef.get(entry));
		}
	}

	private void extractColumnInfo(Table table)
	{
		this.colattrs = new HashMap<String,HashMap<String,Object>>();
		JSONArray columns = table.definition().getJSONArray("mapping");

		for (int i = 0; i < columns.length(); i++)
		{
			JSONObject coldef = columns.getJSONObject(i);
			String[] entries = JSONObject.getNames(coldef);
			HashMap<String,Object> colattrs = new HashMap<String,Object>();
			for (String entry : entries) colattrs.put(entry.toLowerCase(),coldef.get(entry));

			String name = coldef.getString("name").toLowerCase();
			this.colattrs.put(name,colattrs);
		}
	}


	private void extractFieldTags()
	{
		Elements sections = dom.getElementsByTag("columns");

		for (int i = 0; i < sections.size(); i++)
		{
			Elements element = sections.get(i).getElementsByTag("column");

			for (int j = 0; j < element.size(); j++)
			{
				Node def = element.get(j);
				String[] types = def.attributes().get("types").split(", ");

				for (String type : types)
				{
					type = type.trim().toLowerCase();
					if (type.startsWith(",")) type = type.substring(1);
					if (type.endsWith(",")) type = type.substring(0,type.length()-1);
					this.fields.put(type,new Field(def));
				}
			}
		}
	}


	private void load(String file) throws Exception
	{
		this.file = file;

		file = Generator.templates+this.file;
		fields = new HashMap<String,Field>();

		this.dom = Jsoup.parse(Utils.load(file));
	}
}
