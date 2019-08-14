package com.lxfly2000.acfunget;

import com.lxfly2000.utilities.AndroidDownloadFileTask;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

public class Common {
    public static void SetAcFunHttpHeader(AndroidDownloadFileTask task){
        task.SetUserAgent("Mozilla/5.0 (X11; Linux x86_64; rv:64.0) Gecko/20100101 Firefox/64.0");
        task.SetAcceptCharset("UTF-8,*;q=0.5");
        task.SetAcceptEncoding("gzip,deflate,sdch");
        task.SetAccept("text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        task.SetAcceptLanguage("en-US,en;q=0.8");
    }

    //参考：https://github.com/soimort/you-get/blob/develop/src/you_get/common.py#L155
    public static byte[] RC4(byte[]key,byte[]data){
        int[]state=new int[256];
        for(int i=0;i<state.length;i++)
            state[i]=i;
        int i=0,j=0;
        for(i=0;i<256;i++){
            j+=state[i]+key[i%key.length];
            j&=0xFF;
            int t=state[j];
            state[j]=state[i];
            state[i]=t;
        }

        i=0;
        j=0;
        ByteArrayOutputStream output=new ByteArrayOutputStream();
        for(byte ch:data){
            i+=1;
            i&=0xFF;
            j+=state[i];
            j&=0xFF;
            int t=state[j];
            state[j]=state[i];
            state[i]=t;
            int prn=state[(state[i]+state[j])&0xFF];
            output.write(ch^prn);
        }
        try{
            output.flush();
        }catch (IOException e){/*Ignore*/}
        return output.toByteArray();
    }
}
