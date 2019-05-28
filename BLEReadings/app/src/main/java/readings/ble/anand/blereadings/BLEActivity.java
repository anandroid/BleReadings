package readings.ble.anand.blereadings;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.github.mikephil.charting.charts.ScatterChart;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.ScatterData;
import com.github.mikephil.charting.data.ScatterDataSet;

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
import readings.ble.anand.blereadings.utils.Utils;
import readings.ble.anand.blereadings.viewmodels.DevicesLiveData;
import readings.ble.anand.blereadings.viewmodels.ScannerStateLiveData;
import readings.ble.anand.blereadings.viewmodels.ScannerViewModel;

/* This is the activity which shows reading logs of individual BLE's in graph form */

public class BLEActivity extends AppCompatActivity {

    public static final String EXTRA_DEVICE = "readings.ble.anand.blereadings.EXTRA_DEVICE";


    private TextView resultTextView;
    private TextView currentReadingTextView;

    private ScannerViewModel mScannerViewModel;
    private Runnable dataCollectorRunnable;
    private Handler dataCollectorHandler;
    private Map<String, List<Integer>> bleHistoryMap = new HashMap<>();
    private List<ReadingAndTime> bleHistoryList = new ArrayList<>();
    private int currentWayPointIndex = 1;

    final int DATA_COLLECION_SIZE_FOR_EACH_WAYPOINT = 10000;
    final int DATA_COLLECTION_INTERVAL_IN_MS = 300;
    DiscoveredBluetoothDevice device;

    ScatterChart scatterChart;


    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ble);

        //gets ble device name to be read
        final Intent intent = getIntent();
        device = intent.getParcelableExtra(EXTRA_DEVICE);
        String deviceName = device.getName();
        String deviceAddress = device.getAddress();

        setUpUI(deviceName, deviceAddress);

        bleHistoryMap = new HashMap<>();

        startBLEReading();

    }

    // this code is to initialise UI elements - toolbar , textviews
    private void setUpUI(String deviceName, String deviceAddress) {
        final Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitleTextColor(getResources().getColor(R.color.white));
        setSupportActionBar(toolbar);
        if (!(deviceName != null && deviceName.trim().length() > 1)) {
            deviceName = "BLE";
        }
        getSupportActionBar().setTitle(deviceName);
        getSupportActionBar().setSubtitle(deviceAddress);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        resultTextView = (TextView) findViewById(R.id.resultTextView);
        currentReadingTextView = (TextView) findViewById(R.id.currentReadingReaTextView);
        scatterChart = findViewById(R.id.scatterChart);

        mScannerViewModel = ViewModelProviders.of(this).get(ScannerViewModel.class);
    }

    // This function populates the graph
    private void fillUpScatterData(ArrayList scatterEntries) {
        ScatterDataSet scatterDataSet = new ScatterDataSet(scatterEntries, "");
        ScatterData scatterData = new ScatterData(scatterDataSet);
        scatterDataSet.setColor(getResources().getColor(R.color.asuOrange));

        scatterChart.setData(scatterData);
        scatterChart.setContentDescription("");
        scatterChart.invalidate();
    }


    private void startBLEReading() {
        // Disable the UI view , enable when user clicked
        clear();
        resultTextView.setVisibility(View.GONE);
        //Start scanning and publishing
        mScannerViewModel.startScan();
        //Start handling the readings
        getData();
        //Start the subscriber
        mScannerViewModel.getScannerState().observe(this, this::startScan);
    }


    // This function populates the ble data read
    String stringConstructor = "";
    int previousStringConstructorIndex = 0;
    ArrayList scatterEntries = new ArrayList<>();

    private void populateDataOnText() {

        int lastReading = 0;

        // populate the log in the UI text view
        while (previousStringConstructorIndex < bleHistoryList.size()) {
            ReadingAndTime readingAndTime = bleHistoryList.get(previousStringConstructorIndex);
            String tempstringConstructor = readingAndTime.time + "  :  " + readingAndTime.reading + " dB" + "\n";
            stringConstructor = tempstringConstructor + stringConstructor;
            previousStringConstructorIndex++;
            lastReading = readingAndTime.reading;

            scatterEntries.add(new BarEntry(previousStringConstructorIndex, -readingAndTime.reading));
        }
        resultTextView.setText(stringConstructor);
        if (lastReading != 0) {
            currentReadingTextView.setText(lastReading + " dB");
        } else {
            currentReadingTextView.setText("");
        }

        // populate the log in the UI graph view
        fillUpScatterData(scatterEntries);
    }


    // returns the time of ble reading by our programme
    public String getCurrentTimeUsingCalendar() {
        Calendar cal = Calendar.getInstance();
        Date date = cal.getTime();
        DateFormat dateFormat = new SimpleDateFormat("mm:ss");
        String formattedDate = dateFormat.format(date);
        return formattedDate;
    }


    int index = 0;
    long previousTimeStampNanos = 0;
    private void getData() {

        index = 0;
        dataCollectorHandler = new Handler();
        // Define the code block to be executed
        dataCollectorRunnable = new Runnable() {
            @Override
            public void run() {

                // Do something here on the main thread
                Log.d("Handlers", "Called on main thread");
                // Repeat this the same runnable code block again another 2 seconds
                // 'this' is referencing the Runnable object
                DevicesLiveData devicesLiveData = mScannerViewModel.getDevices();
                if (devicesLiveData != null && devicesLiveData.getValue() != null) {
                    for (DiscoveredBluetoothDevice device : devicesLiveData.getValue()) {

                        if (device.getAddress().equals(BLEActivity.this.device.getAddress())) {
                            if (device.getScanResult().getTimestampNanos() != previousTimeStampNanos) {
                                bleHistoryList.add(new ReadingAndTime(getCurrentTimeUsingCalendar(), device.getRssi()));
                            }
                            previousTimeStampNanos = device.getScanResult().getTimestampNanos();
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
            if (!state.isBluetoothEnabled()) {
                clear();
            }
        } else {
            // Handling of permission is done in First Activity itself
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
