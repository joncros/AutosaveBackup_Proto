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
    Boolean debug = true;
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
            while ((key = watchService.take()) != null) {
                for (WatchEvent<?> event : key.pollEvents()) {
                    if (event.kind() == StandardWatchEventKinds.OVERFLOW) {
                        logger.warn("WatchService overflow event");
                        continue;
                    }
                    
                    logger.debug("WatchEvent file: {}, kind: {}"
                            , event.context(), event.kind());
                    File acceptedFile = null;
                    Path filePath = folder.resolve((Path) event.context());
                    if (acceptedFile == null && filter.accept(filePath.toFile())) {
                        acceptedFile = filePath.toFile();
                        logger.info("File accepted by filter: {}", acceptedFile);
                        //wait for acceptedFile modify to complete
                        FileChannel channel = null;
                        while (channel == null) {
                            try {
                                channel = new RandomAccessFile(acceptedFile, "rw").getChannel();
                            }
                            catch (IOException e) {
                                //still waiting for access to acceptedFile, do nothing
                                Thread.sleep(500);
                            }
                        }
                        channel.lock();
                        logger.trace("File lock acquired");
                        Backup.write(filePath);
                        channel.close();
                    }
                }
                key.reset();
            }
        } 
        catch (IOException | InterruptedException | ClosedWatchServiceException e) {
            //todo exit on ioexception?
            //todo don't log InterruptedException when user types quit?
            logger.catching(e);
        }
    }
}
