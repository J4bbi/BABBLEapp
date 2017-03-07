package babble.dialog.remote;

/** A response from a Client instance
 *
 * @TODO Beef out this class and put in everything that needs to be in it.
 */
public class ClientResponse {
	private String word;
	private String speaker;
	
	public ClientResponse(String result) {
		String frags[] = result.split(":");
		this.word = frags[1].trim();
		this.speaker = frags[0].trim();
	}
	
	public String getWord() {
		return word;
	}
	
	public String getSpeaker() {
		return speaker;
	}
	
	public String toString() {
		return "[Response \"" + speaker + ": " + word + "\"]";
	}
}
