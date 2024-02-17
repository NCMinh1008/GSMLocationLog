package com.navis.gsmlocationlog;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.telephony.CellIdentityGsm;
import android.telephony.CellIdentityLte;
import android.telephony.CellIdentityWcdma;
import android.telephony.CellInfo;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoWcdma;
import android.telephony.CellLocation;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.telephony.gsm.GsmCellLocation;
import android.util.Log;
import android.Manifest;

import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.List;

public class GSMTrack {
    private final Context mContext;
    public TelephonyManager telephonyManager;
    public class BaseStation {
        public String mcc;            // Mobile Country Code
        public String mnc;            // Mobile Network Code
        public int lac;            // Location Area Code or TAC(Tracking Area Code) for LTE
        public int cid;            // Cell Identity
        public int rssi;            // Signal strength as dBm
        public String type;        // Signal type, GSM or WCDMA or LTE or CDMA
        public long timeStamp;

        // Getter and Setter for all fields
        public String getMcc() {
            return mcc;
        }

        public void setMcc(String mcc) {
            this.mcc = mcc;
        }

        public String getMnc() {
            return mnc;
        }

        public void setMnc(String mnc) {
            this.mnc = mnc;
        }

        public int getLac() {
            return lac;
        }

        public void setLac(int lac) {
            this.lac = lac;
        }

        public int getCid() {
            return cid;
        }

        public void setCid(int cid) {
            this.cid = cid;
        }

        public int getRssi() {
            return rssi;
        }

        public void setRssi(int rssi) {
            this.rssi = rssi;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }
        public  void setTimeStamp(long ts) {this.timeStamp = ts;}
        public long getTimeStamp(){return this.timeStamp;}

        @Override
        public String toString() {
            return "CellInfo{" +
                    "mcc=" + mcc +
                    ", mnc=" + mnc +
                    ", lac=" + lac +
                    ", cid=" + cid +
                    ", rssi=" + rssi +
                    ", type='" + type + '\'' +
                    ", timeStamp=" + timeStamp +
                    '}';
        }
    }

    public ArrayList<BaseStation> lstBS;

    GSMTrack(Context context){
        this.mContext = context;
        lstBS = new ArrayList<>();
    }

    public void scanCellInfo(){
        telephonyManager = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
        List<CellInfo> cellInfoList = null;
        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
//        cellInfoList = telephonyManager.getAllCellInfo();
//        if(cellInfoList != null){
//            if(cellInfoList.size() > 0){
//                int cellNumber = cellInfoList.size();
//                for(int i = 0; i < cellInfoList.size(); i++){
//                    BaseStation bs = bindData((cellInfoList.get(i)));
//                    lstBS.add(bs);
//                    Log.d("GSM_LOG", i + ", " + bs.toString());
//                }
//            }
//        }

        TelephonyManager.CellInfoCallback cellInfoCallback = new TelephonyManager.CellInfoCallback() {
            @Override
            public void onCellInfo(List<CellInfo> cellInfo) {
                if(cellInfo != null){
                    lstBS.clear();
                    if(cellInfo.size() > 0){
                        for(int i = 0; i < cellInfo.size(); i++){
                            BaseStation bs = bindData((cellInfo.get(i)));
                            if(bs.getCid() != CellInfo.UNAVAILABLE) {
                                lstBS.add(bs); //Không lấy các CellID = UNAVAILABLE
                            }
                            Log.d("GSM_LOG", i + ", " + bs.toString());
                        }
                    }
                }
            }
        };
        telephonyManager.requestCellInfoUpdate(mContext.getMainExecutor(), cellInfoCallback);
    }

    private BaseStation bindData(CellInfo cellInfo) {
        BaseStation baseStation = null;
        //Check Cell type 2G，3G，4G
        if (cellInfo instanceof CellInfoWcdma) {
            //3G Cell
            CellInfoWcdma cellInfoWcdma = (CellInfoWcdma) cellInfo;
            CellIdentityWcdma cellIdentityWcdma = cellInfoWcdma.getCellIdentity();
            baseStation = new BaseStation();
            baseStation.setType("WCDMA");
            baseStation.setCid(cellIdentityWcdma.getCid());
            baseStation.setLac(cellIdentityWcdma.getLac());
            baseStation.setMcc(cellIdentityWcdma.getMccString());
            baseStation.setMnc(cellIdentityWcdma.getMncString());
            if (cellInfoWcdma.getCellSignalStrength() != null) {
                baseStation.setRssi(cellInfoWcdma.getCellSignalStrength().getDbm()); //Get the signal strength as dBm
            }
            baseStation.setTimeStamp(cellInfoWcdma.getTimeStamp());
        } else if (cellInfo instanceof CellInfoLte) {
            //4G
            CellInfoLte cellInfoLte = (CellInfoLte) cellInfo;
            CellIdentityLte cellIdentityLte = cellInfoLte.getCellIdentity();
            baseStation = new BaseStation();
            baseStation.setType("LTE");
            if (cellIdentityLte.getCi() != 0 && cellIdentityLte.getCi() != 2147483647)
            {
                int cellID = cellIdentityLte.getCi();
                baseStation.setCid(cellID);
            }

            baseStation.setMnc(cellIdentityLte.getMncString());
            baseStation.setMcc(cellIdentityLte.getMccString());
            baseStation.setLac(cellIdentityLte.getTac());
            if (cellInfoLte.getCellSignalStrength() != null) {
                baseStation.setRssi(cellInfoLte.getCellSignalStrength().getDbm());
            }
            baseStation.setTimeStamp(cellInfoLte.getTimeStamp());

        } else if (cellInfo instanceof CellInfoGsm) {
            //2G (GSM)
            CellInfoGsm cellInfoGsm = (CellInfoGsm) cellInfo;

            CellIdentityGsm cellIdentityGsm = cellInfoGsm.getCellIdentity();
            baseStation = new BaseStation();
            baseStation.setType("GSM");
            baseStation.setCid(cellIdentityGsm.getCid());
            baseStation.setLac(cellIdentityGsm.getLac());
            baseStation.setMcc(cellIdentityGsm.getMccString());
            baseStation.setMnc(cellIdentityGsm.getMncString());
            if (cellInfoGsm.getCellSignalStrength() != null) {
                baseStation.setRssi(cellInfoGsm.getCellSignalStrength().getDbm());
            }
            baseStation.setTimeStamp(cellInfoGsm.getTimeStamp());
        } else {
            //2/3G
        }
        return baseStation;
    }

}
