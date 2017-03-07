package babble.dialog.remote;

public class ClientDisconnectedException extends Exception {
    private String msg;

    public ClientDisconnectedException(String msg) {
        this.msg = msg;
    }

    public String getMessage() {
        return msg;
    }

    private static final long serialVersionUID = -3115490965259331419L;
}
