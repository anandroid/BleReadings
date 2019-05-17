package readings.ble.anand.blereadings;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProviders;
import readings.ble.anand.blereadings.adapter.DiscoveredBluetoothDevice;
import readings.ble.anand.blereadings.objects.ReadingAndTime;
import readings.ble.anand.blereadings.utils.StoreBleResultInFile;
import readings.ble.anand.blereadings.utils.Utils;
import readings.ble.anand.blereadings.viewmodels.DevicesLiveData;
import readings.ble.anand.blereadings.viewmodels.ScannerStateLiveData;
import readings.ble.anand.blereadings.viewmodels.ScannerViewModel;

public class WayPointActivity extends AppCompatActivity {

    public static final String EXTRA_DEVICE = "readings.ble.anand.blereadings.EXTRA_DEVICE";


    private TextView resultTextView;
    private TextView currentReadingTextView;

    private ScannerViewModel mScannerViewModel;
    private Runnable dataCollectorRunnable;
    private Handler dataCollectorHandler;
    private Map<String,List<Integer>> bleHistoryMap =  new HashMap<>();
    private List<ReadingAndTime> bleHistoryList = new ArrayList<>();
    private int currentWayPointIndex = 1;

    final int DATA_COLLECION_SIZE_FOR_EACH_WAYPOINT = 10000 ;
    final int DATA_COLLECTION_INTERVAL_IN_MS = 300;
    DiscoveredBluetoothDevice device;


    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_waypoint);

        final Intent intent = getIntent();
        device = intent.getParcelableExtra(EXTRA_DEVICE);
        String deviceName = device.getName();
        String deviceAddress = device.getAddress();

        final Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitleTextColor(getResources().getColor(R.color.white));
        setSupportActionBar(toolbar);
        if(!(deviceName!=null && deviceName.trim().length()>1)){
            deviceName = "BLE";
        }
        getSupportActionBar().setTitle(deviceName);
        getSupportActionBar().setSubtitle(deviceAddress);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);


        resultTextView = (TextView)findViewById(R.id.resultTextView);
        currentReadingTextView = (TextView)findViewById(R.id.currentReadingReaTextView);






        mScannerViewModel = ViewModelProviders.of(this).get(ScannerViewModel.class);
        //mScannerViewModel.getScannerState().observe(this, this::startScan);

        bleHistoryMap = new HashMap<>();

        handleIHaveMoved();
    }



    private void handleIHaveMoved(){
        clear();
        resultTextView.setVisibility(View.VISIBLE);
        mScannerViewModel.startScan();
        getData(DATA_COLLECION_SIZE_FOR_EACH_WAYPOINT);
        mScannerViewModel.getScannerState().observe(this, this::startScan);
    }

    String stringConstructor ="";
    int previousStringConstructorIndex=0;

    private void populateDataOnText(){

        int lastReading=0;

        while (previousStringConstructorIndex<bleHistoryList.size()){
            ReadingAndTime readingAndTime = bleHistoryList.get(previousStringConstructorIndex);
            String tempstringConstructor= readingAndTime.time+"  :  "+readingAndTime.reading+" dB"+"\n";
            stringConstructor=tempstringConstructor+stringConstructor;
            previousStringConstructorIndex++;
            lastReading = readingAndTime.reading;
        }
        resultTextView.setText(stringConstructor);
        currentReadingTextView.setText(lastReading+" dB");
    }

    public String  getCurrentTimeUsingCalendar() {
        Calendar cal = Calendar.getInstance();
        Date date=cal.getTime();
        DateFormat dateFormat = new SimpleDateFormat("mm:ss");
        String formattedDate=dateFormat.format(date);
        return formattedDate;
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

                            if(device.getAddress().equals(WayPointActivity.this.device.getAddress())){
                                bleHistoryList.add(new ReadingAndTime(getCurrentTimeUsingCalendar(),device.getRssi()));
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

    @Override
    protected void onRestart() {
        super.onRestart();
        clear();
    }

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
