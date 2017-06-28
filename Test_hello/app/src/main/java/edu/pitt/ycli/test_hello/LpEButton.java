package edu.pitt.ycli.test_hello;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.location.Location;
import android.location.LocationListener;
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
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import org.achartengine.GraphicalView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

public class LpEButton extends AppCompatActivity {


    private Context context_this;
    private static Context context;

    private short temp=0;

    private Button init_msp430, Visible,list;

    final int handlerState = 0;
    Handler bluetoothIn;
    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;
    private Set<BluetoothDevice> pairedDevices;
    private StringBuilder recDataString = new StringBuilder();
    private ConnectedThread mConnectedThread;
    // SPP UUID service - this should work for most devices
    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    // String for MAC address
    private static String bt_address;
    private byte [] readData;
    private String readMessage;

    public String Data_logger_name = "LCN_DataLogger";
    public boolean Datalogger_found=false;
    BluetoothDevice paired_result = null;


    private int endOfLineIndex;
    private int index_start;//
    private int index_start_size;
    private String dataInPrint;    //

    private Button bt_requestimg;

    public String filename = "lpebutton_image_received";
    public File dir_path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
    SimpleDateFormat formatter = new SimpleDateFormat("_yyyy_MM_dd_HH_mm");

    public File data_file;

    byte[] header_size = new byte[]{-1,117,50,33};
    byte[] header_data = new byte[]{62,-1,-1,117};

    String header_size_Str = new String(header_size);
    String FrameStr;
    int header_size_flag = 0;
    int jpg_end_flag = 0;
    int totalframes=0;//100;
    int cnt_frames=0;

    int image_saved_flag;
    int new_img_flag;

    int header_size_flag_show=1;

    TextView progress_txt;
    String message_data;

    byte [] data_buffer=new byte[2*1024*1024];//2MB
    int cnt_received = 0;

    int valid_frame=0;
    int receive_error = 1;
    char feedback_byte;
    int sub_cnt_frames=0;

    int correction_byte;

    OutputStream os = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lp_ebutton);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Intent intent = getIntent();

        context_this = this;

        LpEButton.context = getApplicationContext();

        bt_requestimg = (Button) findViewById(R.id.button_image_requesting);
        bt_requestimg.setEnabled(true);

        image_saved_flag = 0;
        new_img_flag = 0;

        progress_txt = (TextView)findViewById(R.id.Progress_txt);
        progress_txt.setEnabled(true);


        bluetoothIn = new Handler() {
            public void handleMessage(android.os.Message msg) {
                if (msg.what == handlerState) {                                     //if message is what we want
                    readMessage = (String) msg.obj;
                    byte[] data;

                    if ((valid_frame >= 1) & (header_size_flag_show == 1)) {

                        bt_requestimg.setText("Valid Image !!!");
                        Toast.makeText(getApplicationContext(), "Image will be saved soon......", Toast.LENGTH_LONG).show();
                        header_size_flag_show = 0;
                    }

                    int ratio_t = (int) (100*((float)cnt_received/(40*totalframes)));
                    if(ratio_t>100) ratio_t = 100;
                    message_data = String.format("Receiving  Image:  %d%% ", ratio_t);
                    progress_txt.setText(message_data);//todayStateStr);

                    if ((jpg_end_flag == 1) & (header_size_flag == 1)) {

                        //message_data = String.format("Receiving picture: %d\n", cnt_received/(40*totalframes));
                        //progress_txt.setText("Progress:  100% ");//todayStateStr);

                        progress_txt.setText("Writing  Image ......");

                        Log.w("Writing, total bytes: ", Integer.toString(cnt_received));

                        cnt_frames = 0;
                        for (int k = 0; k < cnt_received - 20;) {
                            if ((data_buffer[k] == header_data[0]) & (data_buffer[k + 1] == header_data[1])&(data_buffer[k+2] == header_data[2]) & (data_buffer[k + 3] == header_data[3]))
                            {

                                data = Arrays.copyOfRange(data_buffer, k + 4, k + 20);

                                try {
                                    os = new FileOutputStream(data_file, true);
                                    os.write(data);
                                    os.close();

                                } catch (IOException e) {
                                    // Unable to create file, likely because external storage is
                                    // not currently mounted.
                                    Log.w("ExternalStorage", "Error writing " + data_file, e);
                                }

                                cnt_frames++;
                                Log.w("Image frame number ", Integer.toString(k)+", "+ Integer.toString(cnt_frames)+" of "+ Integer.toString(totalframes));

                                k += 20;
                            }
                            else
                                k++;
                        }

                        Log.w("Final frame number ", Integer.toString(cnt_frames));
                        mConnectedThread.resetConnection();

                        if (cnt_frames == totalframes)
                            Toast.makeText(getApplicationContext(), "Received correctly!", Toast.LENGTH_LONG).show();
                        else
                            Toast.makeText(getApplicationContext(), "Image corrupted!", Toast.LENGTH_LONG).show();

                        //if (cnt_frames == totalframes)
                        {
                            //System.exit(0);
                            finish();
                        }
                        cnt_frames = 0;
                        cnt_received = 0;
                        jpg_end_flag = 0;
                        valid_frame = 0;

                    }

                }
            }
        };
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        checkBTState();
    }

    public void Start_requesting(View view) {

        image_saved_flag = 0;
        new_img_flag = 0;

        bt_requestimg.setText("Pairing Bluetooth......");
        bt_requestimg.setEnabled(false);

        // get paired devices
        pairedDevices = btAdapter.getBondedDevices();

        for (BluetoothDevice bt : pairedDevices) {

            String paired_bt_name = bt.getName();
            if (paired_bt_name.equals(Data_logger_name)) {
                Datalogger_found = true;
                paired_result = bt;
                bt_address = bt.getAddress();

                break;
            }

        }

        if(Datalogger_found) {
            bt_requestimg.setText("Waiting for image......");

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

            //receive data
            //data_logger_init();

            //start to record data
            Toast.makeText(getApplicationContext(), "Image will be saved under .../documents/"
                    , Toast.LENGTH_LONG).show();
        }
        else
        {
            bt_requestimg.setText("Request image...");
            bt_requestimg.setEnabled(true);

            //start to record data
            Toast.makeText(getApplicationContext(), "Data_Logger_MSP430 was not found!"
                    , Toast.LENGTH_LONG).show();
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
        private InputStream mmInStream;
        private OutputStream mmOutStream;
        public byte[] buffer;

        public void resetConnection() {
            if (mmInStream != null) {
                try {mmInStream.close();} catch (Exception e) {}
                mmInStream = null;
            }

            if (mmOutStream != null) {
                try {mmOutStream.close();} catch (Exception e) {}
                mmOutStream = null;
            }

            if (btSocket != null) {
                try {btSocket.close();} catch (Exception e) {}
                btSocket = null;
            }

        }

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
            buffer = new byte[256];//256];
            int numBytes;
            //     OutputStream os = null;

            // Keep looping to listen for received messages
            while (true) {
                try {
                    numBytes = mmInStream.read(buffer);            //read bytes from input buffer
                    readData = buffer.clone();

                    receive_error = 1;

                    valid_frame++;

                    if(jpg_end_flag==0) {


                        //looking for jpg size
                        if(header_size_flag==0) {
                            FrameStr = new String(readData);
                            index_start = FrameStr.indexOf(header_size_Str);
                            if(index_start>=0) {
                                totalframes = (readData[index_start + 4] & 0xFF) + 256 * (readData[index_start + 5] & 0xFF);//
                                cnt_frames = 0;
                                header_size_flag = 1;

                                Log.w("total frame : ", Integer.toString(totalframes));

                                dir_path.mkdirs();
                                Date now = new Date();
                                String fileName_formatted = filename + formatter.format(now) + ".jpg";
                                data_file = new File(dir_path, fileName_formatted);

                                //delete data
                                for(int i=index_start-2;i<index_start+16;i++)
                                    readData[i] = 0;

                                //collect all valid data
                                System.arraycopy(readData, 0, data_buffer, cnt_received, numBytes);
                                cnt_received += numBytes;
                                //Log.w("total bytes received : ", Integer.toString(cnt_received));
                            }
                        }
                        else {

                            //collect all valid data
                            System.arraycopy(readData, 0, data_buffer, cnt_received, numBytes);
                            cnt_received += numBytes;
                            Log.w("bytes received ", Integer.toString(valid_frame)+", "+Integer.toString(numBytes) );

                            //check jpg end
                            for (int j = 0; j < numBytes - 1; j++) {
                                if ((readData[j] == -1) & (readData[j + 1] == -39)) {
                                    jpg_end_flag = 1;

                                    Log.w("jpg_end_flag : ", Integer.toString(jpg_end_flag));
                                }
                            }
                        }
                    }


//            buffer = new byte[1024];
//            int numBytes;
//       //     OutputStream os = null;
//
//            // Keep looping to listen for received messages
//            while (true) {
//                try {
//                    numBytes = mmInStream.read(buffer);            //read bytes from input buffer
//                    readData = buffer.clone();
//
//                    receive_error = 1;
//
//                    if(header_size_flag==0) {
//
//                        //os.close();//close previous file
//
//                        // msg.arg1 = bytes from connect thread
//                        //recDataString.append(readMessage);                                      //keep appending to string until ~
//                        //endOfLineIndex = recDataString.indexOf("~");                    // determine the end-of-line
//                        FrameStr = new String(readData);
//                        index_start = FrameStr.indexOf(header_size_Str);
//                        if(index_start>=0){
//                            totalframes = (readData[index_start+4]&0xFF)+256*(readData[index_start+5]&0xFF);//
//                            cnt_frames = 0;
//                            header_size_flag = 1;
//
//                            valid_frame = 1;
//                            receive_error = 0;
//
//                            //file operation
//                            /*dir_path.mkdirs();
//                            Date now = new Date();
//                            String fileName_formatted = filename + formatter.format(now) + ".jpg";
//                            data_file = new File(dir_path, fileName_formatted);
//
//                            os = new FileOutputStream(data_file, true);//open file*/
//
//                            //feedback_byte = 0xb4;
//                            //String initalD = String.format("%s", feedback_byte);
//                            //mConnectedThread.write(Integer.toString(feedback_byte));    // Send "0" via B
//
//                        }
//                    }
//                    else{//recDataString.charAt(0) == '#') {
//
//                        byte[] data;
//
//                        FrameStr = new String(readData);
//
//                        //check possible new header
//                        index_start = FrameStr.indexOf(header_size_Str);
//                        if(index_start>=0){
//                            //totalframes = (readData[index_start+4]&0xFF)+256*(readData[index_start+5]&0xFF);//
//
//                            valid_frame = 1;
//
//
//                            receive_error = 0;
//
//                            //save old image
//                            byte[] jpg_data = new byte[16 * cnt_frames];
//                            System.arraycopy(data_buffer, 0, jpg_data, 0, 16 * cnt_frames);
//                            //file operation
//                            dir_path.mkdirs();
//                            Date now = new Date();
//                            String fileName_formatted = filename + formatter.format(now) + ".jpg";
//                            data_file = new File(dir_path, fileName_formatted);
//
//                            try {
//                                os = new FileOutputStream(data_file, true);
//                                os.write(jpg_data);
//                                os.close();
//
//                            } catch (IOException e) {
//                                // Unable to create file, likely because external storage is
//                                // not currently mounted.
//                                Log.w("ExternalStorage", "Error writing " + data_file, e);
//                            }
//
//                            System.exit(0);
//
//                            //new_img_flag = 1;
//                            //cnt_frames = 0;
//                        }
//                        else {//valid image data
//                            index_start = FrameStr.indexOf(header_data_Str);
//
//                            //if(cnt_frames==0)
//                            //   data = Arrays.copyOfRange(readData,1,16);//skip the first byte which is 0
//                            //else
//                            //    data = Arrays.copyOfRange(readData,0,32);//16);
//
//                            ////save data
//                            if (index_start >= 0) {
//                                data = Arrays.copyOfRange(readData, index_start + 2, index_start + +19);//18);//34);
//
//                                //verify data
//                                correction_byte=0;
//                                for(i=0;i<16;i++)
//                                    correction_byte += data[i];
//
//                                if((correction_byte&0xFF)==(data[16]&0xFF)) {
//                                    receive_error = 0;
//
//                                    System.arraycopy(data, 0, data_buffer, 16 * cnt_frames, 16);
//
//                                    cnt_frames++;
//
//                                    valid_frame=1;
//                                }
//                                else
//                                    receive_error = 1;
//
//                                //feedback_byte = 0xb3;
//                                //String initalD = String.format("%s", feedback_byte);
//                                //mConnectedThread.write(Integer.toString(feedback_byte));    // Send "0" via B
//
//                                //if(cnt_frames%100==0)
//                                    Log.w("Image frame number ", Integer.toString(cnt_frames)+" of "+ Integer.toString(totalframes));
//
//                                if (cnt_frames == totalframes) {
//                                    byte[] jpg_data = new byte[16 * totalframes];
//
//                                    header_size_flag = 0;
//
//                                    System.arraycopy(data_buffer, 0, jpg_data, 0, 16 * totalframes);
//
//                                    //file operation
//                                    dir_path.mkdirs();
//                                    Date now = new Date();
//                                    String fileName_formatted = filename + formatter.format(now) + ".jpg";
//                                    data_file = new File(dir_path, fileName_formatted);
//
//                                    try {
//                                        os = new FileOutputStream(data_file, true);
//                                        os.write(jpg_data);
//                                        os.close();
//
//                                        image_saved_flag = 1;
//
//                                    } catch (IOException e) {
//                                        // Unable to create file, likely because external storage is
//                                        // not currently mounted.
//                                        Log.w("ExternalStorage", "Error writing " + data_file, e);
//                                    }
//
//
//                                    //os.close();//close file
//                                    //Toast.makeText(getApplicationContext(), "Image named: "+data_file.toString(), Toast.LENGTH_LONG).show();
//
//                                    System.exit(0);
//                                }
//                            }
//                        }
//                    }
//
                    String readMessage = new String(buffer, 0, numBytes);
                    // Send the obtained bytes to the UI Activity via handler
                    bluetoothIn.obtainMessage(handlerState, numBytes, -1, readMessage).sendToTarget();
                    //bluetoothIn.obtainMessage(handlerState, numBytes, -1, buffer).sendToTarget();
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
