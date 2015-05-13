package com.baike.util;
import java.io.IOException;
import java.net.SocketException;
import java.util.Map;

import org.apache.http.HttpHost;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
import org.apache.http.util.EntityUtils;

/**
 * http客户端用于发送请求
 *
 */
public class WikiHttpClient {
	public static HttpHost proxy = null;
	/**
	 * 发送get请求
	 * @param uri 	请求路径
	 * @return 放回请求API得到的结果
	 */
	public static String get(String uri){
		String content = "";
		CloseableHttpClient client = null;
		RequestBuilder reBuilder = RequestBuilder.get();
		if(client == null){
			if(proxy == null)
				client = HttpClients.custom().build();
			else{
				DefaultProxyRoutePlanner routePlanner= new DefaultProxyRoutePlanner(proxy);
				client = HttpClients.custom().setRoutePlanner(routePlanner).build();
			}
		}
		reBuilder.setUri(uri);
		reBuilder.addHeader("User-Agent", 
				"Mozilla/5.0 (Windows NT 6.3; WOW64) "+
                "AppleWebKit/537.36 (KHTML, like Gecko) "+
                "Chrome/37.0.2062.124 Safari/537.36");
		HttpUriRequest request = reBuilder.build();
		CloseableHttpResponse response = null;
		try {
			response = client.execute(request);
			if(response.getStatusLine().getStatusCode() == 403)
			{
				System.out.println(uri + "is forbidden.");
				return "403";
			}
			content = EntityUtils.toString(response.getEntity(),"utf-8");
			response.close();
			request = null;
			response = null;
			reBuilder = null;
			client.close();
		} catch(SocketException e){
			System.out.println(uri + "is forbidden.");
			return "403";
		} catch (ClientProtocolException e) {

			System.out.println("#"+uri);
			return "invalid";
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		client = null;
		return content;
	}

	/**
	 * 发送post请求
	 * @param uri 	请求路径
	 * @return 放回请求API得到的结果
	 */
	public static String post(String uri, Map<String,String> params){
		String content = "";
		
		CloseableHttpClient client = null;
		if(proxy == null)
			client = HttpClients.custom().build();
		else{
			DefaultProxyRoutePlanner routePlanner= new DefaultProxyRoutePlanner(proxy);
			client = HttpClients.custom().setRoutePlanner(routePlanner).build();
		}
		RequestBuilder reBuilder = RequestBuilder.post();
		reBuilder.setUri(uri);
		for(String key: params.keySet()){
			reBuilder.addParameter(key, params.get(key));
		}
		HttpUriRequest request = reBuilder.build();
		CloseableHttpResponse response = null;
		try {
			response = client.execute(request);
			content = EntityUtils.toString(response.getEntity());
			response.close();
			client.close();
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return content;
	}
}
