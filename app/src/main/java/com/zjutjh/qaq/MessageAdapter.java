package com.zjutjh.qaq;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.ViewHolder> {
    List<Message> messageList;

    public MessageAdapter(List<Message> messageList) {
        this.messageList = messageList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = null;
        if (viewType == Message.TYPE_LEFT) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.msg_box_left, parent, false);
        } else if (viewType == Message.TYPE_RIGHT) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.msg_box_right, parent, false);
        }
        assert view != null;
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Message msg = messageList.get(position);
        holder.msgBox.setText(msg.getContent());
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    @Override
    public int getItemViewType(int position) {
        Message msg = messageList.get(position);
        return msg.getType();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        LinearLayout layout;
        TextView msgBox;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            layout = itemView.findViewById(R.id.layout);
            msgBox = itemView.findViewById(R.id.msg_box);
        }
    }
}
