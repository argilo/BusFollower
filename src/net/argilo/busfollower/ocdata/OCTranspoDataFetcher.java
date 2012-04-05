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
import android.database.sqlite.SQLiteDatabase;

public class OCTranspoDataFetcher {
	public static GetNextTripsForStopResult getNextTripsForStop(Context context, SQLiteDatabase db, String stopNumber, String routeNumber)
			throws IOException, XmlPullParserException, IllegalArgumentException {
		validateStopNumber(context, stopNumber);
		validateRouteNumber(context, routeNumber);
		
		HttpClient client = new DefaultHttpClient();
		HttpPost post = new HttpPost("https://api.octranspo1.com/v1.1/GetNextTripsForStop");
		
		List<NameValuePair> params = new ArrayList<NameValuePair>(4);
		params.add(new BasicNameValuePair("appID", context.getString(R.string.oc_transpo_application_id)));
		params.add(new BasicNameValuePair("apiKey", context.getString(R.string.oc_transpo_application_key)));
		params.add(new BasicNameValuePair("routeNo", routeNumber));
		params.add(new BasicNameValuePair("stopNo", stopNumber));
		post.setEntity(new UrlEncodedFormEntity(params));
		
		HttpResponse response = client.execute(post);
		
		XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
		factory.setNamespaceAware(true);
		XmlPullParser xpp = factory.newPullParser();

		xpp.setInput(response.getEntity().getContent(), "UTF-8");
		xpp.next(); // <soap:Envelope>
		xpp.next(); //   <soap:Body>
		xpp.next(); //     <GetRouteSummaryForStopResponse>
		xpp.next(); //       <GetRouteSummaryForStopResult>
		return new GetNextTripsForStopResult(context, db, xpp, stopNumber);
	}
	
	public static GetRouteSummaryForStopResult getRouteSummaryForStop(Context context, String stopNumber) throws IOException, XmlPullParserException {
		validateStopNumber(context, stopNumber);

		HttpClient client = new DefaultHttpClient();
		HttpPost post = new HttpPost("https://api.octranspo1.com/v1.1/GetRouteSummaryForStop");
		
		List<NameValuePair> params = new ArrayList<NameValuePair>(3);
		params.add(new BasicNameValuePair("appID", context.getString(R.string.oc_transpo_application_id)));
		params.add(new BasicNameValuePair("apiKey", context.getString(R.string.oc_transpo_application_key)));
		params.add(new BasicNameValuePair("stopNo", stopNumber));
		post.setEntity(new UrlEncodedFormEntity(params));
		
		HttpResponse response = client.execute(post);
		
		XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
		factory.setNamespaceAware(true);
		XmlPullParser xpp = factory.newPullParser();

		xpp.setInput(response.getEntity().getContent(), "UTF-8");
		xpp.next(); // <soap:Envelope>
		xpp.next(); //   <soap:Body>
		xpp.next(); //     <GetRouteSummaryForStopResponse>
		xpp.next(); //       <GetRouteSummaryForStopResult>
		return new GetRouteSummaryForStopResult(xpp);
	}
	
	private static void validateStopNumber(Context context, String stopNumber) {
		if (stopNumber.length() < 3 || stopNumber.length() > 4) {
			throw new IllegalArgumentException(context.getString(R.string.invalid_stop_number));
		}
		for (int i = 0; i < stopNumber.length(); i++) {
			if (!Character.isDigit(stopNumber.charAt(i))) {
				throw new IllegalArgumentException(context.getString(R.string.invalid_stop_number));
			}
		}
	}
	
	private static void validateRouteNumber(Context context, String routeNumber) {
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
