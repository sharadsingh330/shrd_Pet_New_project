package com.pawlift.dao.impl;

import com.pawlift.dao.BaseDAO;
import com.pawlift.dao.SettingDAO;
import com.pawlift.model.Setting;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class SettingDAOImpl extends BaseDAO implements SettingDAO {

    private static Setting map(ResultSet rs) throws SQLException {
        Setting s = new Setting();
        s.key = rs.getString("setting_key");
        s.value = rs.getString("setting_value");
        s.updatedAt = ts(rs, "updated_at");
        return s;
    }

    @Override
    public List<Setting> findAll() {
        return list("SELECT setting_key, setting_value, updated_at FROM system_settings ORDER BY setting_key",
                SettingDAOImpl::map);
    }

    @Override
    public Optional<String> findValue(String key) {
        return one("SELECT setting_value FROM system_settings WHERE setting_key = ?",
                rs -> rs.getString("setting_value"), key);
    }

    @Override
    public boolean update(String key, String value) {
        return super.update("UPDATE system_settings SET setting_value = ? WHERE setting_key = ?",
                value == null ? "" : value, key) > 0;
    }
}
