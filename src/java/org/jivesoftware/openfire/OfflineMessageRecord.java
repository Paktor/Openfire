package org.jivesoftware.openfire;

import org.xmpp.packet.JID;
import org.xmpp.packet.Message;
import org.xmpp.packet.Message.Type;

import java.util.Date;

/**
 * Source message copy prepared to offline storage
 *
 * @author Konstantin Yakimov <a href="mailto:kiakimov@gmail.com>kiakimov@gmail.com</a>
 */
public class OfflineMessageRecord {
    private JID from;
    private String username;
    private Type type;
    private String body;
    private Date date;
    private String stanza;
    private String messageId;
    private Long storeId;

    public OfflineMessageRecord(Message message) {
        this.from = message.getFrom();
        this.username = message.getTo().getNode();
        this.type = message.getType();
        this.body = message.getBody();
        this.stanza = message.getElement().asXML();
        this.messageId = message.getID();
        this.date = new Date();
    }

    public OfflineMessageRecord(Long storeId, OfflineMessageRecord source) {
        this.from = source.getFrom();
        this.username = source.getUsername();
        this.type = source.getType();
        this.body = source.getBody();
        this.stanza = source.getStanza();
        this.messageId = source.getMessageId();
        this.date = source.getDate();

        this.storeId = storeId;
    }

    public JID getFrom() {
        return from;
    }

    public String getUsername() {
        return username;
    }

    public Type getType() {
        return type;
    }

    public String getBody() {
        return body;
    }

    public Date getDate() {
        return date;
    }

    public String getStanza() {
        return stanza;
    }

    public Long getStoreId() {
        return storeId;
    }

    public String getMessageId() {
        return messageId;
    }

    @Override
    public String toString() {
        return "OfflineStoredMessage{" +
                "from='" + from.toString() + '\'' +
                ", username='" + username + '\'' +
                ", type=" + type +
                ", body='" + body + '\'' +
                ", date=" + date +
                ", storeId=" + storeId +
                ", messageId=" + messageId +
                '}';
    }
}
