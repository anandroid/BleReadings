package readings.ble.anand.blereadings.utils;

import android.util.Log;

import java.math.BigDecimal;

public class BLEUtils {


    //Calculate distance from RSSI
    public static double getDistanceFromRSSI(int rssi){
        int N = 4;
        BigDecimal  differenceBigDecimal = new BigDecimal(-73 - rssi);
        BigDecimal  denominatorBigDecimal  = new BigDecimal(10*N);

        BigDecimal resultBigDecimal = differenceBigDecimal.divide(denominatorBigDecimal);

        float exponent = (-73 - rssi)/(10*2) ;
        //Log.d("Distance",  Math.pow(10,resultBigDecimal.doubleValue()) + " - Power "+rssi + " exponent "+resultBigDecimal.doubleValue());
        return Math.pow(10,resultBigDecimal.doubleValue());
    }
}

