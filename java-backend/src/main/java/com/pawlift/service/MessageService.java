package com.pawlift.service;

import com.pawlift.dao.MessageDAO;
import com.pawlift.dao.impl.MessageDAOImpl;
import com.pawlift.exception.UnauthorizedException;
import com.pawlift.model.AdoptionApplication;
import com.pawlift.model.Message;
import com.pawlift.model.User;
import com.pawlift.util.Validator;

import java.util.List;

/**
 * Message threads attached to an application. Only the adopter, the pet's
 * shelter and admins can read or write them (checked by ApplicationService).
 */
public class MessageService {

    private final MessageDAO messages;
    private final ApplicationService applications;

    public MessageService() {
        this(new MessageDAOImpl(), new ApplicationService());
    }

    public MessageService(MessageDAO messages, ApplicationService applications) {
        this.messages = messages;
        this.applications = applications;
    }

    public List<Message> list(User actor, String applicationId) {
        if (actor == null) throw new UnauthorizedException("Please sign in");
        // Throws 404 when the caller may not see this application.
        AdoptionApplication app = applications.getAccessible(actor, applicationId);
        return messages.findByApplication(app.id);
    }

    public Message send(User actor, String applicationId, String body) {
        if (actor == null) throw new UnauthorizedException("Please sign in");
        AdoptionApplication app = applications.getAccessible(actor, applicationId);

        Message m = new Message();
        m.applicationId = app.id;
        m.senderId = actor.id; // never trust a sender id from the browser
        m.body = Validator.required(body, "body", 1000);
        return messages.create(m);
    }
}
