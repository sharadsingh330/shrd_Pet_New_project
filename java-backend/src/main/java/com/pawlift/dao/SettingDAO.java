package com.pawlift.dao;

import com.pawlift.model.Setting;

import java.util.List;
import java.util.Optional;

public interface SettingDAO {
    List<Setting> findAll();

    Optional<String> findValue(String key);

    boolean update(String key, String value);
}
