package com.zjutjh.qaq

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MessageAdapter(private var messageList: List<QMessage>) :
    RecyclerView.Adapter<MessageAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view: View = when (viewType) {
            QMessage.TYPE_LEFT -> {
                LayoutInflater.from(parent.context).inflate(R.layout.msg_box_left, parent, false)
            }
            QMessage.TYPE_RIGHT -> {
                LayoutInflater.from(parent.context).inflate(R.layout.msg_box_right, parent, false)
            }
            else -> {
                LayoutInflater.from(parent.context).inflate(R.layout.msg_blank, parent, false)
            }
        }
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        //绑定函数
        val msg = messageList[position]
        if (msg.type != QMessage.TYPE_BLANK) {
            holder.msgBox!!.text = msg.content
            holder.name!!.text = msg.user
            holder.time!!.text = msg.date
        }
    }

    override fun getItemCount(): Int {
        return messageList.size
    }

    override fun getItemViewType(position: Int): Int {
        val msg = messageList[position]
        return msg.type
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var msgBox: TextView?
        var name: TextView?
        var time: TextView?

        init {
            msgBox = itemView.findViewById(R.id.msg_box)
            name = itemView.findViewById(R.id.text_name)
            time = itemView.findViewById(R.id.text_time)
        }
    }
}