package edu.pitt.ycli.test_hello;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
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
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.LineChart;
import org.achartengine.model.TimeSeries;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;


public class Data_Logger extends AppCompatActivity{


    public class Sensor_data {
        float acc_x;
        float acc_y;
        float acc_z;
        float gyro_x;
        float gyro_y;
        float gyro_z;
        float mag_x;
        float mag_y;
        float mag_z;
        float hmag_x;
        float hmag_y;
        float hmag_z;
        float temp;
        float pressure;
        float humidity;
        float VIlight;
        float IRlight;
        float uv_index;
        float proximity;
        int time_ms;

    }

    private Context context_this;
    private static Context context;

    private LocationManager locationManager;
    private Location locationGPS = new Location("dummyprovider");


    private short temp=0;

    private Button init_msp430, Visible,list;

    final int handlerState = 0;
    Handler bluetoothIn;
    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;
    private Set<BluetoothDevice>pairedDevices;
    private StringBuilder recDataString = new StringBuilder();
    private ConnectedThread mConnectedThread;
    // SPP UUID service - this should work for most devices
    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    // String for MAC address
    private static String bt_address;
    private String readMessage;
    private boolean validdata_recvd = false;



    private ListView listview;
    private Switch BT_sw;
    private TextView motion_view;
    private String message_sensordata;

    private Switch  showdata_sw;
    private boolean showdata_en=false;
    private Switch plot_sw;
    private boolean plot_en=false;

    private String chartTitleString;
    private double Yaxis_max, Yaxis_min;
    private LinearLayout chartContainer;
    private static GraphicalView chartview;
    private LineChart line;
    private long t_start;
    private long t_record;

    private CharSequence[] select_items = null;
    private Resources res = null;
    private AlertDialog alert;
    private int data_choice=0;

    public String Data_logger_name = "LCN_DataLogger";
    public boolean Datalogger_found=false;
    BluetoothDevice paired_result = null;

    //pre-checked!!!
    private char motion_on=1;
    private char baro_on=1;
    private char optical_on=0;
    private char LIS_on = 1;

    public StringBuilder sensorDataStr;

    private int endOfLineIndex;
    private int index_start;//
    private String dataInPrint;    //
    private String [] valid_parts;
    private char initialization_data;
    private String sensor_raw;
    private Sensor_data motion_data = new Sensor_data();


    public String filename = "msp430_sensor_data";
    public File dir_path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
    SimpleDateFormat formatter = new SimpleDateFormat("_yyyy_MM_dd_HH_mm");
    Date now = new Date();
    String fileName_formatted = filename + formatter.format(now) + ".txt";
    public File data_file;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data__logger);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Intent intent = getIntent();

        context_this = this;

        Data_Logger.context = getApplicationContext();

        //gps
        // Register the listener with the Location Manager to receive location updates
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Location location_initial = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if(location_initial ==  null) {
            locationGPS.setLatitude(0);//40.44);
            locationGPS.setLongitude(0);//-79.95);
        }else
            locationGPS = location_initial;
        LocationListener locationListener =  new myLocationListener();
        //if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 10, locationListener);


        motion_view = (TextView)findViewById(R.id.MotionData_view);


        res = getResources();
        select_items = res.getStringArray(R.array.data_options_msp430);
        chartContainer = (LinearLayout) findViewById(R.id.chart_msp430);


        //file operation
        dir_path.mkdirs();
        data_file = new File(dir_path, fileName_formatted);
        try {
            OutputStream os = new FileOutputStream(data_file, true);

            String outputString = "ACC_X (mg), ACC_Y, ACC_Z, 10*GYRO_X (rad/s), 10*GYRO_Y, 10*GYRO_Z, 10*MAG_X (uT), 10*MAG_Y, MAG_Z, LIS_MAG_X (mGauss), LIS_MAG_Y, LIS_MAG_Z, T (0.01 Celcius), P (10Pa), H (%), VisibleLight, IRLight, UV_index, proximity, MSP430_Time(ms), local Long, local Lat\n";
            byte[] data = outputString.getBytes();
            os.write(data);
            os.close();

        } catch (IOException e) {
            // Unable to create file, likely because external storage is
            // not currently mounted.
            Log.w("ExternalStorage", "Error writing " + data_file, e);
        }

//        Toast.makeText(context, "File will be saved in .../documents/msp430_sensor_xxxx.txt)"
//                , Toast.LENGTH_LONG).show();


//        list = (Button)findViewById(R.id.button_list);
//        listview = (ListView)findViewById(R.id.listView);

        bluetoothIn = new Handler() {
            public void handleMessage(android.os.Message msg) {
                if (msg.what == handlerState) {                                     //if message is what we want
                    readMessage = (String) msg.obj;                                                                // msg.arg1 = bytes from connect thread
                    recDataString.append(readMessage);                                      //keep appending to string until ~
                    endOfLineIndex = recDataString.indexOf("~");                    // determine the end-of-line

                    if (endOfLineIndex > 0){//&( start_mark.charAt(0) == '#')) {//(index_start>=0)&(endOfLineIndex>index_start)){//                                           // make sure there data before ~
                        //String dataInPrint = recDataString.substring(0, endOfLineIndex);    //index_start+1, endOfLineIndex);    // extract string

                        //txtString.setText("Data Received = " + dataInPrint);
                        //int dataLength = dataInPrint.length();                          //get length of data received
                        //txtStringLength.setText("String Length = " + String.valueOf(dataLength));
                        index_start = recDataString.lastIndexOf("#",endOfLineIndex);
                        if (index_start>0){//recDataString.charAt(0) == '#') {
                            //index_start = recDataString.indexOf("#");//start of data
                            dataInPrint = recDataString.substring(index_start+1, endOfLineIndex);    //
                            valid_parts = dataInPrint.split(",");

                            validdata_recvd = true;


                            ////save data
                            try {

                                OutputStream os = new FileOutputStream(data_file, true);

                                //add gps data
                                String outputString = String.format(", %5.2f, %5.2f\n", locationGPS.getLongitude(), locationGPS.getLatitude());
                                dataInPrint = dataInPrint + outputString;
                                byte[] data = dataInPrint.getBytes();//outputString.getBytes();
                                os.write(data);
                                os.close();

                            } catch (IOException e) {
                                // Unable to create file, likely because external storage is
                                // not currently mounted.
                                Log.w("ExternalStorage", "Error writing " + data_file, e);
                            }
                        }

                        /*if(showdata_en) {

                            message_sensordata = String.format("ACC (x, y, z): (%10.3f, %10.3f, %10.3f) m/s"+Html.fromHtml("<sup><small>2</small></sup>")+"\n"+
                                            "Gyro (x, y, z): (%10.3f, %10.3f, %10.3f) rad/s\n"+
                                            "Mag (x, y, z): (%10.3f, %10.3f, %10.3f) uT\n"+
                                            "Pressure: %10.3f hPa,  Lightness: %10.3f lx\n"+
                                            "Proximity: %2.0f cm, (Long., Lat.): (%5.2f, %5.2f)"+"\u00B0",
                                    motion_data.acc_x, motion_data.acc_y, motion_data.acc_z,
                                    motion_data.gyro_x, motion_data.gyro_y, motion_data.gyro_z,
                                    motion_data.mag_x, motion_data.mag_y, motion_data.mag_z,
                                    motion_data.pressure, motion_data.VIlight, motion_data.proximity, locationGPS.getLongitude(), locationGPS.getLatitude());

                            motion_view.setTextSize(14);

                            motion_view.setText(message_sensordata);//todayStateStr);

                        }*/

                        recDataString.delete(0, recDataString.length());                    //clear all string data
                    }
                }
            }
        };
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        checkBTState();

/*
        BT_sw = (Switch) findViewById(R.id.bluetooth_sw);
        BT_sw.setChecked(btAdapter.isEnabled());
        BT_sw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                                         boolean isChecked) {

                if (isChecked) {
                    if (!btAdapter.isEnabled()) {
                        Intent turnOn = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        startActivityForResult(turnOn, 0);
                        Toast.makeText(getApplicationContext(), "Bluetooth Turned on"
                                , Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(getApplicationContext(), "Bluetooth already on",
                                Toast.LENGTH_LONG).show();
                    }

                    init_msp430.setEnabled(true);
                } else {
                    btAdapter.disable();


                    Datalogger_found = false;
                    showdata_en = false;
                    showdata_sw.setChecked(showdata_en);
                    plot_en = false;
                    plot_sw.setChecked(plot_en);
                    init_msp430.setEnabled(false);
                    showdata_sw.setEnabled(false);
                    plot_sw.setEnabled(false);


                    Toast.makeText(getApplicationContext(), "Bluetooth Turned off",
                            Toast.LENGTH_LONG).show();
                }

            }
        });
        */
/*
        init_msp430 = (Button) findViewById(R.id..button_sendInitialize);
        init_msp430.setEnabled(btAdapter.isEnabled());
        // add button listener
        init_msp430.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {

                if (!btAdapter.isEnabled()) {
                    Toast.makeText(getApplicationContext(), "Bluetooth is off!!!",
                            Toast.LENGTH_SHORT).show();

                    return;
                }
                // get prompts.xml view
                LayoutInflater li = LayoutInflater.from(context_this);
                View promptsView = li.inflate(R.layout.prompt_init_msp430, null);

                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context_this);

                // set prompts.xml to alertdialog builder
                alertDialogBuilder.setView(promptsView);

                //final EditText userInput = (EditText) promptsView
                //       .findViewById(R.id.editTextDialogUserInput);

                // set dialog message
                alertDialogBuilder
                        .setCancelable(false)
                        .setPositiveButton("OK",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {

                                        // get settings
                                        //initialization_data = (char) ((((int) motion_on) << 2) + (((int) baro_on) << 1) + (((int) optical_on)));

                                        // get paired devices
                                        pairedDevices = btAdapter.getBondedDevices();

                                        for (BluetoothDevice bt : pairedDevices) {

                                            String paired_bt_name = bt.getName();
                                            if (paired_bt_name.equals(Data_logger_name)) {
                                                Datalogger_found = true;
                                                paired_result = bt;
                                                bt_address = bt.getAddress();

                                                showdata_sw.setEnabled(true);
                                                plot_sw.setEnabled(true);

                                                break;
                                            }

                                        }

                                        if(Datalogger_found) {
                                            //connect.invoke(proxy, paired_result);

                                            //Get MAC address from DeviceListActivity via intent
                                            //Intent intent = getIntent();

                                            //Get the MAC address from the DeviceListActivty via EXTRA
                                            //address = intent.getStringExtra(DeviceListActivity.EXTRA_DEVICE_ADDRESS);

                                            //create device and set the MAC address
                                            BluetoothDevice device = btAdapter.getRemoteDevice(bt_address);

                                            try {
                                                btSocket = createBluetoothSocket(device);
                                            } catch (IOException e) {
                                                Toast.makeText(getBaseContext(), "Socket creation failed", Toast.LENGTH_LONG).show();
                                            }
                                            // Establish the Bluetooth socket connection.
                                            try
                                            {
                                                btSocket.connect();
                                            } catch (IOException e) {
                                                try
                                                {
                                                    btSocket.close();
                                                } catch (IOException e2)
                                                {
                                                    //insert code to deal with this
                                                }
                                            }
                                            mConnectedThread = new ConnectedThread(btSocket);
                                            mConnectedThread.start();

                                            //send initial data to msp430;
                                            data_logger_init();


                                            //start to record data
                                            Toast.makeText(getApplicationContext(), "Recording started (file saved in .../documents/msp430_sensor_xxxx.txt)"
                                                    , Toast.LENGTH_LONG).show();
                                        }
                                        else
                                        {
                                            //start to record data
                                            Toast.makeText(getApplicationContext(), "Data_Logger_MSP430 was not found!"
                                                    , Toast.LENGTH_LONG).show();
                                        }

                                    }
                                })
                        .setNegativeButton("Cancel",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        dialog.cancel();
                                    }
                                });

                // create alert dialog
                AlertDialog alertDialog = alertDialogBuilder.create();

                // show it
                alertDialog.show();

            }
        });*/

        showdata_sw = (Switch) findViewById(R.id.showdata_msp_sw);
        showdata_sw.setChecked(showdata_en);
        showdata_sw.setEnabled(false);
        showdata_sw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                                         boolean isChecked) {

                if (isChecked) {

//                    if (Datalogger_found) {
                        showdata_en = true;
//                    } else {
//                        showdata_en = false;
//                        //showdata_sw.setChecked(showdata_en);
//
//
//
//                        Toast.makeText(getApplicationContext(), "Data_Logger is not connected or initialized!"
//                                , Toast.LENGTH_SHORT).show();
//                    }

                } else {
                    motion_view.setText(null);
                    showdata_en = false;
                }

            }
        });

        plot_sw = (Switch) findViewById(R.id.plotdata_msp_sw);
        plot_sw.setChecked(plot_en);
        plot_sw.setEnabled(false);
        plot_sw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {


            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                                         boolean isChecked) {

                if (isChecked) {

                    /////select data to plot
                    showDialog();


                } else {
                    plot_en = false;

                    chartContainer.removeAllViews();

                    /*Toast.makeText(getApplicationContext(), "Background Recording..."
                            , Toast.LENGTH_SHORT).show();*/
                }
            }

        });

        /*Handler handler =new Handler();
        final Runnable r = new Runnable() {
            public void run() {
                handler.postDelayed(this, 30000);
                String mydate = java.text.DateFormat.getDateTimeInstance().format(Calendar.getInstance().getTime());
                motion_view.setText(mydate);
            }
        };
        handler.postDelayed(r, 0000);*/


        Thread t_updateData = new Thread() {

            @Override
            public void run() {
                try {
                    while (!isInterrupted()) {
                        Thread.sleep(100);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                long time_millis= System.currentTimeMillis();

                                if(validdata_recvd){
                                    motion_data.acc_x = Integer.parseInt(valid_parts[0]);
                                    motion_data.acc_y = Integer.parseInt(valid_parts[1]);
                                    motion_data.acc_z = Integer.parseInt(valid_parts[2]);
                                    motion_data.gyro_x = Integer.parseInt(valid_parts[3])/10.0f;
                                    motion_data.gyro_y = Integer.parseInt(valid_parts[4])/10.0f;
                                    motion_data.gyro_z = Integer.parseInt(valid_parts[5])/10.0f;
                                    motion_data.mag_x = Integer.parseInt(valid_parts[6])/10.0f;
                                    motion_data.mag_y = Integer.parseInt(valid_parts[7])/10.0f;
                                    motion_data.mag_z = Integer.parseInt(valid_parts[8])/10.0f;
                                    motion_data.hmag_x = Integer.parseInt(valid_parts[9]);
                                    motion_data.hmag_y = Integer.parseInt(valid_parts[10]);
                                    motion_data.hmag_z = Integer.parseInt(valid_parts[11]);
                                    motion_data.temp = Integer.parseInt(valid_parts[12])/100.0f;
                                    motion_data.pressure = Integer.parseInt(valid_parts[13])/10.0f;
                                    motion_data.humidity = Integer.parseInt(valid_parts[14]);
                                    motion_data.VIlight = Integer.parseInt(valid_parts[15]);
                                    motion_data.IRlight = Integer.parseInt(valid_parts[16]);
                                    motion_data.uv_index = Integer.parseInt(valid_parts[17]);
                                    motion_data.proximity = Integer.parseInt(valid_parts[18]);
                                    motion_data.time_ms = Integer.parseInt(valid_parts[19]);
                                }


                                if(showdata_en) {

                                    message_sensordata = String.format(
                                            "ACC (x, y, z): (%6.0f, %6.0f, %6.0f)mg\n"+
                                                    "Gyro (x, y, z): (%6.1f, %6.1f, %6.1f)rad/s\n"+
                                                    "Mag (x, y, z): (%6.1f, %6.1f, %6.1f)uT\n"+
                                                    "LIS_Mag (x, y, z): (%6.0f, %6.0f, %6.0f)mGauss\n"+
                                                    "(T, P, H): (%3.1f"+"\u2103"+", %4.1f hPa, %2.0f%%)\n" +
                                                    "(V, IR, UV): (%5.0f, %5.0f, %2.0f)\n"+
                                                    "Proximity: %4.0f,    (Long., Lat.): (%5.2f, %5.2f)"+"\u00B0",

                                            motion_data.acc_x, motion_data.acc_y, motion_data.acc_z,
                                            motion_data.gyro_x, motion_data.gyro_y, motion_data.gyro_z,
                                            motion_data.mag_x, motion_data.mag_y, motion_data.mag_z,
                                            motion_data.hmag_x, motion_data.hmag_y,motion_data.hmag_z,
                                            motion_data.temp, motion_data.pressure, motion_data.humidity,
                                            motion_data.VIlight, motion_data.IRlight, motion_data.uv_index,
                                            motion_data.proximity, locationGPS.getLongitude(), locationGPS.getLatitude());

                                    motion_view.setTextSize(14);

                                    motion_view.setText(message_sensordata);//todayStateStr);

                                }

                                if(plot_en){
                                    //createChart();

                                    //sample points to plot
                                    //if((time_millis-t_record)>50) {

                                        AccelemoterData data=null;

                                        if(data_choice==0) {
                                            data = new AccelemoterData(time_millis, motion_data.acc_x, motion_data.acc_y, motion_data.acc_z);
                                        }else if(data_choice==1) {
                                            data = new AccelemoterData(time_millis, motion_data.gyro_x, motion_data.gyro_y, motion_data.gyro_z);
                                        }else if(data_choice==2) {
                                            data = new AccelemoterData(time_millis, motion_data.mag_x, motion_data.mag_y, motion_data.mag_z);
                                        }else if(data_choice==3) {
                                            data = new AccelemoterData(time_millis, motion_data.hmag_x, motion_data.hmag_y, motion_data.hmag_z);
                                        }else if(data_choice==4) {
                                            data = new AccelemoterData(time_millis, motion_data.temp, 0, 0);//ignore Y and Z
                                        }else if(data_choice==5) {
                                            data = new AccelemoterData(time_millis, motion_data.pressure, 0, 0);//ignore Y and Z
                                        }else if(data_choice==6) {
                                            data = new AccelemoterData(time_millis, motion_data.VIlight, motion_data.IRlight, 0);//ignore Y and Z
                                        }else if(data_choice==7) {
                                            data = new AccelemoterData(time_millis, motion_data.proximity, 0, 0);//ignore Y and Z
                                        }


                                        //only display latest date
                                        line.addNewData(data, t_start);
                                        chartview.repaint();

                                        //t_record = time_millis;
                                    //}
                                }

                            }
                        });
                    }
                } catch (InterruptedException e) {
                }
            }
        };

        t_updateData.start();




        /*FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });*/
    }


    public static String byteArrayToHex(byte[] a) {
        StringBuilder sb = new StringBuilder(a.length * 2);
        for(byte b: a)
            sb.append(String.format("%02x", b & 0xff));
        return sb.toString();
    }

/*
    public void BT_on(View view){
        if (!BA.isEnabled()) {
            Intent turnOn = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(turnOn, 0);
            Toast.makeText(getApplicationContext(), "Bluetooth Turned on"
                    , Toast.LENGTH_LONG).show();
        }
        else{
            Toast.makeText(getApplicationContext(),"Bluetooth already on",
                    Toast.LENGTH_LONG).show();
        }
    }
*/


    /*---------- Listener class to get coordinates ------------- */
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
    }

    public void onCheckboxClicked(View view) {
        // Is the view now checked?
        boolean checked = ((CheckBox) view).isChecked();

        // Check which checkbox was clicked
        switch(view.getId()) {
            case R.id.checkbox_motionsensor:
                if (checked)
                    motion_on = 1;
                else
                    motion_on = 0;
                break;
            case R.id.checkbox_barometer:
                if (checked)
                    baro_on = 1;
                else
                    baro_on = 0;
                break;
            case R.id.checkbox_optical:
                if (checked)
                    optical_on = 1;
                else
                    optical_on = 0;
                break;
            case R.id.checkbox_LIS_Mag:
                if (checked)
                    LIS_on = 1;
                else
                    LIS_on = 0;
                break;
            default:
                motion_on = 0;
                baro_on = 0;
                optical_on = 0;
                LIS_on = 0;
                break;
        }
    }

    public void data_logger_init(){

/*        if (!btAdapter.isEnabled()) {
            Toast.makeText(getApplicationContext(), "Bluetooth is off!",
                    Toast.LENGTH_LONG).show();

            return;
        }*/

        //send initial data
        initialization_data = (char)(((int)LIS_on<<3)|((int)motion_on<<2)|((int)baro_on<<1)|(int)optical_on);
        String initalD = String.format("%s", initialization_data);
        mConnectedThread.write(initalD);    // Send "0" via B

    }


/*     public void BT_list(View view) {
        if (!BA.isEnabled()) {
            Toast.makeText(getApplicationContext(), "Bluetooth is off!",
                    Toast.LENGTH_LONG).show();

            return;
        }

       pairedDevices = BA.getBondedDevices();

        ArrayList list = new ArrayList();
        for (BluetoothDevice bt : pairedDevices) {

            String paired_bt_name = bt.getName();
            if(paired_bt_name.equals(Data_logger_name))
                Datalogger_found = 1;

            list.add(paired_bt_name);

        }

        Toast.makeText(getApplicationContext(), "Showing Paired Devices",
                Toast.LENGTH_SHORT).show();
        final ArrayAdapter adapter = new ArrayAdapter
                (this, android.R.layout.simple_list_item_1, list);
        listview.setAdapter(adapter);

    }*/

/*
    public void BT_off(){
        btAdapter.disable();
        btSocket.close();
        Toast.makeText(getApplicationContext(),"Bluetooth Turned off" ,
                Toast.LENGTH_LONG).show();
    }

*/




    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_data_logger_msp430, menu);
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

            case R.id.action_Init_MSP430:

                // get prompts.xml view
                LayoutInflater li = LayoutInflater.from(context_this);
                View promptsView = li.inflate(R.layout.prompt_init_msp430, null);

                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context_this);

                // set prompts.xml to alertdialog builder
                alertDialogBuilder.setView(promptsView);

                //final EditText userInput = (EditText) promptsView
                //       .findViewById(R.id.editTextDialogUserInput);

                // set dialog message
                alertDialogBuilder
                        .setCancelable(false)
                        .setPositiveButton("OK",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {

                                        // get settings
                                        //initialization_data = (char) ((((int) motion_on) << 2) + (((int) baro_on) << 1) + (((int) optical_on)));

                                        // get paired devices
                                        pairedDevices = btAdapter.getBondedDevices();

                                        for (BluetoothDevice bt : pairedDevices) {

                                            String paired_bt_name = bt.getName();
                                            if (paired_bt_name.equals(Data_logger_name)) {
                                                Datalogger_found = true;
                                                paired_result = bt;
                                                bt_address = bt.getAddress();

                                                showdata_sw.setEnabled(true);
                                                plot_sw.setEnabled(true);

                                                break;
                                            }

                                        }

                                        if(Datalogger_found) {
                                            //connect.invoke(proxy, paired_result);

                                            //Get MAC address from DeviceListActivity via intent
                                            //Intent intent = getIntent();

                                            //Get the MAC address from the DeviceListActivty via EXTRA
                                            //address = intent.getStringExtra(DeviceListActivity.EXTRA_DEVICE_ADDRESS);

                                            //create device and set the MAC address
                                            BluetoothDevice device = btAdapter.getRemoteDevice(bt_address);

                                            try {
                                                btSocket = createBluetoothSocket(device);
                                            } catch (IOException e) {
                                                Toast.makeText(getBaseContext(), "Socket creation failed", Toast.LENGTH_LONG).show();
                                            }
                                            // Establish the Bluetooth socket connection.
                                            try
                                            {
                                                btSocket.connect();
                                            } catch (IOException e) {
                                                try
                                                {
                                                    btSocket.close();
                                                } catch (IOException e2)
                                                {
                                                    //insert code to deal with this
                                                }
                                            }
                                            mConnectedThread = new ConnectedThread(btSocket);
                                            mConnectedThread.start();

                                            //send initial data to msp430;
                                            data_logger_init();


                                            //start to record data
                                            Toast.makeText(getApplicationContext(), "Recording started (file saved in .../documents/msp430_sensor_xxxx.txt)"
                                                    , Toast.LENGTH_LONG).show();
                                        }
                                        else
                                        {
                                            //start to record data
                                            Toast.makeText(getApplicationContext(), "Data_Logger_MSP430 was not found!"
                                                    , Toast.LENGTH_LONG).show();
                                        }

                                    }
                                })
                        .setNegativeButton("Cancel",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        dialog.cancel();
                                    }
                                });

                // create alert dialog
                AlertDialog alertDialog = alertDialogBuilder.create();

                // show it
                alertDialog.show();



            default:
                return super.onOptionsItemSelected(item);
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
                            Toast.makeText(context, "Selected LIS Mag data", Toast.LENGTH_LONG).show();
                        }else if(data_choice==4) {
                            Toast.makeText(context, "Selected Temperature data", Toast.LENGTH_LONG).show();
                        }else if(data_choice==5) {
                            Toast.makeText(context, "Selected Pressure data", Toast.LENGTH_LONG).show();
                        }else if(data_choice==6) {
                            Toast.makeText(context, "Selected ViLight-IRLight data", Toast.LENGTH_LONG).show();
                        }else if(data_choice==7) {
                            Toast.makeText(context, "Selected Proximity data", Toast.LENGTH_LONG).show();
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
                            chartTitleString = "Accelerometer (mg)";
                            Yaxis_max = 1500;
                            Yaxis_min = -Yaxis_max;
                        }else if(data_choice==1) {
                            chartTitleString = "Gyro (rad/s)";
                            Yaxis_max = 100;
                            Yaxis_min = -Yaxis_max;
                        }else if(data_choice==2) {
                            chartTitleString = "Magnetometer (uT)";
                            Yaxis_max = 100;
                            Yaxis_min = -Yaxis_max;
                        }else if(data_choice==3) {
                            chartTitleString = "High Precision Magnetometer (mGauss)";
                            Yaxis_max = 300;
                            Yaxis_min = -Yaxis_max;
                        }else if(data_choice==4) {
                            chartTitleString = "Temperature(Celcius)";
                            Yaxis_max = 35;
                            Yaxis_min = 10;
                        }else if(data_choice==5) {
                            chartTitleString = "Proximity(hPa)";
                            Yaxis_max = 1000;
                            Yaxis_min = 950;
                        }else if(data_choice==6) {
                            chartTitleString = "ViLight-IRLight";
                            Yaxis_max = 500;
                            Yaxis_min = 200;
                        }else if(data_choice==7) {
                            chartTitleString = "Proximity";
                            Yaxis_max = 1000;
                            Yaxis_min = 500;
                        }

                        line = new LineChart();


                        new ArrayList<AccelemoterData>();
                        chartview = ChartFactory.getLineChartView(Data_Logger.context, line.dataset, line.multiRenderer);
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


    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {

        return  device.createRfcommSocketToServiceRecord(BTMODULEUUID);
        //creates secure outgoing connecetion with BT device using UUID
    }

    //Checks that the Android device Bluetooth is available and prompts to be turned on if off
    private void checkBTState() {

        if(btAdapter==null) {
            Toast.makeText(getBaseContext(), "Device does not support bluetooth", Toast.LENGTH_LONG).show();
        } else {
            if (btAdapter.isEnabled()) {
            } else {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, 1);
            }
        }
    }

    //create new class for connect thread
    private class ConnectedThread extends Thread {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        //creation of the connect thread
        public ConnectedThread(BluetoothSocket socket) {
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                //Create I/O streams for connection
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[256];
            int bytes;

            // Keep looping to listen for received messages
            while (true) {
                try {
                    bytes = mmInStream.read(buffer);            //read bytes from input buffer
                    String readMessage = new String(buffer, 0, bytes);
                    // Send the obtained bytes to the UI Activity via handler
                    bluetoothIn.obtainMessage(handlerState, bytes, -1, readMessage).sendToTarget();
                } catch (IOException e) {
                    break;
                }
            }
        }
        //write method
        public void write(String input) {
            byte[] msgBuffer = input.getBytes();           //converts entered String into bytes
            try {
                mmOutStream.write(msgBuffer);                //write bytes over BT connection via outstream
            } catch (IOException e) {
                //if you cannot write, close the application
                Toast.makeText(getBaseContext(), "Connection Failure", Toast.LENGTH_LONG).show();
                finish();

            }
        }
    }
}