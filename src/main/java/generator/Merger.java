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

	private static final String grignr = "$group$";

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

		this.template.tempattrs.put("group",grignr);
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
			Element merged = groups(elem);
			replace(elem,merged);
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

		template = elem.clone();
		merged = this.template.copy(elem);

		List<Node> children = merged.childNodes();

		for (int i = 0; i < children.size(); i++)
			children.get(i).remove();

		for (String name : columns)
		{
			this.column = name;
			colattrs = this.template.colattrs.get(name);
			this.template.tempattrs.put("group",colattrs.get("group"));

			Element replace = template.clone();
			this.template.replace(replace,colattrs);
			merged.appendChild(replace);
			this.merge(replace,columns);
			this.column = null;

			this.template.tempattrs.put("group",grignr);
		}

		return(merged);
	}

	private Element groups(Element elem) throws Exception
	{
		Elements children = elem.children();
		Element merged = template.copy(elem);
		ArrayList<ArrayList<String>> groups = getGroups();
		ArrayList<Element> nodes = new ArrayList<Element>();

		for (ArrayList<String> columns : groups)
		{
			Integer grid = getAttributeValue(columns.get(0),"group");
			this.template.tempattrs.put("group",grid);

			Element group = template.copy(elem);
			this.template.replace(group,null);

			for (int i = 0; i < children.size(); i++)
				traverse(children.get(i).clone(),group,columns);

			this.template.tempattrs.put("group",grignr);
			nodes.add(group);
			merged.appendChild(group);
		}

		return(merged);
	}


	private ArrayList<Element> traverse(Element elem, Element merged, ArrayList<String> columns) throws Exception
	{
		ArrayList<Element> groups;

		if (elem.tagName().equals("group"))
		{
			groups = group(elem,columns);
			this.template.replace(elem,null);

			if (groups.size() > 0)
			{
				for (int g = 0; g < groups.size(); g++)
				{
					Element group = groups.get(g);
					merged.appendChild(group);
				}
			}
		}
		else
		{
			Elements children = elem.children();

			elem = template.copy(elem);
			this.template.replace(elem,null);

			merged.appendChild(elem);


			for (int i = 0; i < children.size(); i++)
			{
				Element child = children.get(i).clone();
				this.template.replace(child,null);
				merged.appendChild(elem);
				groups = traverse(child,elem,columns);
			}

			groups = new ArrayList<Element>();
		}

		return(groups);
	}

	private ArrayList<Element> group(Element elem, ArrayList<String> columns) throws Exception
	{
		Element template = null;

		template = elem.clone();
		ArrayList<Element> group = new ArrayList<Element>();

		Element replace = columns(template,columns);

		for (int i = 0; i < replace.children().size(); i++)
		{
			Element entry = replace.children().get(i);
			Elements entries = entry.children().clone();

			for (int j = 0; j < entries.size(); j++)
				group.add(entries.get(j));
		}

		return(group);
	}


	private ArrayList<ArrayList<String>> getGroups()
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
				if (!excl)
					curr.add(name);
			}
			else
			{
				if (curr.size() > 0) groups.add(curr);
				curr = new ArrayList<String>();
				if (!excl) curr.add(name);
				cgroup = group;
			}
		}

		if (curr.size() > 0)
			groups.add(curr);

		return(groups);
	}


	private void replace(Node elem1, Node elem2)
	{
		elem1.replaceWith(elem2);
	}

	@SuppressWarnings("unchecked")
	private <T> T getAttributeValue(String name, String attr)
	{
		HashMap<String,Object> attrs = template.colattrs.get(name.toLowerCase());
		Object value = attrs.get(attr.toLowerCase());
		return((T) value);
	}
}
