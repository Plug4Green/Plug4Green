package f4g.f4gGui.gui.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.net.HttpURLConnection;
import java.net.URL;
import f4g.f4gGui.gui.client.FetchService;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;

/**
 * FetchService is a helper service to be able to fetch data.
 * Fetching data directly from the client gives cross-site scripting
 * security issues.
 * 
 * @author Jos de Jong, Vasiliki Georgiadou
 */
public class FetchServiceImpl extends RemoteServiceServlet
		implements FetchService {
	private static final long serialVersionUID = 1L;

	@Override
	public String get(String url) throws IOException {
		return get(url, null);
	}

	@Override
	public String get(String url, String contentType) throws IOException {
		URL serverAddress = new URL(url); 
	    HttpURLConnection conn = 
	    	(HttpURLConnection)serverAddress.openConnection();
	    conn.setConnectTimeout(10000);
	    conn.setReadTimeout(10000);

	    if (contentType != null) {
	    	conn.setRequestProperty("Content-Type", contentType);
	    }

	    InputStream is = conn.getInputStream();
	    String response = streamToString(is);
	    is.close();

	    return response;
	}

	@Override
	public String post(String url, String body) throws IOException {
		return post(url, body, null);
	}

	@Override
	public String post(String url, String body, String contentType) 
			throws IOException {
		URL serverAddress = new URL(url); 
	    HttpURLConnection conn = 
	    	(HttpURLConnection)serverAddress.openConnection();
	    conn.setConnectTimeout(10000);
	    conn.setReadTimeout(10000);

	    if (contentType != null) {
	    	conn.setRequestProperty("Content-Type", contentType);
	    }

	    if (body != null) {
		    conn.setRequestMethod("POST");
		    conn.setDoOutput(true);

		    OutputStream os = conn.getOutputStream();
			os.write(body.getBytes());
			os.flush();
			os.close();
	    }
	    
	    InputStream is = conn.getInputStream();
	    String response = streamToString(is);
	    is.close();

	    return response;
	}

	@Override
	public String put(String url, String body) throws IOException {
		return put(url, body, null);
	}
	@Override
	public String put(String url, String body, 
			String contentType) throws IOException {
		URL serverAddress = new URL(url); 
	    HttpURLConnection conn = 
	    	(HttpURLConnection)serverAddress.openConnection();
	    conn.setConnectTimeout(10000);
	    conn.setReadTimeout(10000);
	    
	    if (contentType != null) {
	    	conn.setRequestProperty("Content-Type", contentType);
	    }
	    	    
	    if (body != null) {
		    conn.setRequestMethod("PUT");
		    conn.setDoOutput(true);

		    OutputStream os = conn.getOutputStream();
			os.write(body.getBytes());
			os.flush();
			os.close();
	    }
	    
	    InputStream is = conn.getInputStream();
	    String response = streamToString(is);
	    is.close();

	    return response;
	}

	private String streamToString(InputStream in) throws IOException {
		StringBuffer out = new StringBuffer();
		byte[] b = new byte[4096];
		for (int n; (n = in.read(b)) != -1;) {
			out.append(new String(b, 0, n));
		}
		return out.toString();
	}
}
