package com.example.myapp.ChatbotFragments;

import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.myapp.R;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ViewHolder> {
    private final List<Message> messages;

    public ChatAdapter(List<Message> messages) {
        this.messages = messages;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView userMessage, botMessage;
        // 新增一个容器，用于自定义消息视图
        FrameLayout customContainer;

        public ViewHolder(View itemView) {
            super(itemView);
            userMessage = itemView.findViewById(R.id.user_message);
            botMessage = itemView.findViewById(R.id.bot_message);
            customContainer = itemView.findViewById(R.id.custom_container);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // 假设 item_message.xml 已经添加了一个 FrameLayout，其id为 custom_container
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_message, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Message message = messages.get(position);
        // 如果是自定义 View 消息，则隐藏文本视图并添加自定义 View到 customContainer
        if (message.isCustomView()) {
            holder.userMessage.setVisibility(View.GONE);
            holder.botMessage.setVisibility(View.GONE);
            holder.customContainer.setVisibility(View.VISIBLE);
            holder.customContainer.removeAllViews();
            holder.customContainer.addView(message.getCustomView());
        } else {
            holder.customContainer.setVisibility(View.GONE);
            // 按照原有逻辑显示文本消息
            if (message.isUser()) {
                holder.userMessage.setVisibility(View.VISIBLE);
                holder.botMessage.setVisibility(View.GONE);
                holder.userMessage.setText(message.getText());
            } else {
                holder.botMessage.setVisibility(View.VISIBLE);
                holder.userMessage.setVisibility(View.GONE);
                holder.botMessage.setText(message.getText());
            }
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }



}
