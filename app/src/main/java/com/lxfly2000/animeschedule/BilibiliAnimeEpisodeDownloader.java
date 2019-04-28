package com.lxfly2000.animeschedule;

import android.content.Context;
import android.widget.Toast;
import androidx.annotation.NonNull;
import com.lxfly2000.utilities.AndroidDownloadFileTask;
import com.lxfly2000.utilities.AndroidSysDownload;
import com.lxfly2000.utilities.FileUtility;
import com.lxfly2000.utilities.StreamUtility;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BilibiliAnimeEpisodeDownloader {
    private Context ctx;
    private AndroidSysDownload sysDownload;
    public int error=0;
    public BilibiliAnimeEpisodeDownloader(@NonNull Context context){
        ctx=context;
        sysDownload=new AndroidSysDownload(ctx);
    }

    private JSONObject jsonEntry,checkedEp;
    private String ssidString,epidString,avidString,cidString;
    private int videoQuality;

    public void DownloadEpisode(JSONObject jsonSeason, int indexEpisode, int videoQuality) {
        switch (videoQuality){
            case 100:videoQuality=16;break;
            case 150:videoQuality=32;break;
            case 200:videoQuality=64;break;
            case 400:videoQuality=80;break;
            case 800:videoQuality=112;break;
        }
        this.videoQuality=videoQuality;
        try {
            //写入entry.json文件
            jsonEntry = new JSONObject(BilibiliUtility.jsonRawBilibiliEntry);
            checkedEp = jsonSeason.getJSONArray("episodes").getJSONObject(indexEpisode);
            jsonEntry.put("is_completed", true);
            jsonEntry.put("total_bytes", 0);//下载所有分段后计算总大小
            jsonEntry.put("type_tag", BilibiliUtility.GetVideoQuality(videoQuality).tag);
            jsonEntry.put("title", jsonSeason.getString("title"));
            jsonEntry.put("cover", jsonSeason.getString("cover"));
            jsonEntry.put("prefered_video_quality", videoQuality);
            jsonEntry.put("danmaku_count", 3000);//需要在弹幕文件下载完成后修改此值
            jsonEntry.put("time_create_stamp", System.currentTimeMillis());
            jsonEntry.put("time_update_stamp", System.currentTimeMillis());
            ssidString=String.valueOf(jsonSeason.getInt("season_id"));
            jsonEntry.put("season_id", ssidString);
            JSONObject jsonSource = new JSONObject();
            jsonSource.put("av_id", checkedEp.getInt("aid"));
            jsonSource.put("cid", checkedEp.getInt("cid"));
            jsonSource.put("website", checkedEp.getString("from"));
            jsonEntry.put("source", jsonSource);
            jsonEntry.getJSONObject("ep").put("av_id", checkedEp.getInt("aid"));
            jsonEntry.getJSONObject("ep").put("page", checkedEp.getInt("page"));
            jsonEntry.getJSONObject("ep").put("danmaku", checkedEp.getInt("cid"));
            jsonEntry.getJSONObject("ep").put("cover", checkedEp.getString("cover"));
            jsonEntry.getJSONObject("ep").put("episode_id", checkedEp.getInt("ep_id"));
            jsonEntry.getJSONObject("ep").put("index", checkedEp.getString("index"));
            jsonEntry.getJSONObject("ep").put("index_title", checkedEp.getString("index_title"));
            jsonEntry.getJSONObject("ep").put("from", checkedEp.getString("from"));
            jsonEntry.getJSONObject("ep").put("season_type", jsonSeason.getInt("season_type"));
            epidString = String.valueOf(checkedEp.getInt("ep_id"));
            avidString=String.valueOf(checkedEp.getInt("aid"));
            cidString=String.valueOf(checkedEp.getInt("cid"));

            //下载danmaku.xml
            AndroidDownloadFileTask taskDownloadDanmaku = new AndroidDownloadFileTask() {
                @Override
                public void OnReturnStream(ByteArrayInputStream stream, boolean success, Object extra) {
                    String damakuPath = BilibiliUtility.GetBilibiliDownloadEpisodePath(ctx, ssidString, epidString) + "/danmaku.xml";
                    if (success) {
                        try {
                            String xmlString = StreamUtility.GetStringFromStream(stream);
                            FileUtility.WriteFile(damakuPath, xmlString);
                            Matcher matcher = Pattern.compile("<maxlimit>[0-9]+</maxlimit>").matcher(xmlString);
                            if (matcher.find()) {
                                xmlString = xmlString.substring(matcher.start(), matcher.end());
                                matcher = Pattern.compile("[0-9]+").matcher(xmlString);
                                if (matcher.find()) {
                                    int danmaku_maxlimit = Integer.parseInt(xmlString.substring(matcher.start(), matcher.end()));
                                    try {
                                        jsonEntry.put("danmaku_count", danmaku_maxlimit);
                                    } catch (JSONException e) {
                                        Toast.makeText(ctx, ctx.getString(R.string.message_json_exception, e.getLocalizedMessage()), Toast.LENGTH_LONG).show();
                                    }
                                }
                            }
                        } catch (IOException e) {
                            error=1;
                            Toast.makeText(ctx, ctx.getString(R.string.message_io_exception, e.getLocalizedMessage()), Toast.LENGTH_LONG).show();
                        }
                    } else {
                        error=1;
                        Toast.makeText(ctx, ctx.getString(R.string.message_download_failed, damakuPath), Toast.LENGTH_LONG).show();
                    }
                    DownloadEpisode_SaveEntryJson();
                }
            };
            taskDownloadDanmaku.execute("http://comment.bilibili.com/" + checkedEp.getInt("cid") + ".xml");
        } catch (JSONException e) {
            error=1;
            Toast.makeText(ctx, ctx.getString(R.string.message_json_exception, e.getLocalizedMessage()), Toast.LENGTH_LONG).show();
        }
    }

    private void DownloadEpisode_SaveEntryJson() {
        BilibiliQueryInfo queryInfo = new BilibiliQueryInfo(ctx);
        queryInfo.SetParam(ssidString, epidString, avidString, cidString, videoQuality);
        queryInfo.SetOnReturnEpisodeInfoFunction(new BilibiliQueryInfo.OnReturnEpisodeInfoFunction() {
            @Override
            public void OnReturnEpisodeInfo(BilibiliQueryInfo.EpisodeInfo info,boolean success) {
                if(!success){
                    error=1;
                    Toast.makeText(ctx,ctx.getString(R.string.message_cannot_fetch_bilibili_video_link),Toast.LENGTH_LONG).show();
                    return;
                }
                try {
                    jsonEntry.put("total_bytes", info.GetDownloadBytesSum());
                } catch (JSONException e) {
                    error=1;
                }
                FileUtility.WriteFile(BilibiliUtility.GetBilibiliDownloadEntryPath(ctx, ssidString, epidString), jsonEntry.toString());
                DownloadEpisode_Video(info);
            }
        });
        queryInfo.Query();
    }

    private void DownloadEpisode_Video(BilibiliQueryInfo.EpisodeInfo info){
        String episodeVideoQualityPath=BilibiliUtility.GetBilibiliDownloadEpisodePath(ctx,ssidString,epidString)+"/"+
                BilibiliUtility.GetVideoQuality(videoQuality);
        try {
            //写入index.json文件
            JSONObject jsonIndex = new JSONObject(BilibiliUtility.jsonRawBilibiliEpisodeIndex);
            jsonIndex.put("from", checkedEp.getString("from"));
            jsonIndex.put("type_tag", BilibiliUtility.GetVideoQuality(videoQuality).tag);
            jsonIndex.put("description", BilibiliUtility.GetVideoQuality(videoQuality).desc);
            //https://github.com/xiaoyaocz/BiliAnimeDownload/blob/852eb5b4fb3fdbd9801be2c6e98f69e3ed4d427a/BiliAnimeDownload/BiliAnimeDownload/MainPage.xaml.cs#L342
            jsonIndex.put("parse_timestamp_milli", System.currentTimeMillis());//当前时间戳（毫秒）
            JSONObject jsonSegment=jsonIndex.getJSONArray("segment_list").getJSONObject(0);
            for(int i=0;i<info.parts;i++) {
                jsonSegment.put("url", info.urls[i][0]);//视频URL
                jsonSegment.put("bytes", info.downloadBytes[i]);//分段的视频大小（字节数）
                if(info.urls[i].length>1){
                    for(int i_backup_url=1;i_backup_url<info.urls[i].length;i_backup_url++){
                        jsonSegment.getJSONArray("backup_urls").put(i_backup_url-1,info.urls[i][i_backup_url]);
                    }
                }else {
                    jsonSegment.remove("backup_urls");//如果没有其他URL则删除备用URL
                }
                jsonIndex.getJSONArray("segment_list").put(i,jsonSegment);

                //写入sum文件
                JSONObject sumFile = new JSONObject();
                sumFile.put("length", info.downloadBytes[i]);//分段的视频大小（字节数）
                FileUtility.WriteFile(episodeVideoQualityPath + "/"+i+".blv.4m.sum", sumFile.toString());

                //执行系统下载
                DownloadMultilinks(info.urls[i],0,info.downloadBytes[i], episodeVideoQualityPath + "/"+i+".blv", "[" + checkedEp.getString("index") + "] " +
                        checkedEp.getString("index_title")+" - "+i);
            }
            FileUtility.WriteFile(BilibiliUtility.GetBilibiliDownloadEpisodeIndexPath(ctx, ssidString, epidString, videoQuality), jsonIndex.toString());
        }catch (JSONException e){
            error=1;
            Toast.makeText(ctx, ctx.getString(R.string.message_json_exception, e.getLocalizedMessage()), Toast.LENGTH_LONG).show();
            return;
        }
        if(info.queryResult!=0){
            error=1;
            Toast.makeText(ctx,info.resultMessage,Toast.LENGTH_LONG).show();
        }
    }

    private void DownloadMultilinks(String[]links,int ilink,int expectSize,String localPath,String notifyTitle){
        if(ilink>=links.length)
            return;
        sysDownload.StartDownloadFile(links[ilink],localPath,notifyTitle);
        sysDownload.SetOnDownloadFinishReceiver(new AndroidSysDownload.OnDownloadCompleteFunction() {
            @Override
            public void OnDownloadComplete(long downloadId) {
                if(FileUtility.GetFileSize(localPath)<expectSize){
                    //说明下载过程中出错或下载失败
                    DownloadMultilinks(links,ilink+1,expectSize,localPath,notifyTitle);
                }else{
                    //下载完成后的动作
                    //暂时先什么都不用做
                }
            }
        });
    }
}
