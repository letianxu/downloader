package com.company;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;

public class SerialDownloader implements Downloader {

    private static final int BUFFER_SIZE = 8192;

    private final String urlStr;
    private final String savedFileName;
    private final int sizeOfChunk;
    private final int maxDownloadChunk;
    private final long maxDownloadSize;
    private final DownloaderListener listener;


    public SerialDownloader(
            String urlStr,
            String savedFileName,
            int sizeOfChunk,
            int maxDownloadChunk,
            long maxDownloadSize,
            DownloaderListener listener) {
        this.urlStr = urlStr;
        this.savedFileName = savedFileName;
        this.sizeOfChunk = sizeOfChunk;
        this.maxDownloadChunk = maxDownloadChunk;
        this.maxDownloadSize = maxDownloadSize;
        this.listener = listener;
    }

    @Override
    public void download() {

        RandomAccessFile savedFile = null;
        int totalChunk = 0;
        long totalDownloadSize = 0;

        try {
            savedFile = new RandomAccessFile(savedFileName, "rw");

            while (totalChunk < maxDownloadChunk && totalDownloadSize < maxDownloadSize) {
                URL url = new URL(urlStr);
                // open Http connection to URL
                HttpURLConnection conn = (HttpURLConnection)url.openConnection();
                // connect to server
                conn.connect();

                // Make sure the response code is in the 200 range.
                if (conn.getResponseCode() / 100 != 2) {
                    onError();
                }

                // get the input stream and seek to the downloaded position
                BufferedInputStream in = new BufferedInputStream(conn.getInputStream());
                in.skip(totalDownloadSize);

                byte data[] = new byte[BUFFER_SIZE];
                int numRead;
                int remaining = (int)(((maxDownloadSize - totalDownloadSize) > sizeOfChunk) ? sizeOfChunk : (maxDownloadSize - totalDownloadSize));
                while((remaining > 0) && ((numRead = in.read(data,0, remaining > BUFFER_SIZE ? BUFFER_SIZE : remaining)) != -1))
                {
                    // write to buffer
                    savedFile.write(data,0, numRead);
                    totalDownloadSize += numRead;
                    remaining -= numRead;
                }

                totalChunk++;

                in.close();
            }

        } catch (IOException e) {
            onError();
        } finally {
            if (savedFile != null) {
                try {
                    savedFile.close();
                    listener.onDownloadSuccess(totalDownloadSize);
                } catch (IOException e) {}
            }
        }
    }

    private void onError() {
        listener.onDownloadFail();
    }
}