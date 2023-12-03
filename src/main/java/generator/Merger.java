package generator;

import java.util.List;
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

		this.template.tempattrs.put("group","$group$");
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
			Element merged = columns(elem,columns);
			replace(elem,merged);
			return(false);
		}

		else

		if (elem.attributes().hasKey(Generator.GROUPS))
		{
			elem.attributes().remove(Generator.GROUPS);
			ArrayList<Element> merged = groups(elem);

			for (int i = 0; i < merged.size(); i++)
				elem.after(merged.get(i));

			delete(elem);
			return(false);
		}

		else

		{
			this.template.replace(elem,null);
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


	private Element columns(Element elem, ArrayList<String> columns) throws Exception
	{
		Element merged = null;
		Element template = null;
		HashMap<String,Object> colattrs = null;

		merged = copy(elem);
		template = elem.clone();

		List<Node> children = merged.childNodes();

		for (int i = 0; i < children.size(); i++)
			children.get(i).remove();

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

		return(merged);
	}


	private ArrayList<Element> groups(Element elem) throws Exception
	{
		Element template = null;
		ArrayList<ArrayList<String>> groups = group();
		ArrayList<Element> merged = new ArrayList<Element>();

		for (ArrayList<String> columns : groups)
		{
			template = elem.clone();
			Element group = copy(elem);

			this.template.replace(template,null);
			Element replace = columns(template,columns);

			for (int i = 0; i < replace.children().size(); i++)
			{
				Element entry = replace.children().get(i);
				Elements entries = entry.children().clone();

				for (int j = 0; j < entries.size(); j++)
					group.appendChild(entries.get(j));
			}

			merged.add(group);
		}

		return(merged);
	}


	private ArrayList<ArrayList<String>> group()
	{
		Integer group = 0;
		Integer cgroup = 0;
		Boolean excl = false;

		HashMap<String,Object> attrs;
		ArrayList<String> curr = new ArrayList<String>();
		ArrayList<ArrayList<String>> groups = new ArrayList<ArrayList<String>>();

		for (String name : template.columns)
		{
			attrs = template.colattrs.get(name);
			group = Utils.nvl((Integer) attrs.get("group"),0);
			excl = Utils.nvl((Boolean) attrs.get("excl"),false);

			if (group == cgroup)
			{
				//if (!excl)
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


	private void delete(Node elem)
	{
		elem.remove();
	}


	private void replace(Node elem1, Node elem2)
	{
		elem1.replaceWith(elem2);
	}


	private Element copy(Element elem)
	{
		Element copy = new Element(elem.tagName());
		copy.attributes().addAll(elem.attributes());
		return(copy);
	}


	@SuppressWarnings("unchecked")
	private <T> T getAttributeValue(String name, String attr)
	{
		HashMap<String,Object> attrs = template.colattrs.get(name.toLowerCase());
		Object value = attrs.get(attr.toLowerCase());
		return((T) value);
	}
}
