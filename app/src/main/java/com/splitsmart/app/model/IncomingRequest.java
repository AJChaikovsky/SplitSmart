package com.splitsmart.app.model;

/** Incoming request with sender display info for the Requests tab. */
public class IncomingRequest {
    private final AppRequest request;
    private final String fromName;
    private final String fromEmail;

    public IncomingRequest(AppRequest request, String fromName, String fromEmail) {
        this.request = request;
        this.fromName = fromName;
        this.fromEmail = fromEmail;
    }

    public AppRequest getRequest() { return request; }
    public String getFromName() { return fromName; }
    public String getFromEmail() { return fromEmail; }

    public String headline() {
        if (request.typeEnum() == RequestType.GROUP_INVITE) {
            String group = request.getGroupName().trim().isEmpty() ? "a group" : request.getGroupName();
            return fromName + " invited you to the group \"" + group + "\"";
        }
        return fromName + " wants to add you as a friend";
    }
}
