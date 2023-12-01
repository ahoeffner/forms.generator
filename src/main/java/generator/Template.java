package generator;

import java.util.List;
import org.jsoup.Jsoup;
import java.util.HashMap;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Attribute;
import org.jsoup.select.Elements;


public class Template
{
	private String file;
	private Document dom;
	private ArrayList<Element> sections;
	private HashMap<String,Field> fields;
	private HashMap<String,Object> tabattrs;
	private HashMap<String,Node> fieldnodes;
	private HashMap<String,HashMap<String,Object>> colattrs;

	public Template(String file) throws Exception
	{
		this.load(file);

	 	this.sections 		= new ArrayList<Element>();
		this.fields 		= new HashMap<String,Field>();
		this.tabattrs 		= new HashMap<String,Object>();
		this.fieldnodes 	= new HashMap<String,Node>();
		this.colattrs 		= new HashMap<String,HashMap<String,Object>>();
	}


	public void merge(Table table)
	{
		extractFieldTags();
		extractTemplates();
		extractTableInfo(table);
		extractColumnInfo(table);

		createFieldNodes();

		for (Element section : sections)
			mergeTemplates(section);

		Document doc = new Document("");
		Element html = new Element("html");
		Element body = new Element("body");

		doc.appendChild(html);
		html.appendChild(body);

		for (Node col : fieldnodes.values())
			body.appendChild(col);

		doc.outputSettings().indentAmount(2);
		//System.out.println(doc);
	}


	private void createFieldNodes()
	{
		HashMap<String,Object> colattrs = null;

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

			Node node = field.node.clone();
			fieldnodes.put(name,node);

			replace(node,colattrs);
		}
	}


	private void replace(Node node, HashMap<String,Object> colattrs)
	{
		List<Attribute> attrs = node.attributes().asList();

		for (Attribute attr : attrs)
		{
			String name = attr.getKey();
			String value = attr.getValue();


			if (isVariable(name))
			{
				node.attributes().remove(name);
				value = replace(name,colattrs);

				// var existed
				if (!value.equals(name))
				{
					name = name.replaceAll("\\$","");
					node.attributes().put(name,value);
				}
			}
			else
			{
				value = replace(value,colattrs);
				attr.setValue(value);
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

		String[] entries = JSONObject.getNames(tabdef);
		for (String entry : entries)
		{
			String attr = entry.toLowerCase();
			if (!attr.equals("mapping")) tabattrs.put(attr,tabdef.get(entry));
		}
	}

	private void extractColumnInfo(Table table)
	{
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


	private void extractTemplates()
	{
		Elements elements = dom.body().children();

		for (int i = 0; i < elements.size(); i++)
		{
			Element elem = elements.get(i);

			if (!elem.tagName().equals("columns"))
				sections.add(elem);
		}
	}


	private void mergeTemplates(Element elem)
	{
		if (elem.tagName().equals("elem"));

	}


	private boolean isVariable(String var)
	{
		var = var.trim();

		if (var.startsWith("$") && var.endsWith("$"))
		{
			for (int i = 1; i < var.length()-1; i++)
				if (var.charAt(i) == '$') return(false);

			return(true);
		}

		return(false);
	}


	private void load(String file) throws Exception
	{
		this.file = file;
		file = Generator.templates+this.file;
		this.dom = Jsoup.parse(Utils.load(file,false));
	}
}
