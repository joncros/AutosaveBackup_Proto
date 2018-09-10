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
 * Simple command-line application to monitor a folder for game Autosaves and 
 * create backups of them.
 * @author Jonathan Croskell
 */
public class AutosaveBackup {
    private static final Logger logger = LogManager.getLogger();
    
    /**
     * @param args the command line arguments
     * First argument (required) is the path to the folder to watch
     * Second argument is the file name to watch for
     */
    public static void main(String[] args) {
        Path folder;
        IOFileFilter filter;
        ExecutorService es;
        CountDownLatch countDownLatch;
        String usage = "\nusage: AutosaveBackup [path to folder] [filename]\n";
      
        logger.trace("arguments: {}", Arrays.toString(args));
        try {
            if (args.length < 1 
                    || args.length > 2
                    || !Files.isDirectory(Paths.get(args[0]))) {
                System.out.print(usage);
                return;
            }    
        }
        catch (InvalidPathException e) {
            System.out.print("\nPath to folder is invalid");
            System.out.print(usage);
            return;
        }
        
        es = Executors.newFixedThreadPool(2);
        countDownLatch = new CountDownLatch(1);
        try {
            folder = Paths.get(args[0]);
            filter = new NameFileFilter(args[1], IOCase.INSENSITIVE);

            //Start a Watcher in es
            //es.submit(new Watcher(folder,filter));
            Future<?> watcherFuture = 
                    es.submit(new Watcher(folder, filter, countDownLatch));
            

            //Start a ConsoleInput in es
            es.submit(new ConsoleInputTask(countDownLatch));
            
            countDownLatch.await();
            //Pause to make sure task that called countDownLatch.countDown() 
            //has completed
            Thread.sleep(500);
            if (watcherFuture.isDone()) {
                //get exception thrown by Watcher
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
    }
    
    /* Logic if filename is optional
                if (args.length == 1) filter = TrueFileFilter.TRUE;
                else filter = new NameFileFilter(args[1], IOCase.INSENSITIVE);
                */
                
                //TODO possibly logic for multiple named save files to back up
                //TODO possibly logic for wildcard in save file name(s)
    
}
