/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.joncros.github.autosavebackup_proto;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import org.apache.commons.io.IOCase;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.NameFileFilter;
//import org.apache.commons.io.filefilter.TrueFileFilter;

/**
 * Simple command-line application to monitor a folder for game Autosaves and 
 * create backups of them.
 * @author Jonathan Croskell
 */
public class AutosaveBackup {
    
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
        
        if (args.length < 1 || args.length > 2) System.out.print(usage);
        
        else {
            folder = Paths.get(args[0]);
            
            /* Logic if filename is optional
            if (args.length == 1) filter = TrueFileFilter.TRUE;
            else filter = new NameFileFilter(args[1], IOCase.INSENSITIVE);
            */
            
            filter = new NameFileFilter(args[1], IOCase.INSENSITIVE);
        
            try {
                // Initialize WatchService
                watchService = FileSystems.getDefault().newWatchService();
                WatchKey watchKey = folder.register(
                    watchService, 
                    StandardWatchEventKinds.ENTRY_CREATE, 
                    StandardWatchEventKinds.ENTRY_MODIFY);
                
                //Listen for events
                WatchKey key;
                while((key = watchService.take()) != null) {
                    for (WatchEvent<?> event : key.pollEvents()) {
                        if (event.kind() == StandardWatchEventKinds.OVERFLOW) { 
                            if (debug) System.out.println("WatchService overflow");
                            continue;
                        }
                        
                        Path filePath = folder.resolve((Path) event.context());
                        File file = filePath.toFile();
                        if (filter.accept(file)) {
                            System.out.println(
                                "File: " + event.context() +
                                ", Event:" + event.kind());
                        }
                        
                        //wait for file modify to complete [FileCnannel lock()?]
                        //backup file [Backup.write()]
                        
                    }
                    key.reset();
                }
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
    
}
