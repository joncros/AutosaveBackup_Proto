/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template acceptedFile, choose Tools | Templates
 * and open the template in the editor.
 */
package com.joncros.github.autosavebackup_proto;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Task that uses a WatchService to monitor a folder for a specific file, 
 * using the Backup class to back up the file whenever it is modified.
 * @author Jonathan Croskell
 */
class Watcher implements Callable<Void> {
    private static final Logger logger = LogManager.getLogger();
    private final Path folder;
    private final IOFileFilter filter;
    private final CountDownLatch countDownLatch;
    
    /**
     * 
     * @param folder Folder to watch
     * @param filter org.apache.commons.io.filefilter.IOFileFilter which will 
     * accept the file to be monitored.
     * @param countDownLatch Signalled if the thread exits due to an IOException, 
     * prompting the main thread to execute shutdown.
     */
    Watcher(Path folder, IOFileFilter filter, CountDownLatch countDownLatch) {
        logger.traceEntry("folder: {}, FileFilter: {}", folder.toAbsolutePath(), filter);
        assert(Files.isDirectory(folder));
        this.folder = folder;
        this.filter = filter;
        this.countDownLatch = countDownLatch;
        logger.traceExit();
    }
    
    @Override
    public Void call() throws InterruptedException, IOException {
        logger.traceEntry();
        try (WatchService watchService = FileSystems.getDefault().newWatchService()) {
            WatchKey watchKey = folder.register(
                watchService, 
                StandardWatchEventKinds.ENTRY_MODIFY);
            logger.info("Watchservice started");
            
            
            /* 
             * boolean set to true upon first matching modify event, prevents 
             * Watcher from responding to duplicate modify events 
             */
            boolean fileModified = false;
            
            //Listen for events
            for(;;) {
                WatchKey key = watchService.take();
                for (WatchEvent<?> event : key.pollEvents()) {
                    if (event.kind() == StandardWatchEventKinds.OVERFLOW) {
                        logger.warn("WatchService overflow event");
                        continue;
                    }
                    
                    logger.debug("WatchEvent file: {}, kind: {}",
                            event.context(), event.kind());
                    Path filePath = folder.resolve((Path) event.context());
                    if (!fileModified && filter.accept(filePath.toFile())) {
                        fileModified = true;
                        logger.info("File accepted by filter: {}", filePath);
                        
                        Backup.write(filePath);
                        
                        /* 
                        * Avoid responding to extra MODIFY events by flushing 
                        * additional events that occured between the filter
                        * accepting the file and the file backup completing.
                        */
                        key.pollEvents();
                        fileModified = false;
                    }
                }
                key.reset();
                logger.trace("Watchkey reset");
            }
        } 
        catch (IOException e) {
            countDownLatch.countDown();
            logger.traceExit();
            throw e;
        }
    }
}
