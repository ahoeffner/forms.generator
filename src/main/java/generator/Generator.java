package generator;

import java.net.URL;
import java.io.File;
import java.nio.file.Path;


public class Generator
{
	public static final String path = findAppHome();
	public static final String output = path + File.separator + "html" + File.separator;
	public static final String tables = path + File.separator + "tables" + File.separator;
	public static final String templates = path + File.separator + "templates" + File.separator;
	public static final Config config = new Config(path + File.separator + "conf"  + File.separator + "config.json");

	public static void main(String[] args) throws Exception
	{
		int len = args.length;
		boolean update = true;

		for (int i = 0; i < len; i++)
		{
			if (args[i].equals("-du") || args[i].equals("--disable-update"))
			{
				len--;
				update = false;

				for (int j = i; j < args.length; j++)
					args[j] = j < args.length - 1 ? args[j+1] : null;
			}
		}

		if (len < 1 || len > 2)
		{
			System.out.println();
			System.out.println("Usage: generate [options] table [template]");
			System.out.println();
			System.out.println("options:");
			System.out.println("         -du | --disable-update : do not update table definition");
			System.out.println();
			System.exit(-1);
		}

		String tab = args[0];
		String tpl = len > 1 ? args[1] : "default";

		if (!tpl.endsWith(".html"))
			tpl += ".html";

		Table table = new Table(config,tab,update);

		if (table.definition() == null)
			throw new Exception("No definition found for "+tab);

		Template template = new Template(tpl);
		template.merge(table);
	}


	private static String findAppHome()
	{
		String sep = File.separator;
		Object obj = new Object() { };

		String cname = obj.getClass().getEnclosingClass().getName();
		cname = "/" + cname.replace('.','/') + ".class";

		URL url = obj.getClass().getResource(cname);
		String path = url.getPath();

		if (url.getProtocol().equals("jar") || url.getProtocol().equals("code-source"))
		{
			path = path.substring(5); // get rid of "file:"
			path = path.substring(0,path.indexOf("!")); // get rid of "!class"
			path = path.substring(0,path.lastIndexOf("/")); // get rid jarname

			if (path.endsWith("/target")) path = path.substring(0,path.length()-7);
			if (path.endsWith("/project")) path = path.substring(0,path.length()-8);
		}
		else
		{
			path = path.substring(0,path.length()-cname.length());
			if (path.endsWith("/classes")) path = path.substring(0,path.length()-8);
			if (path.endsWith("/target")) path = path.substring(0,path.length()-7);
		}

		String escape = "\\";
		if (sep.equals(escape))
		{
			// Windows
			if (path.startsWith("/") && path.charAt(2) == ':')
			path = path.substring(1);

			path = path.replaceAll("/",escape+sep);
		}

		File cw = new File(".");
		Path abs = java.nio.file.Paths.get(path);
		Path base = java.nio.file.Paths.get(cw.getAbsolutePath());
		path = base.relativize(abs).toString();

		// Back until conf folder

		while(true)
		{
			String conf = path+sep+"conf";

			File test = new File(conf);
			if (test.exists()) break;

			int pos = path.lastIndexOf(sep);

			if (pos < 0)
			{
				path = base.toString();
				path = path.substring(0,path.length()-2);
				break;
			}

			path = path.substring(0,pos);
		}

		if (path.startsWith("."))
		{
			path = cw.getAbsolutePath() + sep + path;
			abs = java.nio.file.Paths.get(path).normalize();
			path = abs.toString();
		}

		return(path);
	}
}