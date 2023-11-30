package generator;

import org.jsoup.nodes.Node;


public class Field
{
	public Node node;


	public Field(Node node)
	{
		this.node = node;

		for (Node child : node.childNodes())
		{
			if (!child.nodeName().equals("#text"))
			{
				this.node = child;
				break;
			}
		}
	}
}
