package com.example.myapp.ChatbotFragments;

public interface ChatbotResponseListener {
    void onResponse(String reply);
    void onFailure(String error);
}
