package generator;
import java.io.FileInputStream;
import java.io.ByteArrayOutputStream;


public class Utils
{
	public static String load(String file) throws Exception
	{
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
}
