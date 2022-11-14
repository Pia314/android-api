package com.example.apicalltest;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class APIStructures {

    public class StringOut {

        @SerializedName("out")
        private String out;


        public StringOut(String message) {
            this.out = message;
        }

        public String get() {
            return out;
        }
    }

    public class ListOut {

        @SerializedName("out")
        private List<String> out;


        public ListOut(List<String> message) {
            this.out = message;
        }

        public List<String> get() {
            return out;
        }
    }

    public class MessageOut {
        @SerializedName("out")
        private Message out;

        public MessageOut(Message message) {
            this.out = message;
        }

        public Message get() {
            return out;
        }
    }


    public class Message {

        @SerializedName("messageString")
        private String messageString;


        public Message(String message) {
            this.messageString = message;
        }

        public String get() {
            return messageString;
        }
    }

}