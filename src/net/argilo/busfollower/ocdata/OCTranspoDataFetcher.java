package net.argilo.busfollower.ocdata;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.argilo.busfollower.R;

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

import android.content.Context;

public class OCTranspoDataFetcher {
	private final Context context;
	private final String appID;
	private final String apiKey;
	
	public OCTranspoDataFetcher(Context context) {
		this.context = context;
		appID = context.getString(R.string.oc_transpo_application_id);
        apiKey = context.getString(R.string.oc_transpo_application_key);
	}
	
	public GetNextTripsForStopResult getNextTripsForStop(String stopNumber, String routeNumber) throws IOException, XmlPullParserException {
		validateStopNumber(stopNumber);
		validateRouteNumber(routeNumber);
		
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
	}
	
	public GetRouteSummaryForStopResult getRouteSummaryForStop(String stopNumber) throws IOException, XmlPullParserException {
		validateStopNumber(stopNumber);

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
	}
	
	private void validateStopNumber(String stopNumber) {
		if (stopNumber.length() < 3 || stopNumber.length() > 4) {
			throw new IllegalArgumentException(context.getString(R.string.invalid_stop_number));
		}
		for (int i = 0; i < stopNumber.length(); i++) {
			if (!Character.isDigit(stopNumber.charAt(i))) {
				throw new IllegalArgumentException(context.getString(R.string.invalid_stop_number));
			}
		}
	}
	
	private void validateRouteNumber(String routeNumber) {
		if (routeNumber.length() < 1 || routeNumber.length() > 3) {
			throw new IllegalArgumentException(context.getString(R.string.invalid_route_number));
		}
		for (int i = 0; i < routeNumber.length(); i++) {
			if (!Character.isDigit(routeNumber.charAt(i))) {
				throw new IllegalArgumentException(context.getString(R.string.invalid_route_number));
			}
		}
	}
}
