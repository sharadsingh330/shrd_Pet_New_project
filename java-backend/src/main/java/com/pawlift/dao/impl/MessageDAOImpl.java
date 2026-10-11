package com.pawlift.dao.impl;

import com.pawlift.dao.BaseDAO;
import com.pawlift.dao.MessageDAO;
import com.pawlift.model.Message;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

public class MessageDAOImpl extends BaseDAO implements MessageDAO {

    private static final String SELECT =
            "SELECT m.id, m.application_id, m.sender_id, u.name AS sender_name, m.body, m.created_at "
                    + "FROM messages m JOIN users u ON u.id = m.sender_id ";

    private static Message map(ResultSet rs) throws SQLException {
        Message m = new Message();
        m.id = rs.getString("id");
        m.applicationId = rs.getString("application_id");
        m.senderId = rs.getString("sender_id");
        m.senderName = rs.getString("sender_name");
        m.body = rs.getString("body");
        m.createdAt = ts(rs, "created_at");
        return m;
    }

    @Override
    public List<Message> findByApplication(String applicationId) {
        return list(SELECT + "WHERE m.application_id = ? ORDER BY m.created_at ASC", MessageDAOImpl::map,
                applicationId);
    }

    @Override
    public Message create(Message message) {
        if (message.id == null || message.id.isBlank()) message.id = UUID.randomUUID().toString();
        update("INSERT INTO messages (id, application_id, sender_id, body) VALUES (?,?,?,?)",
                message.id, message.applicationId, message.senderId, message.body);
        return one(SELECT + "WHERE m.id = ?", MessageDAOImpl::map, message.id).orElse(message);
    }
}
