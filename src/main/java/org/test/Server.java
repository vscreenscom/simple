package org.test;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.security.KeyStore;
import java.security.cert.Certificate;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.simpleframework.http.Request;
import org.simpleframework.http.Response;
import org.simpleframework.http.Status;
import org.simpleframework.http.core.Container;
import org.simpleframework.transport.connect.Connection;
import org.simpleframework.transport.connect.SocketConnection;

import android.content.Context;
import android.util.Log;

public class Server implements Container {

	private int port;
	private Connection connection = null;
	private Context context;
	private boolean ssl;

	public Server(Context context, int port, boolean ssl) {
		this.context = context;
		this.port = port;
		this.ssl = ssl;
	}

	@Override
	public void handle(Request request, Response response) {

		try {
			String path = request.getPath().getPath();
			Log.d("simple", "path: " + path);

			if (path == null) {
				response.setCode(Status.BAD_REQUEST.getCode());
				response.setText(Status.BAD_REQUEST.getDescription());
				return;
			}

			if (!request.getMethod().toLowerCase().equals("get")) {
				response.setCode(Status.METHOD_NOT_ALLOWED.getCode());
				response.setText(Status.METHOD_NOT_ALLOWED.getDescription());
				return;
			}

			File f = new File(path);
			if (!f.exists()) {
				response.setCode(Status.NOT_FOUND.getCode());
				response.setText(Status.NOT_FOUND.getDescription());
				return;
			}

			OutputStream os = response.getOutputStream();

			// here, we're responding with the bytes of the file
			respond(os, f);

			// in much the same way,
			// we require an interface that can give us the "converted"
			// bytes of the video, something like,
			//
			// InputStream is = getVideoStream(f);
			// respond(os, is);
			//
			// ideally, the interface could accept a stream in place of a file,
			// this is the more usual pattern for us, so ...
			//
			// InputStream is = getVideoStream(someOtherInputStrean);
			// respond(os, is);
		} catch (Throwable t) {
			response.setCode(Status.INTERNAL_SERVER_ERROR.getCode());
			response.setText(Status.INTERNAL_SERVER_ERROR.getDescription());
		} finally {
			try {
				response.close();
			} catch (IOException e) {
			}
		}
	}

	private void respond(OutputStream os, File file) throws IOException {
		InputStream is = null;
		is = new BufferedInputStream(new FileInputStream(file), 16384);
		respond(os, is);
	}

	private void respond(OutputStream os, InputStream is) throws IOException {
		try {
			int size = 0;
			byte[] bytes = new byte[16384];
			int count;
			while ((count = is.read(bytes)) != -1) {
				os.write(bytes, 0, count);
				size += count;
			}
		} finally {
			is.close();
		}
	}

	TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
		public java.security.cert.X509Certificate[] getAcceptedIssuers() {
			return null;
		}

		public void checkClientTrusted(
				java.security.cert.X509Certificate[] certs, String authType) {
		}

		public void checkServerTrusted(
				java.security.cert.X509Certificate[] certs, String authType) {
		}
	} };

	public synchronized void start() throws IOException {
		connection = new SocketConnection(this);
		SocketAddress address = new InetSocketAddress(port);
		if (ssl) {
			connection.connect(address, creatSSLContext());
		} else {
			connection.connect(address);
		}
	}

	public SSLContext creatSSLContext() {
		try {
			SSLContext sslContext = SSLContext.getInstance("TLS");

			// $ keytool -genkeypair -alias simple -keypass simple \
			// -keystore simple.keystore -storepass simple -storetype bks \
			// -provider org.bouncycastle.jce.provider.BouncyCastleProvider \
			// -providerpath ~/Downloads/bcprov-jdk16-146.jar -keyalg rsa

			// initialize the keystore
			char[] password = "simple".toCharArray();
			KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
			InputStream is = context.getAssets().open("simple.keystore");
			ks.load(is, password);

			Certificate ct = ks.getCertificate("simple");
			Log.i("simple", "self-signed certificate type: " + ct.getType());

			// setup the key manager factory
			KeyManagerFactory kmf = KeyManagerFactory
					.getInstance(KeyManagerFactory.getDefaultAlgorithm());
			kmf.init(ks, password);

			// setup the trust manager factory
			TrustManagerFactory tmf = TrustManagerFactory
					.getInstance(TrustManagerFactory.getDefaultAlgorithm());
			tmf.init(ks);

			// setup the HTTPS context and parameters
			sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
			return sslContext;
		} catch (Exception e) {
			throw new RuntimeException("could not create SSL context", e);
		}
	}

	public synchronized void stop() throws IOException {
		if (connection != null) {
			connection.close();
		}
	}

	public synchronized int getPort() {
		return this.port;
	}
}
