package com.company;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class ParallelDownloader implements Downloader {

    private static final int BUFFER_SIZE = 8192;

    private final String urlStr;
    private final String savedFileName;
    private final int sizeOfChunk;
    private final int maxDownloadChunk;
    private final long maxDownloadSize;
    private final DownloaderListener listener;

    private final List<DowloadThread> downloadThreadList;


    public ParallelDownloader(
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
        downloadThreadList = new ArrayList<>();
    }

    @Override
    public void download() {

        long downloadOffset = 0;
        int downloadChunks = 0;
        long remaining = maxDownloadSize;

        while (downloadChunks < maxDownloadChunk && downloadOffset < maxDownloadSize) {
            int realChunkSize = (int)((remaining > sizeOfChunk) ? sizeOfChunk : remaining);

            DowloadThread downloadThread = new DowloadThread(
                    Integer.toString(downloadChunks),
                    urlStr,
                    savedFileName,
                    realChunkSize,
                    downloadOffset);
            downloadThreadList.add(downloadThread);
            downloadOffset += realChunkSize;
            remaining -= realChunkSize;
            downloadThread.start();
            downloadChunks++;
        }

        for (DowloadThread thread : downloadThreadList) {
            thread.waitFinish();
        }

        listener.onDownloadSuccess(downloadOffset);
    }

    private void onError() {
        listener.onDownloadFail();
    }

    private class DowloadThread implements Runnable {

        private Thread thread;
        private String urlStr;
        private String savedFileName;
        private int sizeOfChunk;
        private long downloadedOffset;

        public DowloadThread(String threadID, String urlStr, String savedFileName, int sizeOfChunk, long downloadedOffset) {
            this.thread = new Thread(this, threadID);
            this.urlStr = urlStr;
            this.savedFileName = savedFileName;
            this.sizeOfChunk = sizeOfChunk;
            this.downloadedOffset = downloadedOffset;
        }

        public void start() {
            thread.start();
        }

        @Override
        public void run() {

            RandomAccessFile savedFile = null;
            BufferedInputStream in =  null;

            int dowloadSize = 0;

            try {
                savedFile = new RandomAccessFile(savedFileName, "rw");
                savedFile.seek(downloadedOffset);

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
                in = new BufferedInputStream(conn.getInputStream());
                in.skip(downloadedOffset);

                byte data[] = new byte[BUFFER_SIZE];
                int numRead;
                int remaining = sizeOfChunk;
                while((remaining > 0) && ((numRead = in.read(data,0, remaining > BUFFER_SIZE ? BUFFER_SIZE : remaining)) != -1))
                {
                    // write to buffer
                    savedFile.write(data,0, numRead);
                    dowloadSize += numRead;
                    remaining -= numRead;
                }

            } catch (IOException e) {
                onError();
            } finally {
                if (savedFile != null) {
                    try {
                        savedFile.close();
                    } catch (IOException e) {}
                }

                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {}
                }
            }
        }

        public void waitFinish() {
            try {
                thread.join();
            } catch (InterruptedException e) {
            }
        }
    }
}
