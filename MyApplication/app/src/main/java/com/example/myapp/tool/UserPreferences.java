package com.example.myapp.tool;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.UUID;

public class UserPreferences {
    private static final String PREFS_NAME = "user_prefs";
    private static final String KEY_USER_KEY = "USER_KEY";
    private SharedPreferences sharedPreferences;

    public UserPreferences(Context context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    // 保存 userKey
    public void saveUserKey(String userKey) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_USER_KEY, userKey);
        editor.apply();
    }

    // 获取 userKey
    public String getUserKey() {
        return sharedPreferences.getString(KEY_USER_KEY, null);
    }

    // 生成并保存新的 userKey（如果尚未存在）
    public String generateAndSaveUserKey() {
        String userKey = getUserKey();
        if (userKey == null) {
            userKey = UUID.randomUUID().toString();
            saveUserKey(userKey);
        }
        return userKey;
    }

    // 清除 userKey（例如用户退出登录时）
    public void clearUserKey() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(KEY_USER_KEY);
        editor.apply();
    }
}
