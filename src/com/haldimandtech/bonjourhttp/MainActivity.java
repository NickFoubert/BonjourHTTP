package com.haldimandtech.bonjourhttp;

import java.io.IOException;
import java.util.ArrayList;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceListener;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class MainActivity extends Activity {

	ArrayList<Service> services;
	ArrayAdapter<Service> data_adapter;
	JmDNS serviceFinder;
	WifiManager.MulticastLock lock;
	
	// Create a message handling object as an anonymous class.
	OnItemClickListener mMessageClickedHandler = new OnItemClickListener() {
		public void onItemClick(AdapterView parent, View v, int position, long id) {
			Service chosenService = services.get(position);
			goToURL(chosenService.url);
		}
	};
	
	private Service fromEvent(ServiceEvent ev) {
		return new Service(ev.getInfo().getName(), ev.getInfo().getURL());
	}
	
	ServiceListener mDnsServiceListener = new ServiceListener() {
		public void serviceResolved(ServiceEvent ev) {
			System.out.println("serviceResolved");
			Service found = fromEvent(ev);
			if (!services.contains((Object) found)) {
				services.add(found);
				MainActivity.this.runOnUiThread(new Runnable() { public void run() { data_adapter.notifyDataSetChanged(); }});
			}
		}

		public void serviceRemoved(ServiceEvent ev) {
			Service removed = fromEvent(ev);
			services.remove((Object) removed);
		}

		public void serviceAdded(ServiceEvent event) {
			// Required to force serviceResolved to be called again
			// (after the first search)
			serviceFinder.requestServiceInfo(event.getType(), event.getName(), 1);
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_PROGRESS);
		setContentView(R.layout.activity_main);
		services = new ArrayList<Service>();
		
		data_adapter = new ArrayAdapter<Service>(this,
				android.R.layout.simple_list_item_1, services);

		ListView listView = (ListView) findViewById(R.id.listView);
		listView.setAdapter(data_adapter);
		listView.setOnItemClickListener(mMessageClickedHandler);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	@Override
	protected void onResume() {
		super.onResume();
		WifiManager wifi = (WifiManager) getSystemService(android.content.Context.WIFI_SERVICE);
		lock = wifi.createMulticastLock("HeeereDnssdLock");
	    lock.setReferenceCounted(true);
	    lock.acquire();
		new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void... arg0) {
				try {
					System.out.println("Starting serviceFinder");
					serviceFinder = JmDNS.create();
					System.out.println("Adding service listener");
					serviceFinder.addServiceListener("_http._tcp.local.", mDnsServiceListener);
				} catch (IOException e) {
					e.printStackTrace();
					System.out.println("oops");
				}
				return null;
			}
	    }.execute();
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		if (lock != null) lock.release();
		
		new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void... arg0) {
				try {
					System.out.println("Stopping serviceFinder");
					serviceFinder.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				return null;
			}
	    }.execute();
	}
	
	public void goToUri(Uri uri) {
		Intent intent = new Intent(Intent.ACTION_VIEW, uri);
		startActivity(intent);
	}

	public void goToURL(String url) {
		Uri uri = Uri.parse(url);
		goToUri(uri);
	}

}
