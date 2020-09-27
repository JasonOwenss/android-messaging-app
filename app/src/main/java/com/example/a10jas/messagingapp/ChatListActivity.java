package com.example.a10jas.messagingapp;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketExtension;
import com.neovisionaries.ws.client.WebSocketFactory;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatListActivity extends AppCompatActivity {

    private static final String SERVER = "wss://messagapp.herokuapp.com/?username=";
    private static final int TIMEOUT = 5000;
    private WebSocket ws;
    private String username;
    private int userid;
    private ArrayList<JSONObject> chats = new ArrayList();
    private ArrayList<JSONObject> friendRequests = new ArrayList<>();
    private Button buttonFriendRequests;
    private RequestQueue queue;
    private LinearLayout chatListLinearLayout;
    private ProgressBar progressBar;
    public final static String USERNAME = "com.example.a10jas.messagingapp.USERNAME";
    public final static String USERID = "com.example.a10jas.messagingapp.USERID";
    public final static String ADRESSEENAME = "com.example.a10jas.messagingapp.ADRESSEENAME";
    public final static String ADRESSEEID = "com.example.a10jas.messagingapp.ADRESSEEID";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_list);
        Intent intent = getIntent();
        this.username = intent.getStringExtra(LoginActivity.USERNAME);
        this.userid = intent.getIntExtra(LoginActivity.USERID,0);
        System.out.printf("%s, %d\n", this.username, userid);

        Toolbar topToolBar = (Toolbar) findViewById(R.id.chatListToolbar);
        setSupportActionBar(topToolBar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        topToolBar.getNavigationIcon().setColorFilter(Color.WHITE,PorterDuff.Mode.MULTIPLY);


        this.chatListLinearLayout = findViewById(R.id.chatListLinearLayout);
        this.progressBar = findViewById(R.id.progressBar);
        buttonFriendRequests = findViewById(R.id.buttonFriendRequests);
        buttonFriendRequests.setOnClickListener(new friendRequestButtonOnClickListener());

        this.queue = Volley.newRequestQueue(ChatListActivity.this);
        chatsPostRequest();

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(new Runnable() {
            public void run() {
                try {
                    runWS();
                } catch (WebSocketException e){
                    System.out.println(e);
                } catch (IOException e){
                    System.out.println(e);
                }
            }
        });
        executorService.shutdown();
    }

    private void chatsPostRequest(){
        String url ="https://messagapp.herokuapp.com/getfriends/" + Integer.toString(this.userid);

        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        System.out.println(response);
                        try{
                            JSONObject Jobj = new JSONObject(response);
                            if(Jobj.has("error")){
                                String error = Jobj.getString("error");
                                CharSequence text = "Server Error: error loading friends";
                                int duration = Toast.LENGTH_LONG;
                                Toast toast = Toast.makeText(ChatListActivity.this, text, duration);
                                toast.show();
                            }else{
                                ArrayList<JSONObject> chatData = new ArrayList<>();
                                JSONArray dataArray = new JSONObject(response).getJSONArray("data");
                                for (int i=0;i<dataArray.length();i++){
                                    ChatListActivity.this.chats.add(dataArray.getJSONObject(i));
                                    chatData.add(dataArray.getJSONObject(i));
                                }
                                loadChats(chatData);
                            }

                        }catch (JSONException e){
                            System.out.println(e);
                        }

                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                //textView.setText("That didn't work!");
            }
        });


        // Add the request to the RequestQueue.
        this.queue.add(stringRequest);
    }

    private void loadChats(final ArrayList<JSONObject> dataArray){

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                LinearLayout chatListLinearLayout = ChatListActivity.this.chatListLinearLayout;
                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                layoutParams.gravity = Gravity.LEFT;
                //layoutParams.setMargins(5, 5, 5, 5); // (left, top, right, bottom)

                chatListLinearLayout.removeAllViews();

                for(int i=0;i<dataArray.size();i++){

                    TextView textView2 = new TextView(ChatListActivity.this);
                    textView2.setLayoutParams(layoutParams);
                    try {
                        textView2.setText(dataArray.get(i).getString("username"));
                        textView2.setId(dataArray.get(i).getInt("userid"));
                        System.out.println(dataArray.get(i).getString("username"));
                    }catch (JSONException e){
                        textView2.setText("no username");
                    }
                    textView2.setClickable(true);
                    textView2.setOnClickListener(new onChatClickListener(textView2.getText().toString(),textView2.getId()));
                    textView2.setTextSize(TypedValue.COMPLEX_UNIT_SP, 26);
                    textView2.setPadding(25,25,25,25);
                    textView2.setBackgroundResource(R.drawable.textlines);
                    LayerDrawable ld = (LayerDrawable) textView2.getBackground();
                    textView2.setBackground(ld);
                    TypedValue outValue = new TypedValue();
                    ChatListActivity.this.getTheme().resolveAttribute(
                            android.R.attr.selectableItemBackground, outValue, true);
                    textView2.setForeground(getDrawable(outValue.resourceId));
                    chatListLinearLayout.addView(textView2);
                }
            }
        });


    }

    private class onChatClickListener implements View.OnClickListener{
        String adresseeName;
        int adresseeId;

        public onChatClickListener(String adresseeName, int adresseeId){
            this.adresseeId = adresseeId;
            this.adresseeName = adresseeName;
        }

        @Override
        public void onClick(View v){
            Intent intent = new Intent(ChatListActivity.this, MainActivity.class);
            intent.putExtra(USERNAME, username);
            intent.putExtra(USERID, userid);
            intent.putExtra(ADRESSEENAME, this.adresseeName);
            intent.putExtra(ADRESSEEID, this.adresseeId);
            close();
            startActivityForResult(intent, 1);
        }
    }

    private class friendRequestButtonOnClickListener implements View.OnClickListener{


        public friendRequestButtonOnClickListener(){

        }

        @Override
        public void onClick(View v){
            Intent intent = new Intent(ChatListActivity.this, FriendsActivity.class);
            intent.putExtra(USERNAME, username);
            intent.putExtra(USERID, userid);
            close();
            startActivityForResult(intent, 1);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode,resultCode,data);
        System.out.println("ONACTIVITY");
        if (resultCode == RESULT_OK && requestCode == 1) {
            if(data != null){
                this.username = data.getStringExtra(MainActivity.USERNAME);
                this.userid = data.getIntExtra(MainActivity.USERID, 0);
                chatsPostRequest();
                ExecutorService executorService = Executors.newSingleThreadExecutor();
                executorService.execute(new Runnable() {
                    public void run() {
                        try {
                            runWS();
                        } catch (WebSocketException e){
                            System.out.println(e);
                        } catch (IOException e){
                            System.out.println(e);
                        }
                    }
                });
                executorService.shutdown();
            }
        }
    }

    private void runWS() throws IOException, WebSocketException{
        // Connect to the echo server.
        this.ws = this.connect();

        // Close the WebSocket.
        //ws.disconnect();
    }

    private void close(){
        this.ws.disconnect();
    }

    private WebSocket connect() throws IOException, WebSocketException
    {

        return new WebSocketFactory()
                .setConnectionTimeout(TIMEOUT)
                .createSocket(SERVER+username)
                .addListener(new WebSocketAdapter() {
                    // A text message arrived from the server.
                    public void onTextMessage(WebSocket websocket, String message) {
                        try {
                            JSONObject JSobj = new JSONObject(message);
                            String msgType = JSobj.getString("type");
                            switch(msgType){
                                case("ping"):
                                    websocket.sendText("{\"type\":\"ping\"}");
                                    break;
                                case("friendRequest"):
                                    String msg = JSobj.getString("data");
                                    int senderId = JSobj.getInt("senderId");
                                    break;
                                case("friendRequestAccept"):
                                    String adresseeName = JSobj.getString("adresseeName");
                                    int adresseeId = JSobj.getInt("adresseeId");
                                    System.out.println(ChatListActivity.this.chats.toString());
                                    ChatListActivity.this.chats.add(new JSONObject().put("username",adresseeName)
                                            .put("userid",adresseeId));
                                    System.out.println(ChatListActivity.this.chats.toString());
                                    //loadChats(ChatListActivity.this.chats);
                                    ChatListActivity.this.chatsPostRequest();
                                    break;
                            }

                        } catch(org.json.JSONException e){
                            System.out.println(e);
                        }
                    }
                })
                .addExtension(WebSocketExtension.PERMESSAGE_DEFLATE)
                .connect();
    }

    @Override
    public void onBackPressed(){
        ws.disconnect();
        System.out.println("Closing");
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
        }
        return true;
    }
}