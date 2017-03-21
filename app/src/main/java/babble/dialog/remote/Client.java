package babble.dialog.remote;

import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;

/** Client to communicate with {@link babble.dialog.remote.Server}
 *
 * This is designed to not have any dependencies on Babble, so the Client, ClientResponse and RemoteSettings clients can
 *  be copied into any projects that require communicating with a Server.
 *
 * This class exposes a main method which allows the user to send messages using stdin, and also a simple send/recieve
 *  interface if you instantiate it.
 *
 * It can use either regular sockets or SSL sockets based on the SECURE property in RemoteSettings.
 */
public class Client {
	private BufferedReader reader;
	private BufferedWriter writer;

	/** When run as a standalone Java execution, it will attempt to initiate a console connection with a server
	 *
	 * Command syntax is `java Client hostname [port]`
	 *
	 * It will automatically try to connect to the given hostname using the given port (defaults to
	 *  RemoteSettings.DEFAULT_PORT), and will send and  messages using stdin/stdout.
	 */
	public static void main(String[] args) {
		Scanner stdin = new Scanner(System.in);

		try {
			Client c = null;

			switch(args.length) {
				case 2:
					c = new Client(args[0], Integer.parseInt(args[1]));
					break;
				case 1:
					c = new Client(args[0]);
					break;
				default:
					System.err.println("Usage: Client hostname [port]");
					System.exit(2);
			}

			while(true) {
				try {
					if(System.in.available() > 0) {
						String in = stdin.nextLine();
						c.sendUtterance(in);
					}
				} catch (IOException e) {
					// TODO If stdin is broken, we have bigger problems
				}
				ClientResponse r = c.getResponse();
				if(r != null) {
					System.out.println(r);
				}
			}
		} catch(ClientDisconnectedException e) {
			System.err.println("Failed to connect Client: " + e.getMessage());
			System.exit(1);
		}
	}

	public Client(String host) throws ClientDisconnectedException {
		this(host, RemoteSettings.DEFAULT_PORT);
	}

	/** Creating an instance of Client will automatically create a connection
	 *
	 * From here, use sendUtterance and getResponse to communicate with the remote agent.
	 */
	public Client(String host, int port) throws ClientDisconnectedException {
		try {
			SocketFactory factory;
			if(RemoteSettings.SECURE) {
				factory = SSLSocketFactory.getDefault();
			}else{
				factory = SocketFactory.getDefault();
			}
			Socket sock = factory.createSocket(host, port);

			reader = new BufferedReader(new InputStreamReader(sock.getInputStream()));
			writer = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream()));
		} catch (UnknownHostException e) {
			System.err.print("Unknown host: ");
			System.err.println(host);
			throw new ClientDisconnectedException("Unknown host: "+host);
		} catch (IOException e) {
			System.err.print("Failed to set up socket: ");
			System.err.println(e.getMessage());
		}
	}

	/** Send a string utterance to the server
	 *
	 * This is a string, consisting of any number of words.
	 */
	public void sendUtterance(String utterance) throws ClientDisconnectedException {
		try {
			writer.write(utterance);
			writer.newLine();
			writer.flush();
		} catch (IOException e) {
			throw new ClientDisconnectedException("Failed to send utterance: " + e.getMessage());
		} catch (NullPointerException e)	{
			throw new ClientDisconnectedException("Failed to send utterance (NullPointer): " + e.getMessage());
		}
	}

	/** Call this method to get a response from the server
	 *
	 * @TODO Figure out/decide on when this should be called.
	 */
	public ClientResponse getResponse() throws ClientDisconnectedException {
		try {
			if(reader.ready()) {
				return new ClientResponse(reader.readLine());
			}else{
				//Log.v("BABBLE-app", "Response failed");
				return null;
			}
		} catch (IOException e) {
			throw new ClientDisconnectedException("Failed to get response: " + e.getMessage());
		} catch (NullPointerException e)	{
			throw new ClientDisconnectedException("Failed to get response: " + e.getMessage());
		}

	}
}
