package com.pawlift.dao;

import com.pawlift.model.Message;

import java.util.List;

public interface MessageDAO {
    List<Message> findByApplication(String applicationId);

    Message create(Message message);
}
