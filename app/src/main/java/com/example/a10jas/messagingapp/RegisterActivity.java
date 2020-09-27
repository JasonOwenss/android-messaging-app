package com.example.a10jas.messagingapp;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {
    public final static String USERNAME = "com.example.a10jas.messagingapp.USERNAME";
    public final static String USERID = "com.example.a10jas.messagingapp.USERID";
    private EditText editTextUsername;
    private EditText editTextPassword;
    private Button registerButton;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        Toolbar topToolBar = (Toolbar) findViewById(R.id.registerToolbar);
        setSupportActionBar(topToolBar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        topToolBar.getNavigationIcon().setColorFilter(Color.WHITE, PorterDuff.Mode.MULTIPLY);

        this.editTextUsername = this.findViewById(R.id.editTextUsername);
        this.editTextPassword = this.findViewById(R.id.editTextPassword);
        this.registerButton = this.findViewById(R.id.registerButton);
        this.progressBar = findViewById(R.id.progressBar);
        this.registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                register(v);
            }
        });

    }

    private void register(View v){
        this.progressBar.setVisibility(View.VISIBLE);
        final String username = this.editTextUsername.getText().toString();
        final String password = this.editTextPassword.getText().toString();

        RequestQueue queue = Volley.newRequestQueue(RegisterActivity.this);
        String url ="https://messagapp.herokuapp.com/register/";

        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        System.out.println(response);
                        try{
                            JSONObject Jobj = new JSONObject(response);
                            if (Jobj.has("data")){
                                System.out.println("That username is taken");
                                String error = Jobj.getString("data");
                                CharSequence text = error;
                                int duration = Toast.LENGTH_LONG;
                                Toast toast = Toast.makeText(RegisterActivity.this, text, duration);
                                toast.show();

                                RegisterActivity.this.editTextUsername.getText().clear();
                                RegisterActivity.this.editTextPassword.getText().clear();
                                RegisterActivity.this.progressBar.setVisibility(View.INVISIBLE);
                            }else{
                                Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                                int userid = Jobj.getInt("id");
                                intent.putExtra(USERNAME, username);
                                intent.putExtra(USERID, userid);
                                RegisterActivity.this.editTextUsername.getText().clear();
                                RegisterActivity.this.editTextPassword.getText().clear();
                                RegisterActivity.this.progressBar.setVisibility(View.INVISIBLE);
                                setResult(RESULT_OK, intent);
                                finish();
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
}