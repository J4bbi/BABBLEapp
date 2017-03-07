package babble.dialog.remote;

/** Configuration settings for remote babble communication
 *
 * Used by both Client and Server.
 */
public class RemoteSettings {
	/** The default port to communicate on */
	public static final int DEFAULT_PORT = 34756;
	/** If true, then SSLSockets will be used instead of regular Sockets */
	public static final boolean SECURE = false;
}
