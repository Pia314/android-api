package com.example.apicalltest;

import java.util.HashMap;

public class ActionGestures {
    public enum MyGesture{
        PICK,
        DROP
    }

    public static HashMap<String, MyGesture> gestureHashMap = new HashMap<String, MyGesture>(){{
        put("PICK", MyGesture.PICK);
        put("DROP", MyGesture.DROP);
    }};

    public static class MyAction{
        private String username;
        private MyGesture gesture;

        public MyAction(String u, MyGesture g){
            this.username = u;
            this.gesture = g;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public MyGesture getGesture() {
            return gesture;
        }

        public void setGesture(MyGesture gesture) {
            this.gesture = gesture;
        }
    }

    public static MyAction retrieveAction(String input){
        if (input.equals("") || input.split(" ").length != 2){
            return null;
        }
        String[] parts = input.split(" ");
        String potentialUsername = parts[0];
        String potentialGesture = parts[1];
        if (!gestureHashMap.containsKey(potentialGesture)){
            return null;
        };
        return new MyAction(potentialUsername, gestureHashMap.get(potentialGesture));
    }
}
