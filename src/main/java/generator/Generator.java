package generator;

import java.net.URL;
import java.io.File;
import java.nio.file.Path;


public class Generator
{
	public static final String COLUMN = "column";
	public static final String COLUMNS = "foreach-column";

	public static final String path = findAppHome();
	public static final String tables = path + File.separator + "tables" + File.separator;
	public static final String templates = path + File.separator + "templates" + File.separator;
	public static final String configfile = path + File.separator + "conf" + File.separator + "config.json";
	public static final String primarykey = path + File.separator + "conf" + File.separator + "primarykey.sql";

	public static final Config config = new Config();
	public static String path(String file) {return(Generator.tables + File.separator + file + File.separator);}



	public static void main(String[] args) throws Exception
	{
		String file = null;
		int len = args.length;
		boolean update = false;

		String program = System.getenv("EXECUTABLE");
		if (program == null) program = "Generator";

		for (int i = 0; i < len; i++)
		{
			if (args[i].equals("-u") || args[i].equals("--update"))
			{
				len--;
				update = true;

				for (int j = i; j < args.length; j++)
					args[j] = j < args.length - 1 ? args[j+1] : null;

				i -= 2;
            continue;
			}

         if (args[i].equals("-f") || args[i].equals("--file"))
			{
            if (args.length > i)
            {
               len -= 2;
               file = args[i+1];

               for (int j = i; j < args.length; j++)
                  args[j] = j < args.length - 2 ? args[j+2] : null;

					i -= 2;
               continue;
            }
			}
		}

		if (len < 1 || len > 2)
		{
			System.out.println();
			System.out.println("Usage: "+program+" [options] table [template]");
			System.out.println();
			System.out.println("options:");
			System.out.println("         -f | --file : override table as filename");
			System.out.println("         -u | --update : update table definition");
			System.out.println();
			System.exit(-1);
		}

		String tab = args[0];
		String tpl = len > 1 ? args[1] : "default";

		if (file == null)
			file = tab;

		if (!tpl.endsWith(".html"))
			tpl += ".html";

      //System.out.println("deleting file");
      //Table.delete(file);

		int pos = file.indexOf(".");
		if (pos > 0) file = file.substring(0,pos);

      if (!Table.exists(file))
         update = true;

		Table table = new Table(config,tab,file,update);

		if (table.definition() == null)
			throw new Exception("No definition found for "+tab);

		Template template = new Template(tpl);
		template.merge(table,file);
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