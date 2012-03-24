package net.argilo.busfollower;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import net.argilo.busfollower.ocdata.GetNextTripsForStopResult;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.util.Log;

public class OCTranspoDataFetcher {
	private static final String TAG = "OCTranspoDataFetcher";
	
	private final String appID;
	private final String apiKey;
	
	public OCTranspoDataFetcher(String appID, String apiKey) {
		this.appID = appID;
		this.apiKey = apiKey;
	}
	
	public GetNextTripsForStopResult getNextTripsForStop(String routeNumber, String stopNumber) {
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
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
