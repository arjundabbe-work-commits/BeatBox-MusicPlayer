package com.beatbox.player.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Ignore;
import androidx.annotation.NonNull;

@Entity(tableName = "settings")
class SettingEntity {
    @NonNull @PrimaryKey public String settingKey;
    public String value;
    SettingEntity() { }
    @Ignore SettingEntity(String key, String value) { this.settingKey = key; this.value = value; }
}

@Dao
public abstract class SettingsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) protected abstract void put(SettingEntity setting);
    public final void set(String key,String value){put(new SettingEntity(key,value));}
    @Query("SELECT value FROM settings WHERE settingKey = :key LIMIT 1") public abstract String get(String key);
    @Query("DELETE FROM settings WHERE settingKey = :key") public abstract void remove(String key);
}
