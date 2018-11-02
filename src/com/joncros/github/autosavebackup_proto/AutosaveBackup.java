/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.joncros.github.autosavebackup_proto;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.Arrays;
import java.util.Scanner;
import java.util.StringJoiner;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import org.apache.commons.io.IOCase;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.NameFileFilter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Simple command-line application (for use with Windows applications that make 
 * a limited number of autosaves) to backup autosaves. Monitors a folder for a 
 * single autosave or other file with a specific filename, creating a backup 
 * each time the file is modified.
 * @author Jonathan Croskell
 */
public class AutosaveBackup {
    private static final Logger logger = LogManager.getLogger();
    private final Path folder;
    private final IOFileFilter filter;
    private static final String DESCRIPTION = 
            "Watches for a specific file at the specified location, backing it "
            + "up (without overwriting previous backups) in the same folder using"
            + " the pattern [original filename]_copyN.[original extension], where N"
            + " is a whole number. Highest numbered copy is the most recent one.";
    private static final String USAGE =
            "\nusage: AutosaveBackup [file]"
            + "\n where [file] is the location and filename (including extension"
            + " of the file to monitor, i.e., c:\\game\\saves\\save1.sav "
            + "(spaces are allowed in the file path; do not surround the path "
            + "with quotation marks).";
    
    
    /**
     * 
     * @param folder Must be a folder that exists
     * @param filename String, cannot be null or empty
     */
    public AutosaveBackup (Path folder, String filename) {
        if (!Files.isDirectory(folder)) {
            throw new IllegalArgumentException("Path does not point to an "
                    + "existing directory");
        }
        this.folder = folder;
        if (filename.isEmpty()) {
            throw new IllegalArgumentException("filename string is empty");
        }
        this.filter = new NameFileFilter(filename, IOCase.INSENSITIVE);
    }
    
    /**
     * @param args the command line arguments
     * args is the full path to the file to watch for
     */
    public static void main(String[] args) {
        logger.trace("arguments: {}", Arrays.toString(args));
        
        if (args.length == 0) {
            System.out.println(DESCRIPTION);
            System.out.println(USAGE);
            return;
        }
        
        //parse the folder and filename from args
        String argsString = argsAsSingleString(args);
        int separatorPosition = argsString.lastIndexOf(File.separatorChar);
        Path folder = Paths.get(argsString.substring(0, separatorPosition));
        String filename = 
                argsString.substring(separatorPosition+1, argsString.length() -1);
                
        AutosaveBackup ab = new AutosaveBackup(folder, filename);
        ab.start();
    }
    
    public void start() {
        logger.traceEntry();
        ExecutorService es = Executors.newFixedThreadPool(2);
        
        /* 
        * CountDownlatch passed to the Watcher thread and ConsoleInputTask. 
        * The thread that completes first uses countDownLatch to signal this 
        * thread, prompting it to proceed to cleanup and shut down es.
        */
        CountDownLatch countDownLatch = new CountDownLatch(1);
        
        try {
            //Start Watcher
            Future<?> watcherFuture = 
                    es.submit(new Watcher(folder, filter, countDownLatch));
            
            //Start listening to console
            es.submit(new ConsoleInputTask(countDownLatch));
            
            countDownLatch.await();
            
            /*
            * Pause to make sure task that called countDownLatch.countDown() 
            * has completed
            */
            Thread.sleep(500);
            if (watcherFuture.isDone()) {
                //get exception(s) thrown by Watcher
                watcherFuture.get();
            }
        }
        catch (InterruptedException e) {
            logger.catching(e);
        }
        catch (ExecutionException e) {
            logger.catching(e.getCause());
        }
        finally {
            logger.trace("main() finally reached");
            es.shutdownNow();
        }
        logger.traceExit();
    }
    
    /**
     * Helper for processing command line args. Joins args into a single String 
     * from which a file path can be parsed.
     * @param args an array of Strings
     * @return a String
     */
    private static String argsAsSingleString(String[] args) {
        StringJoiner stringJoiner = new StringJoiner(" ");
        for (String s: args) {
            stringJoiner.add(s);
        }
        return stringJoiner.toString();
    }
}
