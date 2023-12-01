package generator;

import org.jsoup.nodes.Node;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;


public class Merger
{
	public Merger()
	{

	}

	public Node merge(Template template, Element section)
	{
		Element node = section.clone();
		Elements elements = node.getAllElements();

		for (int i = 0; i < elements.size(); i++)
		{
			Element elem = elements.get(i);
			System.out.println(elem.tagName());
		}

		return(node);
	}
}
