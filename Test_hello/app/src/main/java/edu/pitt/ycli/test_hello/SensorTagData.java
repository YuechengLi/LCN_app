package edu.pitt.ycli.test_hello;

import android.bluetooth.BluetoothGattCharacteristic;

/**
 * Created by Dave Smith
 * Double Encore, Inc.
 * SensorTagData
 * Conversions for sensor data values on the TI SensorTag
 */
public class SensorTagData {

    private static float temperature;
    private static float[] acc = new float[3];
    private static float[] gyro = new float[3];
    private static float[] mag = new float[3];

    public static float get_Motion_temp()
    {
        return temperature;
    }

    public static float[] get_Motion_acc()
    {
        return acc;
    }

    public static float[] get_Motion_mag()
    {
        return mag;
    }

    public static float[] get_Motion_gyro()
    {
        return gyro;
    }
    public static void extractMotion(BluetoothGattCharacteristic c, int offset) {
        int data;

        data = shortSignedAtOffset(c, offset);//
        temperature = ((float)data-21.0f)/333.87f+21.0f;

        data = shortSignedAtOffset(c, offset+2);//
        acc[0] = (float)data*4000.0f/32768.0f;
        data = shortSignedAtOffset(c, offset+4);//
        acc[1] = (float)data*4000.0f/32768.0f;
        data = shortSignedAtOffset(c, offset+6);//
        acc[2] = (float)data*4000.0f/32768.0f;

        data = shortSignedAtOffset(c, offset+8);//
        gyro[0] = (float)data*500.0f/32768.0f;
        data = shortSignedAtOffset(c, offset+10);//
        gyro[1] = (float)data*500.0f/32768.0f;
        data = shortSignedAtOffset(c, offset+12);//
        gyro[2] = (float)data*500.0f/32768.0f;

        data = shortSignedAtOffset(c, offset+14);//
        mag[0] = (float)data*0.6f;///10.0f;
        data = shortSignedAtOffset(c, offset+16);//
        mag[1] = (float)data*0.6f;////10.0f;
        data = shortSignedAtOffset(c, offset+18);//
        mag[2] = (float)data*0.6f;////10.0f;

        //acc = shortUnsignedAtOffset(c, 2);
        //acc = shortUnsignedAtOffset(c, 4);
        // bits [1..0] are status bits and need to be cleared
        //a = a - (a % 4);

       // return data;//((-6f) + 125f * (a / 65535f));
    }

    public static int extractMotion_acc(BluetoothGattCharacteristic c, int offset) {
        int data;

        data = intSignedAtOffset(c, offset);//shortSignedAtOffset(c, offset);//

        if(data>4000)
            data = data - 8000;

        //acc = shortUnsignedAtOffset(c, 2);
        //acc = shortUnsignedAtOffset(c, 4);
        // bits [1..0] are status bits and need to be cleared
        //a = a - (a % 4);

        return data;//((-6f) + 125f * (a / 65535f));
    }

    public static int extractMotion_gyro(BluetoothGattCharacteristic c, int offset) {
        int data;

        data = intSignedAtOffset(c, offset);//shortSignedAtOffset(c, offset);//

        if(data>5000)
            data = data - 10000;

        //acc = shortUnsignedAtOffset(c, 2);
        //acc = shortUnsignedAtOffset(c, 4);
        // bits [1..0] are status bits and need to be cleared
        //a = a - (a % 4);

        return data;//((-6f) + 125f * (a / 65535f));
    }

    public static double extractHumAmbientTemperature(BluetoothGattCharacteristic c) {
        int rawT = shortSignedAtOffset(c, 0);

        return -46.85 + 175.72/65536 *(double)rawT;
    }

    public static double extractHumidity(BluetoothGattCharacteristic c) {
        int a = shortUnsignedAtOffset(c, 2);
        // bits [1..0] are status bits and need to be cleared
        a = a - (a % 4);

        return ((-6f) + 125f * (a / 65535f));
    }

    public static int[] extractCalibrationCoefficients(BluetoothGattCharacteristic c) {
        int[] coefficients = new int[8];

        coefficients[0] = shortUnsignedAtOffset(c, 0);
        coefficients[1] = shortUnsignedAtOffset(c, 2);
        coefficients[2] = shortUnsignedAtOffset(c, 4);
        coefficients[3] = shortUnsignedAtOffset(c, 6);
        coefficients[4] = shortSignedAtOffset(c, 8);
        coefficients[5] = shortSignedAtOffset(c, 10);
        coefficients[6] = shortSignedAtOffset(c, 12);
        coefficients[7] = shortSignedAtOffset(c, 14);

        return coefficients;
    }

    public static double extractBarTemperature(BluetoothGattCharacteristic characteristic, final int[] c) {
        // c holds the calibration coefficients

        int t_r;	// Temperature raw value from sensor
        double t_a; 	// Temperature actual value in unit centi degrees celsius

        t_r = shortSignedAtOffset(characteristic, 0);

        t_a = (100 * (c[0] * t_r / Math.pow(2,8) + c[1] * Math.pow(2,6))) / Math.pow(2,16);

        return t_a / 100;
    }

    public static double extractBarometer(BluetoothGattCharacteristic characteristic, final int[] c) {
        // c holds the calibration coefficients

        int t_r;	// Temperature raw value from sensor
        int p_r;	// Pressure raw value from sensor
        double S;	// Interim value in calculation
        double O;	// Interim value in calculation
        double p_a; 	// Pressure actual value in unit Pascal.

        t_r = shortSignedAtOffset(characteristic, 0);
        p_r = shortUnsignedAtOffset(characteristic, 2);


        S = c[2] + c[3] * t_r / Math.pow(2,17) + ((c[4] * t_r / Math.pow(2,15)) * t_r) / Math.pow(2,19);
        O = c[5] * Math.pow(2,14) + c[6] * t_r / Math.pow(2,3) + ((c[7] * t_r / Math.pow(2,15)) * t_r) / Math.pow(2,4);
        p_a = (S * p_r + O) / Math.pow(2,14);

        //Convert pascal to in. Hg
        double p_hg = p_a * 0.000296;

        return p_hg;
    }
    
    /**
     * Gyroscope, Magnetometer, Barometer, IR temperature
     * all store 16 bit two's complement values in the awkward format
     * LSB MSB, which cannot be directly parsed as getIntValue(FORMAT_SINT16, offset)
     * because the bytes are stored in the "wrong" direction.
     *
     * This function extracts these 16 bit two's complement values.
     * */
    private static Integer shortSignedAtOffset(BluetoothGattCharacteristic c, int offset) {
        Integer lowerByte = c.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, offset);
        Integer upperByte = c.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT8, offset + 1); // Note: interpret MSB as signed.

        return (upperByte << 8) + lowerByte;
    }
    private static Integer shortUnsignedAtOffset(BluetoothGattCharacteristic c, int offset) {
        Integer lowerByte = c.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, offset);
        Integer upperByte = c.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, offset + 1); // Note: interpret MSB as unsigned.

        return (upperByte << 8) + lowerByte;
    }

    private static Integer intSignedAtOffset(BluetoothGattCharacteristic c, int offset) {
        Integer Byte0 = c.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, offset);
        Integer Byte1 = c.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, offset + 1);
        Integer Byte2 = c.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, offset + 2);
        Integer Byte3 = c.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT8, offset + 3);// Note: interpret MSB as signed.

        return (Byte3 << 24) + (Byte2 << 16) +(Byte1 << 8) +Byte0;
    }

}
