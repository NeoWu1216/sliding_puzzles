package com.example.android.imageprocessinggame;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button buttonStart = (Button)(findViewById(R.id.buttonStart));
        buttonStart.setOnClickListener(this);
        Button buttonCustomize = (Button)(findViewById(R.id.buttonCustomize));
        buttonCustomize.setOnClickListener(this);
        Button buttonHelp = (Button)(findViewById(R.id.buttonHelp));
        buttonHelp.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        Intent paramIntent = null;
        if (v.getId() == R.id.buttonStart) {
            paramIntent = new Intent(this, GameParameter.class);
        } else if (v.getId() == R.id.buttonCustomize) {
            paramIntent = new Intent(this, GameParameterCustomized.class);
        } else if (v.getId() == R.id.buttonHelp) {
            paramIntent = new Intent(this, FAQ.class);
        }// else let it go...
        startActivity(paramIntent);
    }
}
