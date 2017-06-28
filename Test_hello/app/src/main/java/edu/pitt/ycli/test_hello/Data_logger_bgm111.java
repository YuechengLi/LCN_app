package edu.pitt.ycli.test_hello;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothSocket;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class Data_logger_bgm111 extends AppCompatActivity{//} implements BluetoothAdapter.LeScanCallback{

    public class Sensor_data {
        float[] acc = new float[3];
        float[] gyro = new float[3];
        float[] mag = new float[3];
        float temperature;
        long time_ms;

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
    private SparseArray<BluetoothDevice> mDevices;
    private BluetoothGatt mGatt;
    private static final String TAG = "BluetoothGattActivity";
    private BluetoothLeScanner mLEScanner;
    private ScanSettings settings;
    private List<ScanFilter> filters;
    private static final String DEVICE_NAME = "Blue Gecko BGM111";
    private int num_blue_gecko_device = 0;

    //BGM111 Service {00001803-0000-1000-8000-00805f9b34fb, 00001802-0000-1000-8000-00805f9b34fb, 00001804-0000-1000-8000-00805f9b34fb, 00001905-0000-1000-8000-00805f9b34fb, 00001809-0000-1000-8000-00805f9b34fb
    private static final UUID Health_thermometer_service = UUID.fromString("00001809-0000-1000-8000-00805f9b34fb");//
    private static final UUID Health_DATA_CHAR =  UUID.fromString("00002a1c-0000-1000-8000-00805f9b34fb");//
    private static final UUID Health_CONFIG_CHAR =  UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");//

    // Humidity Service
    private static final UUID HUMIDITY_SERVICE = UUID.fromString("f000aa20-0451-4000-b000-000000000000");
    private static final UUID HUMIDITY_DATA_CHAR = UUID.fromString("f000aa21-0451-4000-b000-000000000000");
    private static final UUID HUMIDITY_CONFIG_CHAR = UUID.fromString("f000aa22-0451-4000-b000-000000000000");
    /* Barometric Pressure Service */
    private static final UUID PRESSURE_SERVICE = UUID.fromString("f000aa40-0451-4000-b000-000000000000");
    private static final UUID PRESSURE_DATA_CHAR = UUID.fromString("f000aa41-0451-4000-b000-000000000000");
    private static final UUID PRESSURE_CONFIG_CHAR = UUID.fromString("f000aa42-0451-4000-b000-000000000000");
    private static final UUID PRESSURE_CAL_CHAR = UUID.fromString("f000aa43-0451-4000-b000-000000000000");
    /* Client Configuration Descriptor */
    private static final UUID CONFIG_DESCRIPTOR = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private ProgressDialog mProgress;


    //private BluetoothSocket btSocket = null;
    //private Set<BluetoothDevice> pairedDevices;
    //private StringBuilder recDataString = new StringBuilder();
    //private ConnectedThread mConnectedThread;
    // SPP UUID service - this should work for most devices
    private static final UUID BTMODULEUUID = UUID.fromString("00002A29-0000-1000-8000-00805F9B34FB");
    // String for MAC address
    //private static String bt_address;
    //private String readMessage;
    private boolean validdata_recvd = false;



    private ListView listview;
    //private Switch BT_sw;
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
    private long counter;

    private CharSequence[] select_items = null;
    private Resources res = null;
    private AlertDialog alert;
    private int data_choice=0;


    //pre-checked!!!
    private char motion_on=1;
    private char baro_on=1;
    private char optical_on=1;

    public StringBuilder sensorDataStr;

    private int endOfLineIndex;
    private int index_start;//
    private String dataInPrint;    //
    private String [] valid_parts;
    private char initialization_data;
    private String sensor_raw;
    private Sensor_data motion_data = new Sensor_data();
    private static final int size_buffer = 3000;
    private Sensor_data [] motion_data_buffer = new Sensor_data[size_buffer];
    private int num_data_in_buffer = 0;
    private boolean connection_ready = false;

    public String filename = "bluegecko_sensor_data";
    public File dir_path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
    SimpleDateFormat formatter = new SimpleDateFormat("_yyyy_MM_dd_HH_mm");
    Date now = new Date();
    String fileName_formatted = filename + formatter.format(now) + ".txt";
    public File data_file;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_logger_bgm111);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Intent intent = getIntent();

        context_this = this;

        Data_logger_bgm111.context = getApplicationContext();

        t_record = 0;
        counter = 0;

        motion_view = (TextView)findViewById(R.id.Data_view_bgm111);

        res = getResources();
        select_items = res.getStringArray(R.array.data_options_bgm111);
        chartContainer = (LinearLayout) findViewById(R.id.chart_bmg111);


        //file operation
        dir_path.mkdirs();
        data_file = new File(dir_path, fileName_formatted);
        try {
            OutputStream os = new FileOutputStream(data_file, true);

            String outputString = "Sys_Time(ms), ACC_X (mg), ACC_Y, ACC_Z, GYRO_X (rad/s), GYRO_Y, GYRO_Z\n";
            byte[] data = outputString.getBytes();
            os.write(data);
            os.close();

        } catch (IOException e) {
            // Unable to create file, likely because external storage is
            // not currently mounted.
            Log.w("ExternalStorage", "Error writing " + data_file, e);
        }
        Toast.makeText(context, "File will be saved as .../documents/bluegecko_sensor_xxxx.txt"
                , Toast.LENGTH_LONG).show();

        //check BLE support
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "Sorry, BLE Not Supported !!!", Toast.LENGTH_SHORT).show();
            finish();
        }

        BluetoothManager manager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        btAdapter = manager.getAdapter();
        checkBTState();
        mDevices = new SparseArray<BluetoothDevice>();

        mProgress = new ProgressDialog(this);
        mProgress.setIndeterminate(true);
        mProgress.setCancelable(false);


        showdata_sw = (Switch) findViewById(R.id.showdata_bgm111_sw);
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

        plot_sw = (Switch) findViewById(R.id.plotdata_bgm111_sw);
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


        Thread t_updateData = new Thread() {

            @Override
            public void run() {
                try {
                    while (!isInterrupted()) {
                        Thread.sleep(30);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                long time_millis= System.currentTimeMillis();

                                counter++;

                                if(t_record !=motion_data.time_ms ) {


                                    if (validdata_recvd) {

                                        showdata_sw.setEnabled(true);
                                        plot_sw.setEnabled(true);

                                        String outputString = String.format("%d, %4.1f, %6.1f, %6.1f, %6.1f, %6.1f, %6.1f, %6.1f, %6.1f, %6.1f, %6.1f\n",
                                                motion_data.time_ms,
                                                motion_data.temperature,
                                                motion_data.acc[0],
                                                motion_data.acc[1],
                                                motion_data.acc[2],
                                                motion_data.gyro[0],
                                                motion_data.gyro[1],
                                                motion_data.gyro[2],
                                                motion_data.mag[0],
                                                motion_data.mag[1],
                                                motion_data.mag[2]
                                        );

                                        try {
                                            OutputStream os = new FileOutputStream(data_file, true);

                                            byte[] data = outputString.getBytes();
                                            os.write(data);
                                            os.close();

                                        } catch (IOException e) {
                                            // Unable to create file, likely because external storage is
                                            // not currently mounted.
                                            Log.w("ExternalStorage", "Error writing " + data_file, e);
                                        }
/*
                                    String outputString = String.format("%d, %4.1f, %6.1f, %6.1f, %6.1f, %6.1f, %6.1f, %6.1f\n",
                                            time_millis,//motion_data.time_ms,
                                            motion_data.temperature,
                                            motion_data.acc_x,
                                            motion_data.acc_y,
                                            motion_data.acc_z,
                                            motion_data.gyro_x,
                                            motion_data.gyro_y,
                                            motion_data.gyro_z
                                            );

                                    try {
                                        OutputStream os = new FileOutputStream(data_file, true);

                                        byte[] data = outputString.getBytes();
                                        os.write(data);
                                        os.close();

                                    } catch (IOException e) {
                                        // Unable to create file, likely because external storage is
                                        // not currently mounted.
                                        Log.w("ExternalStorage", "Error writing " + data_file, e);
                                    }*/

/*
                                    motion_data_buffer[num_data_in_buffer] = new Sensor_data();
                                    motion_data_buffer[num_data_in_buffer].time_ms=motion_data.time_ms;
                                    motion_data_buffer[num_data_in_buffer].acc_x=motion_data.acc_x;
                                    motion_data_buffer[num_data_in_buffer].acc_y=motion_data.acc_y;
                                    motion_data_buffer[num_data_in_buffer].acc_z=motion_data.acc_z;
                                    motion_data_buffer[num_data_in_buffer].gyro_x=motion_data.gyro_x;
                                    motion_data_buffer[num_data_in_buffer].gyro_y=motion_data.gyro_y;
                                    motion_data_buffer[num_data_in_buffer].gyro_z=motion_data.gyro_z;

                                    num_data_in_buffer++;
                                    if(num_data_in_buffer==size_buffer) {
                                        Toast.makeText(context, "Writing file...", Toast.LENGTH_SHORT).show();

                                        for(int i = 0;i<size_buffer;i++) {
                                            //save data
                                            String outputString = String.format("%d, %6d, %6d, %6d, %6.1f, %6.1f, %6.1f\n",
                                                    motion_data_buffer[i].time_ms,
                                                        motion_data_buffer[i].acc_x,
                                                        motion_data_buffer[i].acc_y,
                                                        motion_data_buffer[i].acc_z,
                                                        motion_data_buffer[i].gyro_x,
                                                        motion_data_buffer[i].gyro_y,
                                                        motion_data_buffer[i].gyro_z);

                                            try {
                                                OutputStream os = new FileOutputStream(data_file, true);

                                                byte[] data = outputString.getBytes();
                                                os.write(data);
                                                os.close();

                                            } catch (IOException e) {
                                                // Unable to create file, likely because external storage is
                                                // not currently mounted.
                                                Log.w("ExternalStorage", "Error writing " + data_file, e);
                                            }
                                        }

                                        num_data_in_buffer = 0;
                                        Toast.makeText(context, "Done!", Toast.LENGTH_SHORT).show();
                                    }*/
                                    }


                                    if (showdata_en) {

                                    /*message_sensordata = String.format(
                                            "ACC (x, y, z): (%6d, %6d, %6d)mg\n"+
                                                    "Gyro (x, y, z): (%6.1f, %6.1f, %6.1f)rad/s\n"+
                                                    "Mag (x, y, z): (%6.1f, %6.1f, %6.1f)uT\n"+
                                                    "%d (ms)",

                                            motion_data.acc_x, motion_data.acc_y, motion_data.acc_z,
                                            motion_data.gyro_x, motion_data.gyro_y, motion_data.gyro_z,
                                            motion_data.mag_x, motion_data.mag_y, motion_data.mag_z, motion_data.time_ms);*/

                                        message_sensordata = String.format("ACC (x, y, z): (%6.1f, %6.1f, %6.1f)mg\r\n" +
                                                        "GYRO (x, y, z): (%6.1f, %6.1f, %6.1f)rad/s\n" +
                                                        "MAG (x, y, z): (%6.1f, %6.1f, %6.1f)uT\n" +
                                                        "temperature: %4.1f ℃\n",
                                                motion_data.acc[0], motion_data.acc[1], motion_data.acc[2],
                                                motion_data.gyro[0], motion_data.gyro[1], motion_data.gyro[2],
                                                motion_data.mag[0], motion_data.mag[1], motion_data.mag[2],
                                                motion_data.temperature);
                                        motion_view.setTextSize(14);

                                        motion_view.setText(message_sensordata);//todayStateStr);

                                    }

                                    if (plot_en) {
                                        //createChart();

                                        AccelemoterData data = null;

                                        if (data_choice == 0) {
                                            data = new AccelemoterData(time_millis, motion_data.acc[0], motion_data.acc[1], motion_data.acc[2]);
                                        } else if (data_choice == 1) {
                                            data = new AccelemoterData(time_millis, motion_data.gyro[0], motion_data.gyro[1], motion_data.gyro[2]);//ignore Y and Z
                                        } else if (data_choice == 2) {
                                            data = new AccelemoterData(time_millis, motion_data.mag[0], motion_data.mag[1], motion_data.mag[2]);//ignore Y and Z
                                        } else if (data_choice == 3) {
                                            data = new AccelemoterData(time_millis, motion_data.temperature, 0, 0);//ignore Y and Z
                                        }


                                        //only display latest date
                                        line.addNewData(data, t_start);
                                        chartview.repaint();
                                    }


                                }

                                t_record = motion_data.time_ms;
                            }
                        });
                    }
                } catch (InterruptedException e) {
                }
            }
        };

        t_updateData.start();

/*        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });*/


    }

    @Override
    protected void onDestroy() {
        if (mGatt == null) {
            return;
        }
        mGatt.close();
        mGatt = null;
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_bgm111, menu);

        //Add any device elements we've discovered to the overflow menu
        for (int i=0; i < mDevices.size(); i++) {
            BluetoothDevice device = mDevices.valueAt(i);
            menu.add(0, mDevices.keyAt(i), 0, device.getName());
        }

        /*String bg_list = String.format("%s(%d)", DEVICE_NAME, num_blue_gecko_device);
        menu.add(0, mDevices.keyAt(0), 0, bg_list);*/

        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        switch (item.getItemId()) {

            case R.id.action_scan:
                mDevices.clear();
                scanLeDevice(true);
                return true;

            default:

                //Obtain the discovered device to connect with
                BluetoothDevice device = mDevices.get(item.getItemId());
                Log.i(TAG, "Connecting to "+device.getName());
                /*
                 * Make a connection with the device using the special LE-specific
                 * connectGatt() method, passing in a callback for GATT events
                 */
                mGatt = device.connectGatt(this, false, mGattCallback);
                mGatt.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH);
                scanLeDevice(false);// will stop after first device detection
                //Display progress UI
                mHandler.sendMessage(Message.obtain(null, MSG_PROGRESS, "Connecting to " + device.getName() + "..."));

                return super.onOptionsItemSelected(item);
        }

    }


    private Runnable mStopRunnable = new Runnable() {
        @Override
        public void run() {
            scanLeDevice(true);
        }
    };
    private Runnable mStartRunnable = new Runnable() {
        @Override
        public void run() {
            scanLeDevice(false);
        }
    };


    private void scanLeDevice(final boolean enable) {
        if (enable) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    num_blue_gecko_device = 0;
                    mLEScanner.stopScan(mScanCallback);
                }
            }, 5000);

            mLEScanner.startScan(filters, settings, mScanCallback);

        } else {

            mLEScanner.stopScan(mScanCallback);

        }
    }

    /*
    private LeDeviceListAdapter mLeDeviceListAdapter;

    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, int rssi,
                                     byte[] scanRecord) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mLeDeviceListAdapter.addDevice(device);
                            mLeDeviceListAdapter.notifyDataSetChanged();
                        }
                    });
                }
            };
*/

    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            Log.i("callbackType", String.valueOf(callbackType));
            Log.i("result", result.toString());
            BluetoothDevice btDevice = result.getDevice();

            String name = btDevice.getName();
            //if (DEVICE_NAME.equals(name)) {
                mDevices.put(btDevice.hashCode(), btDevice);

                num_blue_gecko_device++;
                //Update the overflow menu
                invalidateOptionsMenu();
            //}

            ////connectToDevice(btDevice);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            for (ScanResult sr : results) {
                Log.i("ScanResult - Results", sr.toString());
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.e("Scan Failed", "Error Code: " + errorCode);
        }
    };


    /*
     * In this callback, we've created a bit of a state machine to enforce that only
     * one characteristic be read or written at a time until all of our sensors
     * are enabled and we are registered to get notifications.
     */
    private BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {

        /* State Machine Tracking */
        private int mState = 0;

        private void reset() { mState = 3; } //0

        private void advance() { mState++; }

        /*
         * Send an enable command to each sensor by writing a configuration
         * characteristic.  This is specific to the SensorTag to keep power
         * low by disabling sensors you aren't using.
         */
//        private void enableNextSensor(BluetoothGatt gatt) {
//            BluetoothGattCharacteristic characteristic;
//            /*switch (mState) {
//                case 0:
//                    Log.d(TAG, "Enabling pressure cal");
//                    characteristic = gatt.getService(PRESSURE_SERVICE)
//                            .getCharacteristic(PRESSURE_CONFIG_CHAR);
//                    characteristic.setValue(new byte[] {0x02});
//                    break;
//                case 1:
//                    Log.d(TAG, "Enabling pressure");
//                    characteristic = gatt.getService(PRESSURE_SERVICE)
//                            .getCharacteristic(PRESSURE_CONFIG_CHAR);
//                    characteristic.setValue(new byte[] {0x01});
//                    break;
//                case 2:
//                    Log.d(TAG, "Enabling humidity");
//                    characteristic = gatt.getService(HUMIDITY_SERVICE)
//                            .getCharacteristic(HUMIDITY_CONFIG_CHAR);
//                    characteristic.setValue(new byte[] {0x01});
//                    break;
//                case 3:
//                    Log.d(TAG, "Enabling Health thermometer service");
//                    characteristic = gatt.getService(Health_thermometer_service)
//                            .getCharacteristic(Health_CONFIG_CHAR);
//                    characteristic.setValue(new byte[] {0x03});
//                    break;
//                default:
//                    mHandler.sendEmptyMessage(MSG_DISMISS);
//                    Log.i(TAG, "All Sensors Enabled");
//                    return;
//            }*/
//            Log.d(TAG, "Enabling Health thermometer service");
//            characteristic = gatt.getServices().get(2).getCharacteristics().get
//                    (0);//gatt.getService(Health_thermometer_service)
//                    //.getCharacteristic(Health_CONFIG_CHAR);
//            characteristic.setValue(new byte[] {0x03});
//
//            gatt.writeCharacteristic(characteristic);
//
//            connection_ready = true;
//        }



        /*
         * Read the data characteristic's value for each sensor explicitly
         */
//        private void readNextSensor(BluetoothGatt gatt) {
//            BluetoothGattCharacteristic characteristic;
//            switch (mState) {
//                case 0:
//                    Log.d(TAG, "Reading pressure cal");
//                    characteristic = gatt.getService(PRESSURE_SERVICE)
//                            .getCharacteristic(PRESSURE_CAL_CHAR);
//                    break;
//                case 1:
//                    Log.d(TAG, "Reading pressure");
//                    characteristic = gatt.getService(PRESSURE_SERVICE)
//                            .getCharacteristic(PRESSURE_DATA_CHAR);
//                    break;
//                case 2:
//                    Log.d(TAG, "Reading humidity");
//                    characteristic = gatt.getService(HUMIDITY_SERVICE)
//                            .getCharacteristic(HUMIDITY_DATA_CHAR);
//                    break;
//                case 3:
//                    Log.d(TAG, "Reading health");
//                    characteristic = gatt.getService(Health_thermometer_service)
//                            .getCharacteristic(Health_DATA_CHAR);
//                    break;
//                default:
//                    mHandler.sendEmptyMessage(MSG_DISMISS);
//                    Log.i(TAG, "All Sensors Enabled");
//                    return;
//            }
//
//            gatt.readCharacteristic(characteristic);
//        }


        /*
         * Enable notification of changes on the data characteristic for each sensor
         * by writing the ENABLE_NOTIFICATION_VALUE flag to that characteristic's
         * configuration descriptor.
         */
//        private void setNotifyNextSensor(BluetoothGatt gatt) {
//            BluetoothGattCharacteristic characteristic;
//            switch (mState) {
//                case 0:
//                    Log.d(TAG, "Set notify pressure cal");
//                    characteristic = gatt.getService(PRESSURE_SERVICE)
//                            .getCharacteristic(PRESSURE_CAL_CHAR);
//                    break;
//                case 1:
//                    Log.d(TAG, "Set notify pressure");
//                    characteristic = gatt.getService(PRESSURE_SERVICE)
//                            .getCharacteristic(PRESSURE_DATA_CHAR);
//                    break;
//                case 2:
//                    Log.d(TAG, "Set notify humidity");
//                    characteristic = gatt.getService(HUMIDITY_SERVICE)
//                            .getCharacteristic(HUMIDITY_DATA_CHAR);
//                    break;
//                case 3:
//                    Log.d(TAG, "Set notify health");
//                    characteristic = gatt.getService(Health_thermometer_service)
//                            .getCharacteristic(Health_DATA_CHAR);
//                    break;
//                default:
//                    mHandler.sendEmptyMessage(MSG_DISMISS);
//                    Log.i(TAG, "All Sensors Enabled");
//                    return;
//            }
//
////            Log.d(TAG, "Set notify health");
////            characteristic = gatt.getService(Health_thermometer_service)
////                    .getCharacteristic(Health_DATA_CHAR);
//
//            //Enable local notifications
//            gatt.setCharacteristicNotification(characteristic, true);
//            //Enabled remote notifications
//            BluetoothGattDescriptor desc = characteristic.getDescriptor(CONFIG_DESCRIPTOR);
//            desc.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
//            gatt.writeDescriptor(desc);
//        }

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.d(TAG, "Connection State Change: " + status + " -> " + connectionState(newState));
            if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED) {
                /*
                 * Once successfully connected, we must next discover all the services on the
                 * device before we can read and write their characteristics.
                 */
                gatt.discoverServices();
                mHandler.sendMessage(Message.obtain(null, MSG_PROGRESS, "Discovering Services..."));
            } else if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_DISCONNECTED) {
                /*
                 * If at any point we disconnect, send a message to clear the weather values
                 * out of the UI
                 */
                mHandler.sendEmptyMessage(MSG_CLEAR);
            } else if (status != BluetoothGatt.GATT_SUCCESS) {
                /*
                 * If there is a failure at any stage, simply disconnect
                 */
                gatt.disconnect();
            }
        }


        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            mGatt = gatt;
            List<BluetoothGattService> services = gatt.getServices();
            Log.d(TAG, "Services Discovered: " + services.toString());
            mHandler.sendMessage(Message.obtain(null, MSG_PROGRESS, "Enabling Sensors..."));
            /*
             * With services discovered, we are going to reset our state machine and start
             * working through the sensors we need to enable
             */
            //reset();
            //enableNextSensor(gatt);
            //gatt.readCharacteristic(services.get(6).getCharacteristics().get
            //        (0));


            //BluetoothGattCharacteristic therm_char = services.get(2).getCharacteristics().get(0);
            BluetoothGattCharacteristic therm_char = services.get(0).getCharacteristics().get(3);//keyfob, bo

            mGatt.setCharacteristicNotification(therm_char, true);
            for (BluetoothGattDescriptor descriptor : therm_char.getDescriptors()) {
                descriptor.setValue( BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);//ENABLE_NOTIFICATION_VALUE);//
                mGatt.writeDescriptor(descriptor);
            }

            mHandler.sendEmptyMessage(MSG_DISMISS);


        }


        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            //Once notifications are enabled, we move to the next sensor and start over with enable
            //advance();
            //enableNextSensor(gatt);
        }

 /*       @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {

            //After reading the initial value, next we enable notifications
            //setNotifyNextSensor(gatt);

            Log.i("CharacteristicUUID", characteristic.getUuid().toString());

            mHandler.sendMessage(Message.obtain(null, MSG_HEALTH, characteristic));
            mHandler.sendEmptyMessage(MSG_DISMISS);
            Log.i(TAG, "Health read");
        }*/

        /*
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            //After writing the enable flag, next we read the initial value
            readNextSensor(gatt);
        }*/

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            /*
             * After notifications are enabled, all updates from the device on characteristic
             * value changes will be posted here.  Similar to read, we hand these up to the
             * UI thread to update the display.
             */
            /*if (HUMIDITY_DATA_CHAR.equals(characteristic.getUuid())) {
                mHandler.sendMessage(Message.obtain(null, MSG_HUMIDITY, characteristic));
            }
            if (PRESSURE_DATA_CHAR.equals(characteristic.getUuid())) {
                mHandler.sendMessage(Message.obtain(null, MSG_PRESSURE, characteristic));
            }
            if (PRESSURE_CAL_CHAR.equals(characteristic.getUuid())) {
                mHandler.sendMessage(Message.obtain(null, MSG_PRESSURE_CAL, characteristic));
            }
            if (Health_DATA_CHAR.equals(characteristic.getUuid())) {
                mHandler.sendMessage(Message.obtain(null, MSG_HEALTH, characteristic));
            }*/

            //Log.d(TAG,"Time:" + System.currentTimeMillis());


            updateMotionValues(characteristic);
            validdata_recvd = true;

            //mHandler.sendMessage(Message.obtain(null, MSG_HEALTH, characteristic));
        }


        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            Log.d(TAG, "Remote RSSI: "+rssi);
        }

        private String connectionState(int status) {
            switch (status) {
                case BluetoothProfile.STATE_CONNECTED:
                    return "Connected";
                case BluetoothProfile.STATE_DISCONNECTED:
                    return "Disconnected";
                case BluetoothProfile.STATE_CONNECTING:
                    return "Connecting";
                case BluetoothProfile.STATE_DISCONNECTING:
                    return "Disconnecting";
                default:
                    return String.valueOf(status);
            }
        }
    };

    /*
     * We have a Handler to process event results on the main thread
     */
    private static final int MSG_HEALTH = 104;
    private static final int MSG_HUMIDITY = 101;
    private static final int MSG_PRESSURE = 102;
    private static final int MSG_PRESSURE_CAL = 103;
    private static final int MSG_PROGRESS = 201;
    private static final int MSG_DISMISS = 202;
    private static final int MSG_CLEAR = 301;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            BluetoothGattCharacteristic characteristic;
            switch (msg.what) {
                case MSG_HEALTH:
                    characteristic = (BluetoothGattCharacteristic) msg.obj;
                    if (characteristic.getValue() == null) {
                        Log.w(TAG, "Error obtaining health value");
                        return;
                    }
                    updateMotionValues(characteristic);

                    break;
/*                case MSG_HUMIDITY:
                    characteristic = (BluetoothGattCharacteristic) msg.obj;
                    if (characteristic.getValue() == null) {
                        Log.w(TAG, "Error obtaining humidity value");
                        return;
                    }
                    updateHumidityValues(characteristic);
                    break;
                case MSG_PRESSURE:
                    characteristic = (BluetoothGattCharacteristic) msg.obj;
                    if (characteristic.getValue() == null) {
                        Log.w(TAG, "Error obtaining pressure value");
                        return;
                    }
                    updatePressureValue(characteristic);
                    break;
                case MSG_PRESSURE_CAL:
                    characteristic = (BluetoothGattCharacteristic) msg.obj;
                    if (characteristic.getValue() == null) {
                        Log.w(TAG, "Error obtaining cal value");
                        return;
                    }
                    updatePressureCals(characteristic);
                    break;*/
                case MSG_PROGRESS:
                    mProgress.setMessage((String) msg.obj);
                    if (!mProgress.isShowing()) {
                        mProgress.show();
                    }
                    break;
                case MSG_DISMISS:
                    mProgress.hide();
                    break;
                case MSG_CLEAR:
                    //clearDisplayValues();
                    break;
            }

        }
    };

    /* Methods to extract sensor data and update the UI */

    private void updateMotionValues(BluetoothGattCharacteristic characteristic) {
        SensorTagData.extractMotion(characteristic, 0);
        motion_data.temperature = SensorTagData.get_Motion_temp();//
        motion_data.acc = SensorTagData.get_Motion_acc();//SensorTagData.extractMotion_acc(characteristic, 1);
        motion_data.gyro = SensorTagData.get_Motion_gyro();//SensorTagData.extractMotion_gyro(characteristic, 9);
        motion_data.mag = SensorTagData.get_Motion_mag();
        motion_data.time_ms = 0xFFFFFFFF&(System.currentTimeMillis());


//        String outputString = String.format("%d, %4.1f, %6.1f, %6.1f, %6.1f, %6.1f, %6.1f, %6.1f, %6.1f, %6.1f, %6.1f\n",
//                motion_data.time_ms,
//                motion_data.temperature,
//                motion_data.acc[0],
//                motion_data.acc[1],
//                motion_data.acc[2],
//                motion_data.gyro[0],
//                motion_data.gyro[1],
//                motion_data.gyro[2],
//                motion_data.mag[0],
//                motion_data.mag[1],
//                motion_data.mag[2]
//        );
//
//        try {
//            OutputStream os = new FileOutputStream(data_file, true);
//
//            byte[] data = outputString.getBytes();
//            os.write(data);
//            os.close();
//
//        } catch (IOException e) {
//            // Unable to create file, likely because external storage is
//            // not currently mounted.
//            Log.w("ExternalStorage", "Error writing " + data_file, e);
//        }

        Log.d(TAG, "BGM111 ACC: "+motion_data.time_ms + " " + motion_data.mag[0] + " " + motion_data.mag[1] + " " + motion_data.mag[2]);
    }

    private void updateHumidityValues(BluetoothGattCharacteristic characteristic) {
        double humidity = SensorTagData.extractHumidity(characteristic);

        //mHumidity.setText(String.format("%.0f%%", humidity));
    }

    private int[] mPressureCals;
    private void updatePressureCals(BluetoothGattCharacteristic characteristic) {
        mPressureCals = SensorTagData.extractCalibrationCoefficients(characteristic);
    }

    private void updatePressureValue(BluetoothGattCharacteristic characteristic) {
        if (mPressureCals == null) return;
        double pressure = SensorTagData.extractBarometer(characteristic, mPressureCals);
        double temp = SensorTagData.extractBarTemperature(characteristic, mPressureCals);

        //mTemperature.setText(String.format("%.1f\u00B0C", temp));
        //mPressure.setText(String.format("%.2f", pressure));
    }



//
//    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
//
//        return  device.createRfcommSocketToServiceRecord(BTMODULEUUID);
//        //creates secure outgoing connecetion with BT device using UUID
//    }

    //Checks that the Android device Bluetooth is available and prompts to be turned on if off
    private void checkBTState() {

        if(btAdapter==null) {
            Toast.makeText(getBaseContext(), "Device does not support bluetooth", Toast.LENGTH_LONG).show();
        } else {
            if (btAdapter.isEnabled()) {
                mLEScanner = btAdapter.getBluetoothLeScanner();
                settings = new ScanSettings.Builder()
                        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                        .build();
                filters = new ArrayList<ScanFilter>();
            } else {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, 1);
            }
        }
    }
//
//    //create new class for connect thread
//    private class ConnectedThread extends Thread {
//        private final InputStream mmInStream;
//        private final OutputStream mmOutStream;
//
//        //creation of the connect thread
//        public ConnectedThread(BluetoothSocket socket) {
//            InputStream tmpIn = null;
//            OutputStream tmpOut = null;
//
//            try {
//                //Create I/O streams for connection
//                tmpIn = socket.getInputStream();
//                tmpOut = socket.getOutputStream();
//            } catch (IOException e) { }
//
//            mmInStream = tmpIn;
//            mmOutStream = tmpOut;
//        }
//
//        public void run() {
//            byte[] buffer = new byte[256];
//            int bytes;
//
//            // Keep looping to listen for received messages
//            while (true) {
//                try {
//                    bytes = mmInStream.read(buffer);            //read bytes from input buffer
//                    String readMessage = new String(buffer, 0, bytes);
//                    // Send the obtained bytes to the UI Activity via handler
//                    bluetoothIn.obtainMessage(handlerState, bytes, -1, readMessage).sendToTarget();
//                } catch (IOException e) {
//                    break;
//                }
//            }
//        }
//        //write method
//        public void write(String input) {
//            byte[] msgBuffer = input.getBytes();           //converts entered String into bytes
//            try {
//                mmOutStream.write(msgBuffer);                //write bytes over BT connection via outstream
//            } catch (IOException e) {
//                //if you cannot write, close the application
//                Toast.makeText(getBaseContext(), "Connection Failure", Toast.LENGTH_LONG).show();
//                finish();
//
//            }
//        }
//    }
//


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
                            Toast.makeText(context, "Selected Temperature data", Toast.LENGTH_LONG).show();
                        }/*else if(data_choice==4) {
                            Toast.makeText(context, "Selected ViLight-IRLight data", Toast.LENGTH_LONG).show();
                        }else if(data_choice==5) {
                            Toast.makeText(context, "Selected Proximity data", Toast.LENGTH_LONG).show();
                        }*/


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
                            Yaxis_max = 2000;
                            Yaxis_min = -Yaxis_max;
                        }else if(data_choice==1) {
                            chartTitleString = "Gyro (rad/s)";
                            Yaxis_max = 100;
                            Yaxis_min = -Yaxis_max;
                        }else if(data_choice==2) {
                            chartTitleString = "Magnetometer (uT)";
                            Yaxis_max = 150;
                            Yaxis_min = -Yaxis_max;
                        }else if(data_choice==3) {
                            chartTitleString = "Temperature (℃)";
                            Yaxis_max = 50;
                            Yaxis_min = -Yaxis_max;
                        }/*else if(data_choice==4) {
                            chartTitleString = "ViLight-IRLight";
                            Yaxis_max = 500;
                            Yaxis_min = 200;
                        }else if(data_choice==5) {
                            chartTitleString = "Proximity";
                            Yaxis_max = 1000;
                            Yaxis_min = 500;
                        }*/

                        line = new LineChart();


                        new ArrayList<AccelemoterData>();
                        chartview = ChartFactory.getLineChartView(Data_logger_bgm111.context, line.dataset, line.multiRenderer);
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


}
