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
 * Simple command-line application for Windows to monitor a folder for a single 
 * Autosave or other file with a specific filename, creating a backup each time
 * the file is modified.
 * @author Jonathan Croskell
 */
public class AutosaveBackup {
    private static final Logger logger = LogManager.getLogger();
    private final Path folder;
    private final IOFileFilter filter;
    private static final String USAGE = 
            "\nusage: AutosaveBackup [path to folder] [filename] will start the "
            + "program with the specified folder and filename\n";
    
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
     * First argument (required) is the path to the folder to watch.
     * Second argument is the file name to watch for.
     */
    public static void main(String[] args) {
        logger.trace("arguments: {}", Arrays.toString(args));
        
        Path folder = Paths.get("");
        String filename = "";
        boolean validInput = false;
        
        /* 
        * If two or more command line args provided and first arg is a valid 
        * path, obtain filename from remaining args.
        */
        if (args.length >= 2
                && (folder = validateFolderPath(args[0])) != null) {
            validInput = true;
            filename = args[1];
            
            // for case where spaces exist in the filename, so args.length > 2
            for (int i = 2; i < args.length; i++) {
                filename = filename + " " + args[i];
            }
        }
        else {
            System.out.print(USAGE);
        }
        while (!validInput) {
            Scanner in = new Scanner(System.in);
            System.out.println("Type the full path to the folder to monitor:");
            folder = validateFolderPath(in.nextLine());
            if (folder == null) {
                //didn't get a valid folder path, prompt for folder again
                continue;
            }
            System.out.println("Type the filename (including extension) of "
                + "the file to watch for");
            filename = in.nextLine();
            validInput = true;
        }
        
        //remove any ' or " charagers if user surrounded filename in quotes
        char first = filename.charAt(0);
        char last = filename.charAt(filename.length() - 1);
        if (first == '\'' || first == '\"'
                && 
                last == '\'' || last == '\"') {
            filename = filename.substring(1, filename.length()-1);
        }
        
        AutosaveBackup ab = new AutosaveBackup(folder, filename);
        ab.start();
    }
    
    public void start() {
        logger.traceEntry();
        ExecutorService es = Executors.newFixedThreadPool(2);
        
        /* 
        * Countdownlatch signalled when either the Watcher thread or
        * ConsoleInputTask thread has ended, prompting main to continue.
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
        catch (InterruptedException | ExecutionException e) {
            logger.trace("Interruption with following exception(s):");
            for (Throwable t: e.getCause().getSuppressed()) {
                logger.catching(t);
            }
        }
        finally {
            logger.trace("main() finally reached");
            es.shutdownNow();
        }
        logger.traceExit();
    }
    
    /*
    * Helper that 
    * @returns a Path corresponding to the String, or null if the Path does not
    * point to an existing folder or an InvalidPathException is encountered
    */
    private static Path validateFolderPath(String pathString) {
        try {
            Path path = Paths.get(pathString);
            if (Files.isDirectory(path)) {
                return path;
            }
            else {
                System.out.println("Error: location is not a directory or does"
                        + " not exist");
                return null;
            }
        }
        catch (InvalidPathException e) {
            System.out.println("Error: illegal characters in path.");
            return null;
        }
    }
}
