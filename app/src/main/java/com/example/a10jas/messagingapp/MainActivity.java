package com.example.a10jas.messagingapp;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketExtension;
import com.neovisionaries.ws.client.WebSocketFactory;

import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent;
import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEventListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    /**
     * The echo server on websocket.org.
     */
    private static final String SERVER = "wss://messagapp.herokuapp.com/?username=";

    /**
     * The timeout value in milliseconds for socket connection.
     */
    private static final int TIMEOUT = 5000;

    private WebSocket ws;
    public ArrayList<String> messageArray = new ArrayList<>();
    private String username;
    private String adresseeName;
    private int adresseeId;
    private int userid;
    private AutoCompleteTextView messageTextView;
    private TextView nameTextView;
    public final static String USERNAME = "com.example.a10jas.messagingapp.USERNAME";
    public final static String USERID = "com.example.a10jas.messagingapp.USERID";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Intent intent = getIntent();
        this.username = intent.getStringExtra(ChatListActivity.USERNAME);
        this.userid = intent.getIntExtra(ChatListActivity.USERID,0);
        this.adresseeName = intent.getStringExtra(ChatListActivity.ADRESSEENAME);
        this.adresseeId = intent.getIntExtra(ChatListActivity.ADRESSEEID,0);
        System.out.printf("%s, %d\n", this.username, this.adresseeId);

        android.support.v7.widget.Toolbar topToolBar = (Toolbar) findViewById(R.id.messageToolbar);
        setSupportActionBar(topToolBar);
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null){
            actionBar.setDisplayHomeAsUpEnabled(true);
            topToolBar.getNavigationIcon().setColorFilter(Color.WHITE, PorterDuff.Mode.MULTIPLY);
        }

        this.nameTextView = findViewById(R.id.nameTextView);
        this.nameTextView.setText(this.adresseeName);

        RequestQueue queue = Volley.newRequestQueue(MainActivity.this);
        String url ="https://messagapp.herokuapp.com/messages/";

        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        System.out.println(response);
                        try{
                            ArrayList<JSONObject> messageData = new ArrayList<>();
                            JSONArray dataArray = new JSONObject(response).getJSONArray("data");
                            for (int i=0;i<dataArray.length();i++){
                                MainActivity.this.messageArray.add(dataArray.getJSONObject(i).toString());
                                messageData.add(dataArray.getJSONObject(i));
                            }
                            loadMessages(messageData);

                        }catch (JSONException e){
                            System.out.println(e);
                        }

                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                //textView.setText("That didn't work!");
            }
        }){
            @Override
            protected Map<String,String> getParams(){
                Map<String,String> params = new HashMap<String, String>();
                params.put("userid",Integer.toString(MainActivity.this.userid));
                params.put("adresseeId", Integer.toString(MainActivity.this.adresseeId));
                return params;
            }

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String,String> params = new HashMap<String, String>();
                params.put("Content-Type","application/x-www-form-urlencoded");
                return params;
            }
        };

        // Add the request to the RequestQueue.
        queue.add(stringRequest);

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
        messageTextView = MainActivity.this.findViewById(R.id.messageTextView);
        KeyboardVisibilityEvent.setEventListener(MainActivity.this, new KeyboardVisibilityEventListener() {
            @Override
            public void onVisibilityChanged(boolean isOpen) {

                final ScrollView scrollView = MainActivity.this.findViewById(R.id.messageContainer);
                if (isOpen &&messageTextView.hasFocus()) {
                    View lastChild = scrollView.getChildAt((scrollView.getChildCount() - 1));
                    int bottom = lastChild.getBottom() + scrollView.getPaddingBottom();
                    int sy = scrollView.getScrollY();
                    int sh = scrollView.getHeight();
                    if (bottom >= sy+sh){
                        scrollView.post(new Runnable() {
                            @Override
                            public void run() {
                                scrollView.fullScroll(View.FOCUS_DOWN);
                            }
                        });
                    }
                }
            }
        });

        Button sendMessageButton = MainActivity.this.findViewById(R.id.sendMessageButton);
        sendMessageButton.setOnClickListener(new messageButtonOnClickListener());

    }

    private void loadMessages(ArrayList<JSONObject> messageData){
        LinearLayout linMessageLayout = MainActivity.this.findViewById(R.id.linMessageLayout);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(10, 10, 10, 10); // (left, top, right, bottom)
        layoutParams.gravity = Gravity.RIGHT;
        LinearLayout.LayoutParams layoutParams2 = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        layoutParams2.setMargins(10, 10, 10, 10); // (left, top, right, bottom)
        layoutParams2.gravity = Gravity.LEFT;

        for(int i=0;i<messageData.size();i++){
            TextView textView2 = new TextView(MainActivity.this);
            textView2.setBackgroundResource(R.drawable.tags_rounded_corners);
            GradientDrawable gd = (GradientDrawable) textView2.getBackground();

            try {
                textView2.setText(messageData.get(i).getString("message_content"));
                if(messageData.get(i).getInt("userid") == this.userid){
                    textView2.setLayoutParams(layoutParams);
                    gd.setColor(Color.parseColor("#34eba4"));
                }else{
                    textView2.setLayoutParams(layoutParams2);
                    gd.setColor(Color.parseColor("#fa78ae"));
                }
            }catch (JSONException e){
                textView2.setText("no username");
            }

            textView2.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
            //textView2.setBackgroundColor(0xffffdbdb); // hex color 0xAARRGGBB
            linMessageLayout.addView(textView2);
            textView2.setMaxWidth((int)((((LinearLayout)textView2.getParent())).getWidth()*0.7));
        }

        final ScrollView scrollView = MainActivity.this.findViewById(R.id.messageContainer);
        scrollView.post(new Runnable() {
            @Override
            public void run() {
                scrollView.fullScroll(View.FOCUS_DOWN);
            }
        });
    }

    private void addNewMessage(final String msg, final int userid){
        this.messageArray.add(msg);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final LinearLayout linMessageLayout = MainActivity.this.findViewById(R.id.linMessageLayout);
                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                layoutParams.gravity = Gravity.RIGHT;
                layoutParams.setMargins(10, 10, 10, 10); // (left, top, right, bottom)
                LinearLayout.LayoutParams layoutParams2 = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                layoutParams2.gravity = Gravity.LEFT;
                layoutParams2.setMargins(10, 10, 10, 10); // (left, top, right, bottom)

                TextView textView2 = new TextView(MainActivity.this);
                textView2.setBackgroundResource(R.drawable.tags_rounded_corners);
                GradientDrawable gd = (GradientDrawable) textView2.getBackground();

                if(userid == MainActivity.this.userid){
                    textView2.setLayoutParams(layoutParams);
                    gd.setColor(Color.parseColor("#34eba4"));
                }else{
                    textView2.setLayoutParams(layoutParams2);
                    gd.setColor(Color.parseColor("#fa78ae"));
                }
                textView2.setText(msg);
                textView2.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
                linMessageLayout.addView(textView2);
                textView2.setMaxWidth((int)((((LinearLayout)textView2.getParent())).getWidth()*0.7));

                final ScrollView scrollView = MainActivity.this.findViewById(R.id.messageContainer);

                View lastChild = scrollView.getChildAt((scrollView.getChildCount() - 1));
                int bottom = lastChild.getBottom() + scrollView.getPaddingBottom();
                int sy = scrollView.getScrollY();
                int sh = scrollView.getHeight();

                if (bottom == sy+sh){
                    scrollView.post(new Runnable() {
                        @Override
                        public void run() {
                            scrollView.fullScroll(View.FOCUS_DOWN);
                        }
                    });
                }


            }
        });


    }

    private class messageButtonOnClickListener implements View.OnClickListener{
        String msg;
        String type;
        int senderId;
        int sendToId;
        AutoCompleteTextView messageTextView;

        public messageButtonOnClickListener(){
            this.messageTextView = MainActivity.this.messageTextView;
            this.type = "message";
            this.senderId = MainActivity.this.userid;
            this.sendToId = MainActivity.this.adresseeId;
        }

        @Override
        public void onClick(View v) {
            JSONObject obj = new JSONObject();
            try {
                if(MainActivity.this.messageTextView.length()>0){
                    this.msg = MainActivity.this.messageTextView.getText().toString();
                    obj.put("msg",this.msg);
                    obj.put("type",this.type);
                    obj.put("senderId",this.senderId);
                    obj.put("sendToId",this.sendToId);
                    obj.put("sendToUsername",MainActivity.this.adresseeName);
                    obj.put("senderUsername",MainActivity.this.username);
                    ws.sendText(obj.toString());
                    addNewMessage(this.msg,this.senderId);
                }else{
                    System.out.println("empty text");
                }


                if(MainActivity.this.messageTextView.length()>0){
                    messageTextView.getText().clear();
                }

            }catch (JSONException e){
                System.out.println(e.getStackTrace());
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
        if(this.ws != null){
            this.ws.disconnect();
        }

    }
    /**
     * Connect to the server.
     */
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
                                    //addNewMessage("ping", 0);
                                    break;
                                case("message"):
                                    String msg = JSobj.getString("data");
                                    int senderId = JSobj.getInt("senderId");
                                    addNewMessage(msg, senderId);
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
        this.close();
        Intent intent = new Intent();
        intent.putExtra(USERNAME, this.username);
        intent.putExtra(USERID, this.userid);
        setResult(RESULT_OK,intent);
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
