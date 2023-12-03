package generator;

import java.util.HashMap;
import java.util.ArrayList;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;


public class Merger
{
	private String column;
	private Template template;

	public Node merge(Template template, Element section) throws Exception
	{
		this.template = template;
		HashMap<String,Object> attrs;
		ArrayList<String> columns = new ArrayList<String>();

		for (String name : template.columns)
		{
			Boolean excl = false;
			attrs = template.colattrs.get(name);
			if (attrs != null) excl = (Boolean) attrs.get("excl");
			if (excl == null || !excl) columns.add(name);
		}

		return(merge(section,columns));
	}


	private Node merge(Element section, ArrayList<String> columns) throws Exception
	{
		boolean done = false;

		while (!done)
		{
			done = true;
			Elements elements = section.getAllElements();

			for (int i = 0; i < elements.size(); i++)
			{
				Element elem = elements.get(i);
				done = replace(elem,columns);
				if (!done) break;
			}
		}

		return(section);
	}


	private boolean replace(Element elem, ArrayList<String> columns) throws Exception
	{
		if (elem.tagName().equals(Generator.COLUMN))
		{
			column(elem);
			return(false);
		}

		else

		if (elem.attributes().hasKey(Generator.COLUMNS))
		{
			elem.attributes().remove(Generator.COLUMNS);
			columns(elem,columns);
			return(false);
		}

		else

		if (elem.attributes().hasKey(Generator.GROUPS))
		{
			elem.attributes().remove(Generator.GROUPS);
			groups(elem);
			return(false);
		}

		return(true);
	}


	private void column(Element elem) throws Exception
	{
		if (column == null) throw new Exception("<column> tag not within foreach-column");
		Node field = template.fieldnodes.get(column).clone();
		field.attributes().addAll(elem.attributes());
		this.replace(elem,field);
	}


	private void columns(Element elem, ArrayList<String> columns) throws Exception
	{
		Element merged = null;
		Element template = null;
		HashMap<String,Object> colattrs = null;

		template = elem.clone();
		merged = new Element("div");

		for (String name : columns)
		{
			this.column = name;
			colattrs = this.template.colattrs.get(name);

			Element replace = template.clone();
			this.template.replace(replace,colattrs);
			merged.appendChild(replace);
			this.merge(replace,columns);
			this.column = null;
		}

		Element next = elem;
		ArrayList<Node> children = new ArrayList<Node>(merged.childNodes());

		for (int i = 0; i < children.size(); i++)
		{
			next.after(children.get(i));
			next = (Element) children.get(i);
		}

		delete(elem);
	}


	private void groups(Element elem) throws Exception
	{
		Element next = elem;
		Element template = null;

		ArrayList<ArrayList<String>> groups = group();
		ArrayList<Node> merged = new ArrayList<Node>();

		for (ArrayList<String> columns : groups)
		{
			template = elem.clone();
			merged.add(merge(template,columns));
		}

		for (int i = 0; i < merged.size(); i++)
		{
			next.after(merged.get(i));
			next = (Element) merged.get(i);
		}

		delete(elem);
	}


	private ArrayList<ArrayList<String>> group()
	{
		Integer group = 0;
		Integer cgroup = 0;

		HashMap<String,Object> attrs;
		ArrayList<String> curr = new ArrayList<String>();
		ArrayList<ArrayList<String>> groups = new ArrayList<ArrayList<String>>();

		for (String name : template.columns)
		{
			attrs = template.colattrs.get(name);
			group = nvl((Integer) attrs.get("group"),0);

			if (group == cgroup)
			{
				curr.add(name);
			}
			else
			{
				if (curr.size() > 0) groups.add(curr);
				curr = new ArrayList<String>();
				cgroup = group;
			}
		}

		return(groups);
	}


	private <T> T nvl(T value, T defval)
	{
		if (value != null) return((T) value);
		return(defval);
	}


	private void delete(Node elem)
	{
		elem.remove();
	}


	private void replace(Node elem1, Node elem2)
	{
		elem1.replaceWith(elem2);
	}
}
