package edu.pitt.ycli.test_hello;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

public class home extends Activity {

    private static Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        home.context = getApplicationContext();


        Button exit_button = (Button) findViewById(R.id.button_exit);
        exit_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                finish();

                System.exit(0);
            }
        });

/*
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Laboratory for Computational Neuroscience\n 3520 Forbes Ave., Pittsburgh, PA 15213 ", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });*/
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

        switch (item.getItemId()) {

            //case R.id.action_settings:

            //   return true;

            case R.id.action_About:

                AlertDialog.Builder builder_about = new AlertDialog.Builder(this);
                builder_about.setTitle("About this App");
                builder_about.setMessage("Designed by ycLi in LCN.\n" +
                        "It can be used for our research without any charge ^-^.\n\n" +
                        "Laboratory for Computational Neuroscience\n" +
                        "3520 Forbes Ave.\n" +
                        "Pittsburgh, PA 15213");

                builder_about.setPositiveButton(
                        "Got It!",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });


                AlertDialog alert_about = builder_about.create();
                alert_about.show();


            default:
                return super.onOptionsItemSelected(item);
        }

    }

    public void Start_Data_Logger_top(View view) {
        Intent intent = new Intent(this, data_logger_top.class);
        startActivity(intent);
    }


    public void Start_ebutton(View view) {
        Intent intent = new Intent(this, LpEButton.class);
        startActivity(intent);
    }


    public void alg_imagetools(View view) {
        Intent intent = new Intent(this, image_tools.class);
        //Intent intent = new Intent(this, ImageManipulationsActivity.class);
        startActivity(intent);
    }

    public void alg_foodRec(View view) {
        Intent intent = new Intent(this, FoodRec.class);
        startActivity(intent);
    }
}
