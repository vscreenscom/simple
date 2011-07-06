package org.test;

import java.io.IOException;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class ServerActivity extends Activity {
	private static final int PORT = 10000;
	
	private Server server = null;
	private TextView portText = null;
	
	   @Override
	   public void onCreate(Bundle savedInstanceState) {
	       super.onCreate(savedInstanceState);
	       setContentView(R.layout.main);	       
	       portText = (TextView) findViewById(R.id.port_text);
	   }

	@Override
	protected void onPause() {
		super.onPause();
		
		if (server != null) {
			try {
				server.stop();
			} catch (IOException e) {
				e.printStackTrace();
			}
			server = null;
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		
		server = new Server(PORT);
		try {
			server.start();
		       portText.setText(Integer.toString(PORT));
		} catch (IOException e) {
			e.printStackTrace();
		       portText.setText("<not started>");
		}
	}
}
