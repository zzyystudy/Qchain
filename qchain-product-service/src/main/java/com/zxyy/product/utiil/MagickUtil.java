package com.zxyy.product.utiil;


import org.apache.commons.exec.CommandLine;

import java.io.InputStream;
import java.io.OutputStream;

public class MagickUtil {
    public static void processImage(InputStream inputStream, String q,
                                 String w, String h, OutputStream outputStream, String outputFormat) {

        // 1. 构建命令
        CommandLine cmdLine = new CommandLine("magick");
        cmdLine.addArgument("-", false);               // stdin
        cmdLine.addArgument("-thumbnail", false);
        cmdLine.addArgument("1200x1200>", false);
        cmdLine.addArgument("-quality", false);
        cmdLine.addArgument("80", false);
        cmdLine.addArgument("webp:-", false);           // stdout

        // 2. 执行：inputStream → magick进程 → outputStream
        CommandUtil.execCommand(cmdLine, inputStream, outputStream);

    }

}