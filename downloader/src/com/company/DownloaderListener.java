package com.company;

public interface DownloaderListener {

    void onDownloadSuccess(long totalDownloadedSize);

    void onDownloadFail();
}
