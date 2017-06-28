package edu.pitt.ycli.test_hello;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaScannerConnection;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.BarChart;
import org.achartengine.chart.LineChart;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.TimeSeries;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class Data_logger_local extends AppCompatActivity   implements SensorEventListener {

    //local data
    private LocationManager locationManager;
    private Location locationGPS = new Location("dummyprovider");
    private double longitude, latitude;

    private LocationListener locationListener;
    private SensorManager senSensorManager;
    private Sensor senAccelerometer;
    private Sensor senGyroscope;
    private Sensor senProximity;
    private Sensor senPressure;
    private Sensor senLight;
    private Sensor senMag;
    private Sensor senTemp;



    private Switch show_sw;
    private boolean show_en=false;
    private Switch plot_sw;
    private boolean plot_en=false;

    private float [] gravity = new float[]{0,0,0};
    private float [] linear_acceleration = new float[]{0,0,0};
    private float [] linear_gyro = new float[]{0,0,0};
    private float proximity;
    private float pressure;
    private float lightness;
    private float [] mag = new float[]{0,0,0};;
    private float temperature;

    public TextView textView;

    public String filename = "local_sensor_data";

    public int temp=0;

    public File dir_path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
    SimpleDateFormat formatter = new SimpleDateFormat("_yyyy_MM_dd_HH_mm");
    Date now = new Date();
    String fileName_formatted = filename + formatter.format(now) + ".txt";
    public File data_file;

    //plot data
    private static Context context;
    private String chartTitleString;
    private double Yaxis_max, Yaxis_min;
    private LinearLayout chartContainer;
    private static GraphicalView chartview;
    private LineChart line;
    private long t_start;
    private long t_record;
    //private View chart;

    private CharSequence[] select_items = null;
    private Resources res = null;
    private AlertDialog alert;
    private int data_choice=0;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_logger_local);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);



        Data_logger_local.context = getApplicationContext();


        /*ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.READ_CONTACTS}, 1);*/


        // Register the listener with the Location Manager to receive location updates
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Location location_initial = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if(location_initial ==  null) {
            locationGPS.setLatitude(0);//40.44);
            locationGPS.setLongitude(0);//-79.95);
        }else
            locationGPS = location_initial;

        LocationListener locationListener =  new myLocationListener();
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 60000, 10, locationListener);

        //locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 10, this);
        //locationGPS = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);



        senSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        senAccelerometer = senSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        senGyroscope = senSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        senProximity = senSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        senPressure = senSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
        senLight = senSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        senMag = senSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        senTemp = senSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);

        senSensorManager.registerListener(this, senAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        senSensorManager.registerListener(this, senGyroscope, SensorManager.SENSOR_DELAY_NORMAL);
        senSensorManager.registerListener(this, senProximity, SensorManager.SENSOR_DELAY_NORMAL);
        senSensorManager.registerListener(this, senPressure, SensorManager.SENSOR_DELAY_NORMAL);
        senSensorManager.registerListener(this, senLight, SensorManager.SENSOR_DELAY_NORMAL);
        senSensorManager.registerListener(this, senMag, SensorManager.SENSOR_DELAY_NORMAL);
        senSensorManager.registerListener(this, senTemp, SensorManager.SENSOR_DELAY_NORMAL);

        res = getResources();
        select_items = res.getStringArray(R.array.data_options);


        chartContainer = (LinearLayout) findViewById(R.id.chart);


        dir_path.mkdirs();
        data_file = new File(dir_path, fileName_formatted);
        try {
            OutputStream os = new FileOutputStream(data_file, true);

            String outputString = "ACC_X (m/s2), ACC_Y, ACC_Z, GYRO_X (rad/s), GYRO_Y, GYRO_Z, MAG_X (uT), MAG_Y, MAG_Z, Pressure (hPa), Light (lx), Long, Lat\n";
            byte[] data = outputString.getBytes();
            os.write(data);
            os.close();

        } catch (IOException e) {
            // Unable to create file, likely because external storage is
            // not currently mounted.
            Log.w("ExternalStorage", "Error writing " + data_file, e);
        }

        Toast.makeText(context, "Recording started (file saved in .../documents/local_sensor_xxxx.txt)"
                , Toast.LENGTH_LONG).show();

        show_sw = (Switch) findViewById(R.id.show_sw);
        plot_sw = (Switch) findViewById(R.id.plotdata_loacal_sw);

        show_sw.setChecked(show_en);
        show_sw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                                         boolean isChecked) {

                if (isChecked) {
                    show_en = true;

                    Toast.makeText(context, "Background Recording..."
                            , Toast.LENGTH_SHORT).show();

                } else {
                    show_en = false;

                    textView.setText(null);

                    Toast.makeText(context, "Background Recording..."
                            , Toast.LENGTH_SHORT).show();
                }
            }

        });

        textView = (TextView) findViewById(R.id.Sensor_content);

        plot_sw.setChecked(plot_en);
        plot_sw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                                         boolean isChecked) {

                if (isChecked) {

                    /////select data to plot
                    showDialog();


                    Toast.makeText(context, "Background Recording..."
                            , Toast.LENGTH_SHORT).show();

                } else {
                    plot_en = false;

                    chartContainer.removeAllViews();

                    Toast.makeText(context, "Background Recording..."
                            , Toast.LENGTH_SHORT).show();
                }
            }

        });

/*        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });*/
    }


    private class myLocationListener implements LocationListener {

        @Override
        public void onLocationChanged(Location loc) {

            if(loc != null)
            locationGPS = loc;
        }

        @Override
        public void onProviderDisabled(String provider) {}

        @Override
        public void onProviderEnabled(String provider) {}

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {}
    }


/*
    public void showLocation(Location currentLocation){
        if(currentLocation != null){
            String s = "";
            s += " Current Location: (";
            s += currentLocation.getLongitude();
            s += ",";
            s += currentLocation.getLatitude();
            s += ")\n Speed: ";
            s += currentLocation.getSpeed();
            s += "\n Direction: ";
            s += currentLocation.getBearing();
            //text.setText(s);
        }
        else{
            //text.setText("");
        }
    }*/

    @Override
    public void onSensorChanged(SensorEvent event) {

        long time_millis= System.currentTimeMillis();

        Sensor sensor = event.sensor;
        if ((senAccelerometer!=null)&(sensor.getType() == Sensor.TYPE_ACCELEROMETER)) {
            final float alpha = 0.8f;

            // Isolate the force of gravity with the low-pass filter.
            gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
            gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
            gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];

            // Remove the gravity contribution with the high-pass filter.
            linear_acceleration[0] = (event.values[0] - gravity[0]);
            linear_acceleration[1] = (event.values[1] - gravity[1]);
            linear_acceleration[2] = (event.values[2] - gravity[2]);
        }
        else if ((senGyroscope!=null)&(sensor.getType() == Sensor.TYPE_GYROSCOPE)) {
            linear_gyro[0] = event.values[0];
            linear_gyro[1] = event.values[1];
            linear_gyro[2] = event.values[2];
        }
        else if ((senMag!=null)&(sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)) {
            mag[0] = event.values[0];
            mag[1] = event.values[1];
            mag[2] = event.values[2];
        }
        else if ((senProximity!=null)&(sensor.getType() == Sensor.TYPE_PROXIMITY)){
            proximity = event.values[0];
        }
        else if ((senPressure!=null)&(sensor.getType() == Sensor.TYPE_PRESSURE)){
            pressure = event.values[0];
        }
        else if ((senLight!=null)&(sensor.getType() == Sensor.TYPE_LIGHT)){
            lightness = event.values[0];
        }
        else if ((senTemp!=null)&(sensor.getType() == Sensor.TYPE_AMBIENT_TEMPERATURE)){
            temperature = senTemp.getPower();//event.values[0];
        }

        //display data
        if(show_en) {


            String message = String.format("%d (ms)\n" +
                            "ACC (x, y, z): (%10.3f, %10.3f, %10.3f) m/s"+Html.fromHtml("<sup><small>2</small></sup>")+"\n"+
                            "Gyro (x, y, z): (%10.3f, %10.3f, %10.3f) rad/s\n"+
                            "Mag (x, y, z): (%10.3f, %10.3f, %10.3f) uT\n"+
                            "Pressure: %10.3f hPa,  Lightness: %10.3f lx\n"+
                            "Proximity: %2.0f cm, (Long., Lat.): (%5.2f, %5.2f)"+"\u00B0",
                    time_millis,
                    linear_acceleration[0], linear_acceleration[1], linear_acceleration[2],
                    linear_gyro[0], linear_gyro[1], linear_gyro[2],
                    mag[0], mag[1], mag[2],
                    pressure, lightness, proximity, locationGPS.getLongitude(), locationGPS.getLatitude());


            textView.setTextSize(14);
            textView.setText(message);//todayStateStr);

        }

        if(plot_en){
            //createChart();

            //sample points to plot
            if((time_millis-t_record)>50) {

                AccelemoterData data=null;

                if(data_choice==0) {
                    data = new AccelemoterData(time_millis, linear_acceleration[0], linear_acceleration[1], linear_acceleration[2]);
                }else if(data_choice==1) {
                    data = new AccelemoterData(time_millis, linear_gyro[0], linear_gyro[1], linear_gyro[2]);
                }else if(data_choice==2) {
                    data = new AccelemoterData(time_millis, mag[0], mag[1], mag[2]);
                }else if(data_choice==3) {
                    data = new AccelemoterData(time_millis, pressure, 0, 0);//ignore Y and Z
                }else if(data_choice==4) {
                    data = new AccelemoterData(time_millis, lightness, 0, 0);//ignore Y and Z
                }


                //only display latest date
                line.addNewData(data, t_start);
                chartview.repaint();

                t_record = time_millis;
            }
        }

        ////save data
        try {

            OutputStream os = new FileOutputStream(data_file, true);

            String outputString = String.format("%d, %10.3f, %10.3f, %10.3f, " +
                    "%10.3f, %10.3f, %10.3f, " +
                    "%10.3f, %10.3f, %10.3f, " +
                    "%10.3f, %10.3f, %5.2f, %5.2f\n",

                    time_millis, linear_acceleration[0], linear_acceleration[1], linear_acceleration[2],
                    linear_gyro[0], linear_gyro[1], linear_gyro[2],
                    mag[0], mag[1], mag[2],
                    pressure, lightness, locationGPS.getLongitude(), locationGPS.getLatitude());

            byte[] data = outputString.getBytes();
            os.write(data);
            os.close();

        } catch (IOException e) {
            // Unable to create file, likely because external storage is
            // not currently mounted.
            Log.w("ExternalStorage", "Error writing " + data_file, e);
        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    protected void onResume() {
        super.onResume();
        senSensorManager.registerListener(this, senAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        senSensorManager.registerListener(this, senGyroscope, SensorManager.SENSOR_DELAY_NORMAL);
        senSensorManager.registerListener(this, senProximity, SensorManager.SENSOR_DELAY_NORMAL);
        senSensorManager.registerListener(this, senPressure, SensorManager.SENSOR_DELAY_NORMAL);
        senSensorManager.registerListener(this, senLight, SensorManager.SENSOR_DELAY_NORMAL);
        senSensorManager.registerListener(this, senMag, SensorManager.SENSOR_DELAY_NORMAL);
        senSensorManager.registerListener(this, senTemp, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        senSensorManager.unregisterListener(this);
    }

    public static void save_data(String filename, String theObjectAr,
                            Context ctx) {
        FileOutputStream fos;
        try {
            fos = ctx.openFileOutput(filename, Context.MODE_PRIVATE);


            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(theObjectAr);
            oos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    public void showDialog( )
    {

        LayoutInflater factory = LayoutInflater.from(this);


        AlertDialog.Builder builder = new AlertDialog.Builder(
                this);
        builder.setTitle("Select data to plot");

        builder.setSingleChoiceItems(select_items, -1,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        //dialog.dismiss();
                        data_choice = which;

                        if(data_choice==0) {
                            Toast.makeText(context, "Selected ACC data", Toast.LENGTH_LONG).show();
                        }else if(data_choice==1) {
                            Toast.makeText(context, "Selected Gyro data", Toast.LENGTH_LONG).show();
                        }else if(data_choice==2) {
                            Toast.makeText(context, "Selected Mag data", Toast.LENGTH_LONG).show();
                        }else if(data_choice==3) {
                            Toast.makeText(context, "Selected Pressure data", Toast.LENGTH_LONG).show();
                        }else if(data_choice==4) {
                            Toast.makeText(context, "Selected Lightness data", Toast.LENGTH_LONG).show();
                        }

                    }
                }).setPositiveButton(
                getResources().getString(R.string.plotdata),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,
                                        int whichButton) {
                        dialog.dismiss();


                        //start to plot
                        plot_en = true;

                        if(data_choice==0) {
                            chartTitleString = "Accelerometer (m/s2)";
                            Yaxis_max = 20;
                            Yaxis_min = -Yaxis_max;
                        }else if(data_choice==1) {
                            chartTitleString = "Gyro (rad/s)";
                            Yaxis_max = 10;
                            Yaxis_min = -Yaxis_max;
                        }else if(data_choice==2) {
                            chartTitleString = "Magnetometer (uT)";
                            Yaxis_max = 200;
                            Yaxis_min = -Yaxis_max;
                        }else if(data_choice==3) {
                            chartTitleString = "Pressure (hPa)";
                            Yaxis_max = 1200;
                            Yaxis_min = 0;
                        }else if(data_choice==4) {
                            chartTitleString = "Lightness (lx)";
                            Yaxis_max = 1200;
                            Yaxis_min = 0;
                        }

                        line = new LineChart();


                        new ArrayList<AccelemoterData>();
                        chartview = ChartFactory.getLineChartView(Data_logger_local.context, line.dataset, line.multiRenderer);
                        chartContainer.addView(chartview, 0);

                        t_start = System.currentTimeMillis();

                        t_record = t_start;
                    }
                });
        alert = builder.create();
        alert.show();


    }

    public class LineChart {

        private GraphicalView view;

        private String chartYTitleString = chartTitleString;
        private double Y_max = Yaxis_max;
        private double Y_min = Yaxis_min;

        private TimeSeries xSeries = new TimeSeries("X");
        private TimeSeries ySeries = new TimeSeries("Y");
        private TimeSeries zSeries = new TimeSeries("Z");

        private XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();

        XYSeriesRenderer xRenderer = new XYSeriesRenderer();
        XYSeriesRenderer yRenderer = new XYSeriesRenderer();
        XYSeriesRenderer zRenderer = new XYSeriesRenderer();

        XYMultipleSeriesRenderer multiRenderer = new XYMultipleSeriesRenderer();

        public LineChart() {


            dataset.addSeries(xSeries);
            dataset.addSeries(ySeries);
            dataset.addSeries(zSeries);


            xRenderer.setColor(Color.RED);
            //xRenderer.setPointStyle(PointStyle.CIRCLE);
            //xRenderer.setFillPoints(true);
            xRenderer.setLineWidth(2);
            xRenderer.setDisplayChartValues(false);


            yRenderer.setColor(Color.GREEN);
            //yRenderer.setPointStyle(PointStyle.CIRCLE);
            //yRenderer.setFillPoints(true);
            yRenderer.setLineWidth(2);
            yRenderer.setDisplayChartValues(false);


            zRenderer.setColor(Color.BLUE);
            //zRenderer.setPointStyle(PointStyle.CIRCLE);
            //zRenderer.setFillPoints(true);
            zRenderer.setLineWidth(2);
            zRenderer.setDisplayChartValues(false);


            multiRenderer.setMargins(new int[]{35, 10, 10, 5});
            //multiRenderer.setMarginsColor(Color.argb(0x00, 0xff, 0x00, 0x00));
            multiRenderer.setShowLabels(true);
            //multiRenderer.setXAxisMin(xSeries.getMaxX()-5000);
            //multiRenderer.setXAxisMax(xSeries.getMaxX());
            multiRenderer.setLabelsColor(Color.RED);
            multiRenderer.setLabelsTextSize(24);

            multiRenderer.setAxisTitleTextSize(30);
            multiRenderer.setChartTitleTextSize(30);
            multiRenderer.setChartTitle(chartYTitleString);
            multiRenderer.setXTitle("Time (ms)");


            multiRenderer.setFitLegend(true);
            multiRenderer.setLegendHeight(20);
            multiRenderer.setShowLegend(true);
            multiRenderer.setLegendTextSize(24);

            // setting x axis label align
            multiRenderer.setXLabelsAlign(Paint.Align.CENTER);
            // setting y axis label to align
            multiRenderer.setYLabelsAlign(Paint.Align.LEFT);
            // setting text style
            multiRenderer.setTextTypeface("sans_serif", Typeface.NORMAL);
            // setting number of values to display in y axis
            multiRenderer.setYLabels(5);
            //multiRenderer.setYTitle(chartYTitleString);//"Accelerometer (m/s2)");
            multiRenderer.setYAxisMax(Y_max);
            multiRenderer.setYAxisMin(Y_min);
            multiRenderer.setZoomButtonsVisible(false);
            multiRenderer.setBackgroundColor(Color.BLACK);
            multiRenderer.setApplyBackgroundColor(true);
            multiRenderer.setPanEnabled(true, false);
            multiRenderer.setPanLimits(new double[]{0.0, Double.MAX_VALUE, Double.MIN_VALUE, Double.MAX_VALUE});

            multiRenderer.setZoomEnabled(true, true);


            multiRenderer.setShowGrid(true);
            multiRenderer.setGridColor(Color.WHITE);

            multiRenderer.addSeriesRenderer(xRenderer);
            multiRenderer.addSeriesRenderer(yRenderer);
            multiRenderer.addSeriesRenderer(zRenderer);
        }

        public GraphicalView getView(Context context) {
            view = ChartFactory.getLineChartView(context, dataset, multiRenderer);
            return view;
        }

        public void addNewData(AccelemoterData data, long t) {
            xSeries.add(data.getTime() - t, data.getX());
            ySeries.add(data.getTime() - t, data.getY());
            zSeries.add(data.getTime() - t, data.getZ());

        }
    }

        public class AccelemoterData {
            private long time;
            private float x;
            private float y;
            private float z;

            public AccelemoterData(long time, float x, float y, float z) {
                this.time = time;
                this.x = x;
                this.y = y;
                this.z = z;
            }

            public long getTime() {
                return time;
            }

            public float getX() {
                return x;
            }

            public float getY() {
                return y;
            }

            public float getZ() {
                return z;
            }
        }


}
