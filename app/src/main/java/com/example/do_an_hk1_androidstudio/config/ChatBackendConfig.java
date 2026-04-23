package com.example.do_an_hk1_androidstudio.config;

import android.text.TextUtils;

public final class ChatBackendConfig {

    private static final String PLACEHOLDER_URL = "https://YOUR_BACKEND_DOMAIN/api/chat";
    private static final String EMULATOR_LOCAL_BACKEND_URL = "http://10.0.2.2:3000/api/chat";

    /*
     * For Android Emulator local testing, 10.0.2.2 points to your computer.
     * Put your deployed backend URL here after deploying cfplus-backend.
     * Example: https://cfplus-chat.onrender.com/api/chat
     *
     * Do not put an OpenAI API key in the Android app. The key belongs in
     * cfplus-backend/.env on your server.
     */
    public static final String CHAT_API_URL = EMULATOR_LOCAL_BACKEND_URL;

    private ChatBackendConfig() {
    }

    public static boolean isConfigured() {
        return !TextUtils.isEmpty(CHAT_API_URL) && !PLACEHOLDER_URL.equals(CHAT_API_URL);
    }
}
