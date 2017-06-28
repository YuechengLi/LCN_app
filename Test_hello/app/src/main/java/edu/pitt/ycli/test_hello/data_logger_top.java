package edu.pitt.ycli.test_hello;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

public class data_logger_top extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_logger_top);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        /*
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });*/
    }



    public void Start_Data_Logger(View view) {
        Intent intent = new Intent(this, Data_Logger.class);
        startActivity(intent);
    }


    public void Start_Data_Logger_bgm111(View view) {
        Intent intent = new Intent(this, Data_logger_bgm111.class);
        startActivity(intent);
    }

    public void Start_Data_Logger_local(View view) {
        Intent intent = new Intent(this, Data_logger_local.class);
        startActivity(intent);
    }

}
