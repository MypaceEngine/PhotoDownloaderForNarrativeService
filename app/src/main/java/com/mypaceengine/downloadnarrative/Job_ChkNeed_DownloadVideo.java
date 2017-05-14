package com.mypaceengine.downloadnarrative;

import com.google.gdata.data.photos.AlbumEntry;

import org.json.JSONObject;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Iterator;

/**
 * Created by MypaceEngine on 2017/05/13.
 */

public class Job_ChkNeed_DownloadVideo extends Job_Google_Abstract{

    String videoInfo=null;

    void setInfo(String _videoInfo){
        videoInfo=_videoInfo;
    }

    void run(){
        boolean download=false;
        try {
            JSONObject videoObj = new JSONObject(videoInfo);
            String uuid_video = videoObj.getString("uuid");
            String takeTime = videoObj.getString("taken_at_local");
            Calendar cal = CnvUtil.cnvCalender(takeTime);
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

            SimpleDateFormat sdf_Album = new SimpleDateFormat("yyyyMMdd");
            SimpleDateFormat sdf_Date = new SimpleDateFormat("yyyy/MM/dd");
            String gAlbumName = "NarrativeClip_" + sdf_Album.format(cal.getTime());
            String gFileName = "NarrativeClip_Video_" + uuid_video;
            AlbumEntry album =null;
            if (dataUtil.getEnableGoogleSync()) {
                album = getAlbumAndCreate(gAlbumName, sdf_Date.format(cal.getTime()));
                download = !isAlreadyUpload(album.getGphotoId(), gFileName);
            }

            File movetoTarget = CnvUtil.cnvFilePath(service.getApplicationContext(), Conf.PhotoFolderName, cal, format);
            if(!download) {

                if (dataUtil.getEnableLocalSync()) {
                    download = !movetoTarget.exists();
                }
            }
            if(download){
                Job_DownloadVideo job=new Job_DownloadVideo();
                job.setInfo(videoInfo);
                service.addJob(job);
            }
            service.removeJob(this);
        }catch (Exception ex){}
    }
}
