/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.joncros.github.autosavebackup_proto;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import org.apache.commons.io.filefilter.IOFileFilter;

/**
 *
 * @author Jonathan Croskell
 */
class Watcher implements Runnable {
    WatchService watchService;
    Boolean debug = true;
    Path folder;
    IOFileFilter filter;
    
    Watcher(Path folder, IOFileFilter filter) {
        this.folder = folder;
        this.filter = filter;
    }
    
    public void run() {
        //todo Determine if I need to check for thread interrupt and cleanup at any point
        try {
            // Initialize WatchService
            watchService = FileSystems.getDefault().newWatchService();
            WatchKey watchKey = folder.register(
                watchService, 
                StandardWatchEventKinds.ENTRY_CREATE, 
                StandardWatchEventKinds.ENTRY_MODIFY);
                
            //Listen for events
            WatchKey key;
            while ((key = watchService.take()) != null) {
                for (WatchEvent<?> event : key.pollEvents()) {
                    if (event.kind() == StandardWatchEventKinds.OVERFLOW) {
                        if (debug) {
                            System.out.println("WatchService overflow");
                        }
                        continue;
                    }

                    Path filePath = folder.resolve((Path) event.context());
                    File file = filePath.toFile();
                    if (filter.accept(file)) {
                        System.out.println(
                                "File: " + event.context()
                                + ", Event:" + event.kind());
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
            return;
        }
    }
}
