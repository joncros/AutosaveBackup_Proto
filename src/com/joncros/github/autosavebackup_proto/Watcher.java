/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template acceptedFile, choose Tools | Templates
 * and open the template in the editor.
 */
package com.joncros.github.autosavebackup_proto;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import static java.nio.file.StandardOpenOption.WRITE;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author Jonathan Croskell
 */
class Watcher implements Runnable {
    private static final Logger logger = LogManager.getLogger();
    WatchService watchService;
    Path folder;
    IOFileFilter filter;
    
    Watcher(Path folder, IOFileFilter filter) {
        logger.traceEntry("folder: {}, FileFilter: {}", folder, filter);
        this.folder = folder;
        this.filter = filter;
        logger.traceExit();
    }
    
    @Override
    public void run() {
        //todo Determine if I need to check for thread interrupt and cleanup at any point
        try {
            // Initialize WatchService
            logger.traceEntry();
            watchService = FileSystems.getDefault().newWatchService();
            WatchKey watchKey = folder.register(
                watchService, 
                StandardWatchEventKinds.ENTRY_MODIFY);
            logger.info("Watchservice started");
            
            //Listen for events
            WatchKey key;
            Boolean fileModified = false;
            while ((key = watchService.take()) != null) {
                for (WatchEvent<?> event : key.pollEvents()) {
                    if (event.kind() == StandardWatchEventKinds.OVERFLOW) {
                        logger.warn("WatchService overflow event");
                        continue;
                    }
                    
                    logger.debug("WatchEvent file: {}, kind: {}"
                            , event.context(), event.kind());
                    Path filePath = folder.resolve((Path) event.context());
                    if (!fileModified && filter.accept(filePath.toFile())) {
                        fileModified = true;
                        logger.info("File accepted by filter: {}", filePath);
                        
                        //wait for modify to complete
                        while (true) {
                            try (FileChannel channel = 
                                    new RandomAccessFile(filePath.toFile(), "rw")
                                            .getChannel()){
                                break;
                            }
                            catch (IOException e) {
                                logger.trace("File modify still in progress...");
                                Thread.sleep(250);
                            }
                        }
                        
                        /* Flush additional events that occured between the 
                        FileFilter accepting the file and the file modification
                        completing */
                        logger.trace("File modify complete.");
                        key.pollEvents();
                        Backup.write(filePath);
                        fileModified = false;
                    }
                }
                key.reset();
                logger.trace("Watchkey reset");
            }
        } 
        catch (IOException | InterruptedException | ClosedWatchServiceException e) {
            //todo exit on ioexception?
            //todo don't log InterruptedException when user types quit?
            logger.catching(e);
        }
    }
}
