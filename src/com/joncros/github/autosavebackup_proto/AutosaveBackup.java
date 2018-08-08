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
        String filename;
        IOFileFilter filter;
        WatchService watchService;
        String usage = "usage: AutosaveBackup [path to folder] [filename]\n";
        Boolean debug = true;
        
        logger.trace("arguments: {}", Arrays.toString(args));
        if (args.length < 1 || args.length > 2) System.out.print(usage);
        
        else {
            folder = Paths.get(args[0]);
            
            /* Logic if filename is optional
            if (args.length == 1) filter = TrueFileFilter.TRUE;
            else filter = new NameFileFilter(args[1], IOCase.INSENSITIVE);
            */
            
            filter = new NameFileFilter(args[1], IOCase.INSENSITIVE);
            
            Thread watcherThread = new Thread(new Watcher(folder, filter));
            watcherThread.start();
            
            Scanner in = new Scanner(System.in);
            System.out.println("Enter \"quit\" to stop monitoring and exit");
            while (true) {
                String line = in.nextLine();
                if (line.trim().equalsIgnoreCase("quit")) watcherThread.interrupt();
            }
        }
    }
    
}
