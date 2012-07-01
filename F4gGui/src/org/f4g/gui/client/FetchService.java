package org.f4g.gui.client;


import java.io.IOException;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

/**
 * The client side stub for the RPC service.
 */
@RemoteServiceRelativePath("FetchService")
public interface FetchService extends RemoteService {
	String get(String Url) throws IOException;
	String get(String Url, String contentType) throws IOException;
	String post(String url, String body) throws IOException;
	String post(String url, String body, String contentType) throws IOException;
	String put(String url, String body) throws IOException;
	String put(String url, String body, String contentType) throws IOException;
}
