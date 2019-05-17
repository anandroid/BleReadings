package readings.ble.anand.blereadings.utils;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class StoreBleResultInFile {

    public static void store(Context context, int wayPointIndex , Map<String,List<Integer>> bleHistoryMap){

        String mainStringConstructor = "{";

        int index = 0;
        while(true) {


            boolean limitOverForADevice = false;

            String stringConstructor = "\""+String.valueOf(index + 1) +"\""+" : [" ;

            for (String deviceName : bleHistoryMap.keySet()) {

                List<Integer> historyList = bleHistoryMap.get(deviceName);
                if (historyList.size() <= index) {
                    limitOverForADevice = true;
                    break;
                }
                int rssiPower = historyList.get(index);
                stringConstructor = stringConstructor + "{  \"" + deviceName + "\"  :  \"" + String.valueOf(rssiPower);
                stringConstructor = stringConstructor + "\"},";
            }
            if(bleHistoryMap.keySet().size()>0) {
                stringConstructor = stringConstructor.substring(0, stringConstructor.length() - 1) + "]";
            } else {
                stringConstructor = stringConstructor+ "]";
            }
            mainStringConstructor = mainStringConstructor+stringConstructor;
            if (limitOverForADevice) {
                mainStringConstructor = mainStringConstructor+"}";
                break;
            }
            mainStringConstructor = mainStringConstructor+",";



            index++;
        }

        Log.d("Here","Writing to File");

        writeToFile("WayPoint_"+String.valueOf(wayPointIndex)+".json",mainStringConstructor,context);

        Log.d("Here","written Done");
    }

    private static void writeToFile(String fileName,String data,Context context) {
        try{
           // File path = context.getFilesDir();
            File folder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)+"/WayPoints/");
            folder.mkdirs();
            File file = new File(folder,fileName);
            file.createNewFile();
            FileOutputStream stream = new FileOutputStream(file);
            try {
                stream.write(data.getBytes());
            } finally {
                stream.close();
            }

        }catch (IOException e){
            Log.d("Exception", "File write failed: " + e.toString());
        }
    }
}
