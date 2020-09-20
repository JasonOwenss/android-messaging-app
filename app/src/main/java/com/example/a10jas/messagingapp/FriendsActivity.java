package com.example.a10jas.messagingapp;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FriendsActivity extends AppCompatActivity {

    private String username;
    private int userid;
    private WebSocket ws;
    private ArrayList<JSONObject> friendRequests = new ArrayList<>();
    private LinearLayout linLayoutFriendRequests;
    private RequestQueue queue;
    private Button searchButton;
    private EditText editTextSearchName;
    private static final String SERVER = "wss://messagapp.herokuapp.com/?username=";
    private static final int TIMEOUT = 5000;
    public final static String USERNAME = "com.example.a10jas.messagingapp.USERNAME";
    public final static String USERID = "com.example.a10jas.messagingapp.USERID";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friends);

        Intent intent = getIntent();
        this.username = intent.getStringExtra(ChatListActivity.USERNAME);
        this.userid = intent.getIntExtra(ChatListActivity.USERID,0);
        System.out.printf("%s, %d\n", this.username, userid);

        Toolbar topToolBar = (Toolbar) findViewById(R.id.toolbarFriendSearch);
        setSupportActionBar(topToolBar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        topToolBar.getNavigationIcon().setColorFilter(Color.WHITE, PorterDuff.Mode.MULTIPLY);


        this.linLayoutFriendRequests = findViewById(R.id.linLayoutFriendRequests);
        this.editTextSearchName = findViewById(R.id.editTextSearchName);
        this.searchButton = findViewById(R.id.searchButton);
        this.searchButton.setOnClickListener(new onSearchFriendClickListener());

        String url = "https://messagapp.herokuapp.com/friendrequests/";

        this.queue = Volley.newRequestQueue(FriendsActivity.this);
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
                                FriendsActivity.this.friendRequests.add(dataArray.getJSONObject(i));
                                messageData.add(dataArray.getJSONObject(i));
                            }
                            loadFriendRequests(messageData);

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
                params.put("adresseeId", Integer.toString(FriendsActivity.this.userid));

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
        this.queue.add(stringRequest);

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

    private void loadFriendRequests(final ArrayList<JSONObject> messageData){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                System.out.println(messageData);
                LinearLayout linLayoutFriendRequests = FriendsActivity.this.linLayoutFriendRequests;
                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                layoutParams.gravity = Gravity.LEFT;
                layoutParams.setMargins(10, 10, 10, 10); // (left, top, right, bottom)


                linLayoutFriendRequests.removeAllViews();


                for(int i=0;i<messageData.size();i++) {

                    LinearLayout horizontalLinLayout = new LinearLayout(FriendsActivity.this);
                    LinearLayout.LayoutParams horizontalLayoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
                    horizontalLayoutParams.setMargins(10, 10, 10, 10);
                    horizontalLinLayout.setLayoutParams(horizontalLayoutParams);
                    horizontalLinLayout.setOrientation(LinearLayout.HORIZONTAL);
                    horizontalLinLayout.setMinimumHeight(100);

                    LinearLayout.LayoutParams nameLayoutParams = new LinearLayout.LayoutParams(0,
                            LinearLayout.LayoutParams.WRAP_CONTENT, 0.6f);
                    LinearLayout.LayoutParams acceptLayoutParams = new LinearLayout.LayoutParams(0,
                            LinearLayout.LayoutParams.MATCH_PARENT, 0.2f);
                    LinearLayout.LayoutParams declineLayoutParams = new LinearLayout.LayoutParams(0,
                            LinearLayout.LayoutParams.MATCH_PARENT, 0.2f);

                    TextView textView2 = new TextView(FriendsActivity.this);
                    textView2.setLayoutParams(nameLayoutParams);
                    try {
                        textView2.setText(messageData.get(i).getString("username"));
                        textView2.setId(messageData.get(i).getInt("userid"));
                        System.out.println(messageData.get(i).getString("username"));
                    } catch (JSONException e) {
                        textView2.setText("no username");
                    }

                    textView2.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
                    horizontalLinLayout.setBackgroundColor(0xffffdbdb); // hex color 0xAARRGGBB
                    Button acceptButton = new Button(FriendsActivity.this);
                    Button declineButton = new Button(FriendsActivity.this);
                    acceptButton.setText("accept");
                    declineButton.setText("decline");
                    acceptButton.setLayoutParams(acceptLayoutParams);
                    declineButton.setLayoutParams(declineLayoutParams);
                    acceptButton.setOnClickListener(new onFriendRequestClickListener(textView2.getText().toString(),textView2.getId(),"acceptfriendrequest"));
                    declineButton.setOnClickListener(new onFriendRequestClickListener(textView2.getText().toString(),textView2.getId(), "declinefriendrequest"));
                    horizontalLinLayout.addView(textView2);
                    horizontalLinLayout.addView(acceptButton);
                    horizontalLinLayout.addView(declineButton);

                    linLayoutFriendRequests.addView(horizontalLinLayout);
                }
            }
        });

    }

    private class onFriendRequestClickListener implements View.OnClickListener {
        String requesterName;
        int requesterId;
        String url;

        public onFriendRequestClickListener(String requesterName, int requesterId, String url){
            this.requesterId = requesterId;
            this.requesterName = requesterName;
            this.url = "https://messagapp.herokuapp.com/"+url+"/";
        }
        @Override
        public void onClick(View v){


            StringRequest stringRequest = new StringRequest(Request.Method.POST, this.url,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            System.out.println(response);
                            try{

                                String data = new JSONObject(response).getString("data");

                                if (data.equals("success")){

                                    for (JSONObject fr: FriendsActivity.this.friendRequests){
                                        if(fr.getString("username") == requesterName){
                                            FriendsActivity.this.friendRequests.remove(fr);

                                        }
                                    }
                                    FriendsActivity.this.loadFriendRequests(FriendsActivity.this.friendRequests);
                                    FriendsActivity.this.ws.sendText(new JSONObject().put("type","friendRequestAccept")
                                                        .put("adresseeId",Integer.toString(FriendsActivity.this.userid))
                                                        .put("adresseeName",FriendsActivity.this.username)
                                                        .put("requesterName",requesterName).toString());

                                }else{
                                    CharSequence text = "Error accepting or declining friend request!";
                                    int duration = Toast.LENGTH_SHORT;

                                    Toast toast = Toast.makeText(FriendsActivity.this, text, duration);
                                    toast.show();
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
            }){
                @Override
                protected Map<String,String> getParams(){
                    Map<String,String> params = new HashMap<String, String>();
                    params.put("adresseeId", Integer.toString(FriendsActivity.this.userid));
                    params.put("requesterId", Integer.toString((requesterId)));

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
            FriendsActivity.this.queue.add(stringRequest);
        }
    }

    private class onSearchFriendClickListener implements View.OnClickListener{

        //String searchName;

        public onSearchFriendClickListener(){
            //this.searchName = FriendsActivity.this.editTextSearchName.getText().toString();
        }

        @Override
        public void onClick(View v) {
            String searchName = FriendsActivity.this.editTextSearchName.getText().toString();
            System.out.printf("adresseeName is %s\n",searchName);

            try{
                if (searchName.length() < 1){
                    CharSequence text = "Enter a name";
                    int duration = Toast.LENGTH_LONG;
                    Toast toast = Toast.makeText(FriendsActivity.this, text, duration);
                    toast.show();
                }else{
                    FriendsActivity.this.ws.sendText(new JSONObject().put("type", "friendRequest")
                            .put("requesterId",FriendsActivity.this.userid)
                            .put("requesterName", FriendsActivity.this.username)
                            .put("adresseeName",searchName).toString());
                    FriendsActivity.this.editTextSearchName.getText().clear();
                }

            }catch (JSONException e){
                System.out.println(e.getStackTrace());
            }

        }
    }

    private void runWS() throws IOException, WebSocketException {
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
                        System.out.println(message);
                        try {
                            JSONObject JSobj = new JSONObject(message);
                            String msgType = JSobj.getString("type");
                            switch(msgType){
                                case("ping"):
                                    websocket.sendText("{\"type\":\"ping\"}");
                                    break;
                                case("friendRequest"):
                                    String requesterName = JSobj.getString("requesterName");
                                    int requesterId = JSobj.getInt("requesterId");
                                    FriendsActivity.this.friendRequests.add(new JSONObject().put("username",requesterName)
                                            .put("userid",requesterId));
                                    loadFriendRequests(FriendsActivity.this.friendRequests);
                                    break;
                                case("error"):
                                    String error = JSobj.getString("data");
                                    CharSequence text = error;
                                    int duration = Toast.LENGTH_LONG;

                                    Toast toast = Toast.makeText(FriendsActivity.this, text, duration);
                                    toast.show();
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
        close();
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