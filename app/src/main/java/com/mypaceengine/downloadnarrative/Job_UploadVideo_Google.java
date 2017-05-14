package com.mypaceengine.downloadnarrative;

import android.util.Log;

import com.google.gdata.data.photos.AlbumEntry;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Iterator;

/**
 * Created by MypaceEngine on 2017/05/13.
 */

public class Job_UploadVideo_Google extends Job_Google_Abstract{
    String tmpPath=null;
    String photoInfo=null;
    GSPContainer gpsData=null;
    public void setInfo(String _tmpPath,String _photoInfo,GSPContainer _gpsData){
        tmpPath=_tmpPath;
        photoInfo=_photoInfo;
        gpsData=_gpsData;
    }

    void run() {
        try {
            JSONObject videoObj = new JSONObject(photoInfo);
            String uuid_video = videoObj.getString("uuid");
            String takeTime = videoObj.getString("taken_at_local");
            String startTime = videoObj.getString("start_timestamp_local");
            String endTime = videoObj.getString("end_timestamp_local");
            boolean favorite=videoObj.getBoolean("favorite");
            String caption=videoObj.getString("caption");

            Calendar cal = CnvUtil.cnvCalender(takeTime);
            SimpleDateFormat sdf_Album = new SimpleDateFormat("yyyyMMdd");
            SimpleDateFormat sdf_Date = new SimpleDateFormat("yyyy/MM/dd");
            String gAlbumName = "NarrativeClip_" + sdf_Album.format(cal.getTime());
            String gFileName = "NarrativeClip_Video_" + uuid_video;
            AlbumEntry album = getAlbumAndCreate(gAlbumName, sdf_Date.format(cal.getTime()));

            File tmpFile = new File(tmpPath);

            Iterator ite = videoObj.keys();
            int width = 0;
            int height = 0;
            String photoUrl = null;
            String format = ".mp4";
            while (ite.hasNext()) {
                String size_key = (String) ite.next();
                JSONObject eachObj = videoObj.getJSONObject(size_key);
                JSONObject sizeObj = eachObj.getJSONObject("size");

                if (sizeObj != null) {
                    int photoWidth = sizeObj.getInt("width");
                    int photoHeight = sizeObj.getInt("height");
                    if (
                            (width < photoWidth) ||
                                    ((width == photoWidth) && (height < photoHeight))
                            ) {
                        width = photoWidth;
                        height = photoHeight;
                        photoUrl = eachObj.getString("url");
                        if (eachObj.has("format")) {
                            format = "." + eachObj.getString("format");
                        }
                    }
                }
            }
            File movetoTarget = CnvUtil.cnvFilePath(service.getApplicationContext(), Conf.PhotoFolderName, cal, format);
            //                    File tmpThumbnailFile =null;
//                    if(thumbnailUrl!=null) {
//                        tmpThumbnailFile=new File(tmp_BASEDIR.getAbsolutePath() + File.separator + "thumbnail.jpg");
//                        saveNarrativeSrv2File(thumbnailUrl, tmpThumbnailFile);
//                    }
            Log.d("PhotodownLoad", "DataFilePath:" + movetoTarget);
            if (dataUtil.getEnableGoogleSync() && (!this.isAlreadyUpload(album.getGphotoId(), gFileName))) {
                double lat = 0;
                double lon = 0;
                boolean available = false;
                String uuid = "";
                if (gpsData != null) {
                    lat = gpsData.getLatitude();
                    lon = gpsData.getLongitude();
                    available = gpsData.gpsAvailable;
                    uuid = gpsData.uuid;
                }
                String description =
                        "Narrative_UUID: " + uuid_video + "\n" +
                                "Narrative_Moment_UUID: " + uuid + "\n" +
                                "Narrative_StartTime: " + startTime + "\n" +
                                "Narrative_EndTime: " + endTime + "\n" +
                                "Narrative_Caption: " + caption + "\n" +
                                "Narrative_Favorite: " + favorite + "\n";

                long millis1 = cal.getTimeInMillis();
                tmpFile.setLastModified(millis1);
                this.upload(album.getGphotoId(), gFileName, description, tmpFile, "video/mp4", lat, lon, available, cal);
            }

            Log.d("PhotodownLoad", "DataFilePath:" + movetoTarget);
            if (dataUtil.getEnableLocalSync() && (!movetoTarget.exists())) {
                tmpFile.renameTo(movetoTarget);
            }
            try {
                tmpFile.deleteOnExit();
            } catch (Exception ex) {
            }
        }catch (Exception ex){}
    }
}
