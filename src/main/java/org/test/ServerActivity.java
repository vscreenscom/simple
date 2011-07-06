package org.test;

import java.io.IOException;

import android.app.Activity;
import android.os.Bundle;

public class ServerActivity extends Activity {
	private Server server = null;
	
	   @Override
	   public void onCreate(Bundle savedInstanceState) {
	       super.onCreate(savedInstanceState);
	       setContentView(R.layout.main);
	   }

	@Override
	protected void onPause() {
		super.onPause();
		
		if (server != null) {
			try {
				server.stop();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			server = null;
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		
		server = new Server(10000);
		try {
			server.start();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
