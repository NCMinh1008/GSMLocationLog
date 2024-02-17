    package com.navis.gsmlocationlog;

    import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
    import static android.Manifest.permission.ACCESS_FINE_LOCATION;
    import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
    import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

    import androidx.appcompat.app.AppCompatActivity;

    import android.content.pm.PackageManager;
    import android.os.Build;
    import android.os.Bundle;
    import android.os.Environment;
    import android.util.Log;
    import android.view.View;
    import android.widget.Button;
    import android.widget.TextView;
    import android.widget.Toast;

    import java.io.BufferedWriter;
    import java.io.File;
    import java.io.FileWriter;
    import java.io.IOException;
    import java.text.SimpleDateFormat;
    import java.util.ArrayList;
    import java.util.Date;
    import java.util.Locale;
    import java.util.Timer;
    import java.util.TimerTask;

    public class MainActivity extends AppCompatActivity {

        private ArrayList<String> permissionsToRequest;
        private ArrayList<String> permissionsRejected = new ArrayList<>();
        private ArrayList<String> permissions = new ArrayList<>();
        private final static int ALL_PERMISSIONS_RESULT = 101;
        GPSTrack gpsTrack;
        GSMTrack gsmTrack;
        TextView tvGPSLog;
        TextView tvGSMLog;
        TextView tvStatus;
        String gpsResult = "";
        String gsmResult = "";
        boolean isScanEnabled = false;
        boolean isOneShot = false;
        private File logFile;
        private File snapshotFile;
        private long measure_count = 0;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_main);
            permissions.add(ACCESS_FINE_LOCATION);
            permissions.add(ACCESS_COARSE_LOCATION);
            permissions.add(WRITE_EXTERNAL_STORAGE);
            permissions.add(READ_EXTERNAL_STORAGE);
            permissionsToRequest = findUnAskedPermissions(permissions);

            //get the permissions we have asked for before but are not granted..
            //we will store this in a global list to access later.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (permissionsToRequest.size() > 0)
                    requestPermissions(permissionsToRequest.toArray(new String[permissionsToRequest.size()]), ALL_PERMISSIONS_RESULT);
            }

            tvGPSLog = findViewById(R.id.tvShowGPS);
            tvGSMLog = findViewById(R.id.tvShowCellInfo);
            tvStatus = findViewById(R.id.tvStatus);

            try{
                if (!isExternalStorageAvailable() || isExternalStorageReadOnly()) {
                    // if the external storage is not avialable then we are displaying the toast message on below line.
                    Toast.makeText(this, "External storage not available on the device..", Toast.LENGTH_SHORT).show();
                } else {
                    String currentTime = new SimpleDateFormat("MMdd_HHmmss", Locale.getDefault()).format(new Date());
                    logFile = new File("/sdcard/logFile_" + currentTime + ".csv");
                    snapshotFile = new File("/sdcard/oneshotFile_" + currentTime + ".csv");
                    Log.d("FILE*", "OPEN LOG FILE SUCCESSFUL");
                    if (!logFile.exists()) {
                        try {
                            logFile.createNewFile();
                            Log.d("FILE*", "CREATE NEW LOG FILE");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    if(!snapshotFile.exists()){
                        try{
                            snapshotFile.createNewFile();
                        }
                        catch (Exception e){
                            e.printStackTrace();
                        }
                    }
                }
            }
            catch (Exception e){
                e.printStackTrace();
            }

            Button btnStart = (Button) findViewById(R.id.btnStart);
            Button btnStop = (Button) findViewById(R.id.btnStop);
            btnStart.setEnabled(true);
            btnStop.setEnabled(true);
            isScanEnabled = false;
            btnStart.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    isScanEnabled = true;
                    btnStart.setEnabled(false);
                    btnStop.setEnabled(true);
                    tvStatus.setText("Status: Scanning ...");
                }
            });

            btnStop.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    isScanEnabled = false;
                    btnStart.setEnabled(true);
                    btnStop.setEnabled(false);
                    tvStatus.setText("Status: Stop");
                }
            });

            Button btnOneShot = (Button) findViewById(R.id.btnOneShot);
            btnOneShot.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    isOneShot = true;
                }
            });

            gpsTrack = new GPSTrack(MainActivity.this);
            gsmTrack = new GSMTrack(MainActivity.this);
            gsmTrack.scanCellInfo();
            gpsTrack.requestLocation();
            measure_count = 0;
            Timer timerLog = new Timer();
            timerLog.schedule(new TimerTask() {
                @Override
                public void run() {
                    if(isScanEnabled) {
                        gpsTrack.requestLocation();
                        double lat = gpsTrack.getLatitude();
                        double lon = gpsTrack.getLongitude();
                        gpsResult = "LATITUDE:  " + lat + "\nLONGITUDE: " + lon;
                        measure_count++;
                        Log.d("FILE*", "LATITUDE:  " + lat + " LONGITUDE: " + lon);
                        String record = String.format("%d, %.8f, %.8f", measure_count, lat, lon);
                        gsmTrack.scanCellInfo();
                        gsmResult = "";
                        if (gsmTrack.lstBS != null && gsmTrack.lstBS.size() > 0) {
                            long timeStamp = gsmTrack.lstBS.get(0).getTimeStamp();
                            int num_of_cell = gsmTrack.lstBS.size();
                            record = record + ", " + timeStamp + ", " + num_of_cell;
                            for (int i = 0; i < gsmTrack.lstBS.size(); i++) {
                                GSMTrack.BaseStation bs = gsmTrack.lstBS.get(i);
                                String s = (i + 1) + ", " + bs.toString();
                                gsmResult = gsmResult + "\n" + s;
                                int lac = bs.getLac();
                                int cid = bs.getCid();
                                int rssi = bs.getRssi();
                                record = record + ", " + lac + ", " + cid + ", " + rssi;
                            }
                        }
                        if(isOneShot){
                            //append to oneshot log file
                            appendToOneShotFile(record);
                            isOneShot = false;
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(getApplicationContext(), "ONESHOT RECORDED \nMEASUREMENT COUNT: " + measure_count +
                                            "\nLAT: " + lat + " LON: " + lon, Toast.LENGTH_LONG).show();
                                }
                            });
                        }
                        else {
                            appendToLogFile(record);
                        }
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                tvGPSLog.setText(gpsResult);
                                tvGSMLog.setText(gsmResult);
                            }
                        });
                    }
                    else{
                        Log.d("GSM_LOG", "SCAN STOPPED\n");
                    }
                }
            }, 1000, 2000 );
        }

        private ArrayList<String> findUnAskedPermissions(ArrayList<String> wanted) {
            ArrayList<String> result = new ArrayList<String>();
            for (String perm : wanted) {
                if (!hasPermission(perm)) {
                    result.add(perm);
                }
            }
            return result;
        }

        private boolean hasPermission(String permission) {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1){
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    return (checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED);
                }
            }
            return true;
        }

        public void appendToLogFile(String text){
            try
            {
                if(logFile != null && logFile.exists()) {
                    BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true));
                    buf.append(text);
                    buf.newLine();
                    buf.close();
                }
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
        public void appendToOneShotFile(String text){
            try
            {
                if(snapshotFile != null && snapshotFile.exists()) {
                    BufferedWriter buf = new BufferedWriter(new FileWriter(snapshotFile, true));
                    buf.append(text);
                    buf.newLine();
                    buf.close();
                }
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
        private static boolean isExternalStorageReadOnly() {
            // on below line getting external storage and checking if it is media mounted read only.
            String extStorageState = Environment.getExternalStorageState();
            if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(extStorageState)) {
                return true;
            }
            return false;
        }
        private static boolean isExternalStorageAvailable() {
            // on below line checking external storage weather it is available or not.
            String extStorageState = Environment.getExternalStorageState();
            if (Environment.MEDIA_MOUNTED.equals(extStorageState)) {
                return true;
            }
            return false;
        }

    }