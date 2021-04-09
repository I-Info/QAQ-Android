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
    List<QMessage> messageList;

    public MessageAdapter(List<QMessage> messageList) {
        this.messageList = messageList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = null;
        if (viewType == QMessage.TYPE_LEFT) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.msg_box_left, parent, false);
        } else if (viewType == QMessage.TYPE_RIGHT) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.msg_box_right, parent, false);
        }
        assert view != null;
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        //绑定函数
        QMessage msg = messageList.get(position);
        String msgString = msg.getUser() +
                " @ " +
                msg.getDate() +
                " say: \n" +
                msg.getContent();
        holder.msgBox.setText(msgString);
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    @Override
    public int getItemViewType(int position) {
        QMessage msg = messageList.get(position);
        return msg.getType();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView msgBox;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            msgBox = itemView.findViewById(R.id.msg_box);
        }
    }
}
