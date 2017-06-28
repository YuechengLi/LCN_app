package edu.pitt.ycli.test_hello;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class image_tools extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_tools);
    }

    public void Start_ImageManipulation(View view) {
        Intent intent = new Intent(this, ImageManipulationsActivity.class);
        startActivity(intent);
    }

    public void Start_FaceDect(View view) {
        Intent intent = new Intent(this, FdActivity.class);
        startActivity(intent);
    }

    public void Start_PlateDect(View view) {
        //Intent intent = new Intent(this, FdActivity.class);
        //startActivity(intent);
    }

}
