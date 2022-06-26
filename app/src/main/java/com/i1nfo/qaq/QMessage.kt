package com.i1nfo.qaq

class QMessage(val user: String?, val date: String?, val content: String?, val type: Int) {
    override fun toString(): String {
        return "QMessage{" +
                "msgType=" + type +
                ", user='" + user + '\'' +
                ", date='" + date + '\'' +
                ", content='" + content + '\'' +
                '}'
    }

    companion object {
        const val TYPE_LEFT = 0x0
        const val TYPE_RIGHT = 0x1
        const val TYPE_BLANK = 0x10
    }
}