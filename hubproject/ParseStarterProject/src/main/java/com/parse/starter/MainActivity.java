/*
 * Copyright (c) 2015-present, Parse, LLC.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.parse.starter;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;

import com.parse.LogInCallback;
import com.parse.ParseAnalytics;
import com.parse.ParseAnonymousUtils;
import com.parse.ParseException;
import com.parse.ParseUser;
import com.parse.SaveCallback;


public class MainActivity extends AppCompatActivity {
    DatabaseHelper helper = new DatabaseHelper(this);
    Switch riderOrDriver;

    public void onButtonClick(View v){
        EditText mUser=(EditText) findViewById(R.id.TFusername);
        EditText mPass=(EditText) findViewById(R.id.TFpassword);
        if (v.getId() == R.id.Blogin)
        {
            String user = mUser.getText().toString();
            String pass = mPass.getText().toString();

            String password = helper.searchPass(user);
            if (pass.equals(password))
            {
                Intent i = new Intent(MainActivity.this, YourLocation.class);
                i.putExtra("Username", user);
                startActivity(i);
            }
            else
            {
                Toast temp = Toast.makeText(MainActivity.this, "Invalid username or password", Toast.LENGTH_SHORT);
                temp.show();
            }

        }
        if (v.getId() == R.id.Bsignup)
        {
            Intent i = new Intent(MainActivity.this, SignUp.class);
            startActivity(i);
        }
        String riderDriver="rider";


        ParseUser.getCurrentUser().setUsername(mUser.getText().toString());

        if(riderOrDriver.isChecked()){
            riderDriver="driver";
        }

        ParseUser.getCurrentUser().put("riderOrDriver", riderDriver);

        ParseUser.getCurrentUser().saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if (e == null){
                    redirectUser();
                }
            }
        });
    }

    public void redirectUser(){
        if (ParseUser.getCurrentUser().get("riderOrDriver").equals("rider")){
            Intent i = new Intent(getApplicationContext(), YourLocation.class);
            startActivity(i);
        }
        else{
            Intent i = new Intent(getApplicationContext(), ViewRequests.class);
            startActivity(i);
        }
    }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

     // ParseUser.getCurrentUser().put("riderOrDriver", "rider");

      riderOrDriver = (Switch) findViewById(R.id.riderOrDriverSwitch);


    ParseAnalytics.trackAppOpenedInBackground(getIntent());

      getSupportActionBar().hide();

      if(ParseUser.getCurrentUser().getUsername() == null){
          ParseAnonymousUtils.logIn(new LogInCallback() {
              @Override
              public void done(ParseUser user, ParseException e) {
                  if(e != null){
                      Log.d("MyApp", "Anonymous login failed.");
                  }
                  else{
                      Log.d("MyApp", "Anonymous user logged in.");
                  }
              }
          });
      }
      else{
          if (ParseUser.getCurrentUser().get("riderOrDriver") != null){
              redirectUser();
          }
      }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.menu_main, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Handle action bar item clicks here. The action bar will
    // automatically handle clicks on the Home/Up button, so long
    // as you specify a parent activity in AndroidManifest.xml.
    int id = item.getItemId();

    //noinspection SimplifiableIfStatement
    if (id == R.id.action_settings) {
      return true;
    }

    return super.onOptionsItemSelected(item);
  }
}
