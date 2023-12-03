package generator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ByteArrayOutputStream;


public class Utils
{
	public static <T> T nvl(T value, T defval)
	{
		if (value != null) return((T) value);
		return(defval);
	}

	public static boolean delete(String file) throws Exception
	{
		return((new File(file)).delete());
	}

	public static boolean exists(String file) throws Exception
	{
		return((new File(file)).exists());
	}

	public static String load(String file, boolean check) throws Exception
	{
		if (check)
		{
			if (!exists(file))
				return(null);
		}

		int read = 0;
		byte[] buf = new byte[4096];
		FileInputStream in = new FileInputStream(file);
		ByteArrayOutputStream out = new ByteArrayOutputStream();

		while(read >= 0)
		{
			out.write(buf,0,read);
			read = in.read(buf);
		}

		in.close();
		return(new String(out.toByteArray()));
	}


	public static void save(String data, String file) throws Exception
	{
		save(data.getBytes("UTF-8"),file);
	}


	public static void save(byte[] data, String file) throws Exception
	{
		File dir = new File(file).getParentFile();
		dir.mkdirs();

		FileOutputStream out = new FileOutputStream(file);
		out.write(data);
		out.close();
	}
}
