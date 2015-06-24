package org.jivesoftware.openfire.nio;

/**
 * @author Konstantin Yakimov <a href="mailto:kiakimov@gmail.com>kiakimov@gmail.com</a>
 */
public class DeliverAttemptToClosedConnection extends RuntimeException {

    public DeliverAttemptToClosedConnection() {
    }

    public DeliverAttemptToClosedConnection(String message) {
        super(message);
    }

    public DeliverAttemptToClosedConnection(String message, Throwable cause) {
        super(message, cause);
    }

    public DeliverAttemptToClosedConnection(Throwable cause) {
        super(cause);
    }
}
