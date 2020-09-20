package com.example.a10jas.messagingapp;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {
    public final static String USERNAME = "com.example.a10jas.messagingapp.USERNAME";
    public final static String USERID = "com.example.a10jas.messagingapp.USERID";
    private String password;
    private EditText editTextUsername;
    private EditText editTextPassword;
    private Button registerButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        Button loginButton = findViewById(R.id.loginButton);
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                authenticate(v);
            }
        });
        this.editTextUsername = findViewById(R.id.editTextUsername);
        this.editTextPassword = findViewById(R.id.editTextPassword);
        this.registerButton = findViewById(R.id.registerButton);
        this.registerButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                registerAccount(v);
            }
        });
    }

    private void registerAccount(View v){
        Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
        startActivityForResult(intent, 1);
    }

    private void authenticate(View v){

        final String username = this.editTextUsername.getText().toString();
        final String password = this.editTextPassword.getText().toString();

        RequestQueue queue = Volley.newRequestQueue(LoginActivity.this);
        String url ="https://messagapp.herokuapp.com/login/";

        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        System.out.println(response);
                        try{
                            JSONObject Jobj = new JSONObject(response);
                            if (Jobj.has("data")){
                                System.out.println("That user does not exist");
                            }else{
                                Intent intent = new Intent(LoginActivity.this, ChatListActivity.class);
                                int userid = Jobj.getInt("id");
                                intent.putExtra(USERNAME, username);
                                intent.putExtra(USERID, userid);
                                LoginActivity.this.editTextUsername.getText().clear();
                                LoginActivity.this.editTextPassword.getText().clear();
                                startActivity(intent);
                            }
                        }catch(JSONException e){
                            System.out.println(e.getStackTrace());
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
                params.put("username",username);
                params.put("password", password);
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

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode,resultCode,data);
        System.out.println("ONACTIVITY");
        if (resultCode == RESULT_OK && requestCode == 1) {
            if(data != null){
                String username = data.getStringExtra(MainActivity.USERNAME);
                int userid = data.getIntExtra(MainActivity.USERID, 0);
                Intent intent = new Intent(LoginActivity.this, ChatListActivity.class);
                intent.putExtra(USERNAME, username);
                intent.putExtra(USERID, userid);
                startActivity(intent);
            }
        }
    }
}