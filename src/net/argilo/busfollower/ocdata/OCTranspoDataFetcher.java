package net.argilo.busfollower.ocdata;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

public class OCTranspoDataFetcher {
	private final String appID;
	private final String apiKey;
	
	public OCTranspoDataFetcher(String appID, String apiKey) {
		this.appID = appID;
		this.apiKey = apiKey;
	}
	
	public GetNextTripsForStopResult getNextTripsForStop(String routeNumber, String stopNumber) throws IOException {
		try {
			HttpClient client = new DefaultHttpClient();
			HttpPost post = new HttpPost("https://api.octranspo1.com/GetNextTripsForStop");
			
			List<NameValuePair> params = new ArrayList<NameValuePair>(4);
			params.add(new BasicNameValuePair("appID", appID));
			params.add(new BasicNameValuePair("apiKey", apiKey));
			params.add(new BasicNameValuePair("routeNo", routeNumber));
			params.add(new BasicNameValuePair("stopNo", stopNumber));
			post.setEntity(new UrlEncodedFormEntity(params));
			
			HttpResponse response = client.execute(post);
			
			XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
			factory.setNamespaceAware(true);
			XmlPullParser xpp = factory.newPullParser();

			xpp.setInput(response.getEntity().getContent(), "UTF-8");
			xpp.next();
			return new GetNextTripsForStopResult(xpp);
		} catch (IllegalStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (XmlPullParserException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	public GetRouteSummaryForStopResult getRouteSummaryForStop(String stopNumber) throws IOException {
		try {
			HttpClient client = new DefaultHttpClient();
			HttpPost post = new HttpPost("https://api.octranspo1.com/GetRouteSummaryForStop");
			
			List<NameValuePair> params = new ArrayList<NameValuePair>(3);
			params.add(new BasicNameValuePair("appID", appID));
			params.add(new BasicNameValuePair("apiKey", apiKey));
			params.add(new BasicNameValuePair("stopNo", stopNumber));
			post.setEntity(new UrlEncodedFormEntity(params));
			
			HttpResponse response = client.execute(post);
			
			XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
			factory.setNamespaceAware(true);
			XmlPullParser xpp = factory.newPullParser();

			xpp.setInput(response.getEntity().getContent(), "UTF-8");
			xpp.next();
			return new GetRouteSummaryForStopResult(xpp);
		} catch (IllegalStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (XmlPullParserException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
}
