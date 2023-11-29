package generator;

import org.jsoup.nodes.Node;


public class Field
{
	public Node node;

	public Field(Node node)
	{
		this.node = node;
		System.out.println(node);
	}

	public Node merge(Column column)
	{
		Node node = this.node.clone();

		return(node);
	}
}
