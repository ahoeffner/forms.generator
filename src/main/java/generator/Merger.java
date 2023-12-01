package generator;

import java.util.HashMap;
import java.util.ArrayList;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;


public class Merger
{
	private Template template;

	public Merger()
	{

	}

	public Node merge(Template template, Element section)
	{
		this.template = template;

		boolean done = false;
		Element node = section.clone();

		while (!done)
		{
			done = true;
			Elements elements = node.getAllElements();

			for (int i = 0; i < elements.size(); i++)
			{
				Element elem = elements.get(i);

				if (elem.attributes().hasKey(Generator.COLUMNS))
				{
					elem.attributes().remove(Generator.COLUMNS);
					columns(elem);
					done = true;
				}

				if (!done)
					break;
			}
		}

		return(node);
	}


	private void columns(Element elem)
	{
		Node merged = null;
		Node template = null;
		HashMap<String,Object> colattrs = null;

		template = elem.clone();
		merged = new Element("div");

		for (String name : this.template.columns)
		{
			colattrs = this.template.colattrs.get(name);

			Node replace = template.clone();
			this.template.replace(replace,colattrs);
			((Element) merged).appendChild(replace);
		}

		if (merged.childNodes().size() == 0)
		{
			replace(elem,merged);
		}
		else
		{
			Element next = elem;
			ArrayList<Node> children = new ArrayList<Node>(merged.childNodes());

			for (int i = 0; i < children.size(); i++)
			{
				next.after(children.get(i));
				next = (Element) children.get(i);
			}

			delete(elem);
		}
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
