package com.example.defaultsmsapp;

public class SmsModel {
    private long id;
    private String address;
    private String body;
    private long date;
    private int type;
    private boolean isRead;
    private String contactName;
    
    // SMS types constants
    public static final int TYPE_INBOX = 1;
    public static final int TYPE_SENT = 2;
    public static final int TYPE_DRAFT = 3;
    public static final int TYPE_OUTBOX = 4;
    public static final int TYPE_FAILED = 5;
    public static final int TYPE_QUEUED = 6;

    public SmsModel() {
    }

    public SmsModel(long id, String address, String body, long date, int type, boolean isRead, String contactName) {
        this.id = id;
        this.address = address;
        this.body = body;
        this.date = date;
        this.type = type;
        this.isRead = isRead;
        this.contactName = contactName;
    }

    // Getters
    public long getId() {
        return id;
    }

    public String getAddress() {
        return address;
    }

    public String getBody() {
        return body;
    }

    public long getDate() {
        return date;
    }

    public int getType() {
        return type;
    }

    public boolean isRead() {
        return isRead;
    }

    public String getContactName() {
        return contactName;
    }

    // Setters
    public void setId(long id) {
        this.id = id;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public void setDate(long date) {
        this.date = date;
    }

    public void setType(int type) {
        this.type = type;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
    }

    // Helper methods
    public boolean isIncoming() {
        return type == TYPE_INBOX;
    }

    public boolean isOutgoing() {
        return type == TYPE_SENT || type == TYPE_OUTBOX || type == TYPE_QUEUED;
    }

    public String getDisplayName() {
        return contactName != null && !contactName.equals(address) ? contactName : address;
    }

    public String getTypeString() {
        switch (type) {
            case TYPE_INBOX:
                return "Received";
            case TYPE_SENT:
                return "Sent";
            case TYPE_DRAFT:
                return "Draft";
            case TYPE_OUTBOX:
                return "Outbox";
            case TYPE_FAILED:
                return "Failed";
            case TYPE_QUEUED:
                return "Queued";
            default:
                return "Unknown";
        }
    }

    @Override
    public String toString() {
        return "SmsModel{" +
                "id=" + id +
                ", address='" + address + '\'' +
                ", body='" + body + '\'' +
                ", date=" + date +
                ", type=" + type +
                ", isRead=" + isRead +
                ", contactName='" + contactName + '\'' +
                '}';
    }
}