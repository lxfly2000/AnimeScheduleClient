package com.lxfly2000.animeschedule;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;
import androidx.test.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;
import com.lxfly2000.utilities.AndroidSysDownload;
import com.lxfly2000.utilities.AndroidUtility;
import com.lxfly2000.utilities.FileUtility;
import com.lxfly2000.utilities.StreamUtility;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class SysDownloadTest {
    String localAnimeJS;
    private CountDownLatch lock=new CountDownLatch(1);
    @Test
    public void DownloadFile()throws Exception{
        Context context= InstrumentationRegistry.getTargetContext();
        localAnimeJS= StreamUtility.GetStringFromStream(context.getResources().openRawResource(R.raw.anime));
        AndroidSysDownload sysDownload=new AndroidSysDownload(context);
        String path=Values.GetRepositoryPathOnLocal()+"/test_anime.js";
        if(FileUtility.IsFileExists(path)){
            Log.d("SysDownloadTest","删除已存在的测试文件");
            FileUtility.DeleteFile(path);
        }
        long did=sysDownload.StartDownloadFile(Values.urlAuthor+"/raw/master/app/src/main/res/raw/anime.js",path,"anime.js");
        Log.d("SysDownloadTest", "开始下载");
        sysDownload.SetOnDownloadFinishReceiver(new AndroidSysDownload.OnDownloadCompleteFunction() {
            @Override
            public void OnDownloadComplete(long downloadId) {
                if(downloadId==did){
                    Log.d("SysDownloadTest","下载完毕");
                    assertEquals(localAnimeJS.replaceAll("\\r\\n","\n"),FileUtility.ReadFile(path));
                    lock.countDown();
                }
            }
        });
        //https://stackoverflow.com/a/1829949
        lock.await();
        assertTrue(sysDownload.IsDownloadIdExists(did));
    }
}
