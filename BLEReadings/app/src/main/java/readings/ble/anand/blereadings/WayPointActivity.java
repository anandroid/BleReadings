package readings.ble.anand.blereadings;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProviders;
import readings.ble.anand.blereadings.adapter.DiscoveredBluetoothDevice;
import readings.ble.anand.blereadings.utils.StoreBleResultInFile;
import readings.ble.anand.blereadings.utils.Utils;
import readings.ble.anand.blereadings.viewmodels.DevicesLiveData;
import readings.ble.anand.blereadings.viewmodels.ScannerStateLiveData;
import readings.ble.anand.blereadings.viewmodels.ScannerViewModel;


public class WayPointActivity extends AppCompatActivity {

    private TextView currentWayPoinStatusTextView,resultTextView;
    private Button currentWayPointStatusButton;

    private ScannerViewModel mScannerViewModel;
    private Runnable dataCollectorRunnable;
    private Handler dataCollectorHandler;
    private Map<String,List<Integer>> bleHistoryMap =  new HashMap<>();
    private int currentWayPointIndex = 1;

    final int DATA_COLLECION_SIZE_FOR_EACH_WAYPOINT = 100 ;
    final int DATA_COLLECTION_INTERVAL_IN_MS = 300;

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_waypoint);

        currentWayPoinStatusTextView = (TextView)findViewById(R.id.current_way_point_status);
        currentWayPointStatusButton = (Button) findViewById(R.id.current_way_point_status_button);
        resultTextView = (TextView)findViewById(R.id.resultTextView);
        currentWayPointStatusButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                handleIHaveMoved();
            }
        });

        mScannerViewModel = ViewModelProviders.of(this).get(ScannerViewModel.class);
        //mScannerViewModel.getScannerState().observe(this, this::startScan);

        bleHistoryMap = new HashMap<>();
    }

    private void setCurrentWayPoinStatusText(int currentWayPointIndex){
        currentWayPoinStatusTextView.setText("Move to WayPoint : "+String.valueOf(currentWayPointIndex));
        currentWayPointStatusButton.setVisibility(View.VISIBLE);
        resultTextView.setVisibility(View.INVISIBLE);
        Log.d("Here","Text Changed");
    }

    private void handleIHaveMoved(){
        clear();
        currentWayPointStatusButton.setVisibility(View.INVISIBLE);
        resultTextView.setVisibility(View.VISIBLE);
        currentWayPoinStatusTextView.setText("Collecting Data for WayPoint : "+String.valueOf(currentWayPointIndex));
        mScannerViewModel.startScan();
        getData(DATA_COLLECION_SIZE_FOR_EACH_WAYPOINT);
        mScannerViewModel.getScannerState().observe(this, this::startScan);
    }

    private void populateDataOnText(){
        String stringConstructor ="";
        for (String deviceName : bleHistoryMap.keySet()){
             List<Integer> historyList = bleHistoryMap.get(deviceName);
             int rssiPower  = historyList.get(historyList.size()-1);
             stringConstructor = stringConstructor+" "+deviceName+"  :  "+String.valueOf(rssiPower);
             stringConstructor = stringConstructor+"\n";
        }
        resultTextView.setText(stringConstructor);
    }


    int index = 0;
    private void getData(final int dataCollectionSize) {

        index = 0 ;
        dataCollectorHandler = new Handler();
        // Define the code block to be executed
        dataCollectorRunnable = new Runnable() {
            @Override
            public void run() {
                if(index<dataCollectionSize) {
                    // Do something here on the main thread
                    Log.d("Handlers", "Called on main thread");
                    // Repeat this the same runnable code block again another 2 seconds
                    // 'this' is referencing the Runnable object
                    DevicesLiveData devicesLiveData = mScannerViewModel.getDevices();
                    if (devicesLiveData != null && devicesLiveData.getValue() != null) {
                        for (DiscoveredBluetoothDevice device : devicesLiveData.getValue()) {

                            if(device.getName()==null || device.getName().equalsIgnoreCase("null")
                                    || device.getName().trim().length()==0){
                                device.setName(device.getAddress());
                            }

                            if (!bleHistoryMap.containsKey(device.getName())) {
                                bleHistoryMap.put(device.getName(), new ArrayList<Integer>());
                            }

                            List<Integer> historyList = bleHistoryMap.get(device.getName());

                            historyList.add(device.getRssi());
                            bleHistoryMap.put(device.getName(), historyList);

                            Log.d("Device ", device.getName() + " " + device.getRssi());
                        }
                    }
                    populateDataOnText();
                    dataCollectorHandler.postDelayed(this, DATA_COLLECTION_INTERVAL_IN_MS);
                } else {
                    StoreBleResultInFile.store(getApplicationContext(),currentWayPointIndex,bleHistoryMap);
                    setCurrentWayPoinStatusText(++currentWayPointIndex);
                    stopScan();
                }
                index++;
            }
        };
        dataCollectorHandler.post(dataCollectorRunnable);
    }




    /**
     * Start scanning for Bluetooth devices or displays a message based on the scanner state.
     */
    private void startScan(final ScannerStateLiveData state) {
        // First, check the Location permission. This is required on Marshmallow onwards in order
        // to scan for Bluetooth LE devices.
        if (Utils.isLocationPermissionsGranted(this)) {
            // Bluetooth must be enabled
            if (state.isBluetoothEnabled()) {
                // We are now OK to start scanning
                if (!state.hasRecords()) {

                } else {

                }
            } else {
                clear();
            }
        } else {
            final boolean deniedForever = Utils.isLocationPermissionDeniedForever(this);
        }
    }

    //Handle onRestart of Activity
    @Override
    protected void onRestart() {
        super.onRestart();
        clear();
    }


    //Handle onStop of Activity
    @Override
    protected void onStop() {
        super.onStop();
        stopScan();
    }

    /**
     * stop scanning for bluetooth devices.
     */
    private void stopScan() {
        mScannerViewModel.stopScan();
        dataCollectorHandler.removeCallbacks(dataCollectorRunnable);
    }

    /**
     * Clears the list of devices, which will notify the observer.
     */
    private void clear() {
        mScannerViewModel.getDevices().clear();
        mScannerViewModel.getScannerState().clearRecords();
        bleHistoryMap = new HashMap<>();
    }


}
