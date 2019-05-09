package com.company;

public class Main implements DownloaderListener{

    public static int DEFAULT_CHUNK_SIZE = 1024 * 1024;
    public static int DEFAULT_MAX_NUM_OF_CHUNK = 4;
    public static long DEFAULT_MAX_DOWNLOAD_SIZE = DEFAULT_MAX_NUM_OF_CHUNK * DEFAULT_CHUNK_SIZE;

    public static void main(String[] args) {

        if (args.length < 1) {
            printHelpInfo();
        }

        String url = "";
        String savedFile = "test";
        int numOfChunk = DEFAULT_MAX_NUM_OF_CHUNK;
        int sizeOfChunk = DEFAULT_CHUNK_SIZE;
        long totalDownloadSize = DEFAULT_MAX_DOWNLOAD_SIZE;
        boolean isParallel = false;

        for (int i = 0; i < args.length; i++) {
            switch (args[i].charAt(0)) {
                case '-':
                    if (args[i].length() < 2) {
                        printHelpInfo();
                    } else {
                        if (args[i].substring(1).equalsIgnoreCase("o")) {
                            savedFile = args[i + 1];
                            i++;
                        }
                        else if (args[i].substring(1).equalsIgnoreCase("parallel")) {
                            isParallel = true;
                        }
                        else if (args[i].substring(1).equalsIgnoreCase("s")) {
                            sizeOfChunk = Integer.valueOf(args[i+1]);
                            i++;
                        }
                        else if (args[i].substring(1).equalsIgnoreCase("n")) {
                            numOfChunk = Integer.valueOf(args[i+1]);
                            i++;
                        }
                        else if (args[i].substring(1).equalsIgnoreCase("t")) {
                            totalDownloadSize = Long.valueOf(args[i+1]);
                            i++;
                        }
                        else {
                            printHelpInfo();
                        }
                    }
                    break;
                default:
                    // arg
                    url = args[i];
                    break;
            }
        }

        if (url.isEmpty()) {
            printHelpInfo();
        }

        if (!isParallel) {
            SerialDownloader downloader = new SerialDownloader(url, savedFile, sizeOfChunk, numOfChunk, totalDownloadSize, new Main());
            downloader.download();
        } else {
            ParallelDownloader downloader = new ParallelDownloader(url, savedFile, sizeOfChunk, numOfChunk, totalDownloadSize, new Main());
            downloader.download();
        }

    }

    private static void printHelpInfo() {
        System.err.println("Usage: java -jar Main.jar [OPTIONS] url");
        System.err.println("-o string Write output to <file> instead of default");
        System.err.println("-parallel Download chunks in parallel instead of sequentially");

        System.exit(1);
    }

    @Override
    public void onDownloadSuccess(long totalDownloadedSize) {
        System.out.println("Downloaded file size: " + totalDownloadedSize);
    }

    @Override
    public void onDownloadFail() {

    }
}
