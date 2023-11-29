package generator;

import java.util.HashMap;
import java.util.List;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;


public class Template
{
	public String file;
	public HashMap<String,Field> columns;


	public Template(String file)
	{
		columns = new HashMap<String,Field>();
		this.file = Generator.templates+file+".html";
	}

	public void load() throws Exception
	{
		String templ = Utils.load(file);
		Document dom = Jsoup.parse(templ);

		Elements types = dom.getElementsByTag("types");

		for (int i = 0; i < types.size(); i++)
		{
			List<Node> element = types.get(i).childNodesCopy();

			for (int j = 0; j < element.size(); j++)
			{
				Node def = element.get(j);
				String type = def.attributes().get("name");

				if (!def.nodeName().equals("#text"))
					this.columns.put(type,new Field(def));
			}
		}
	}
}
