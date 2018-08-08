/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.joncros.github.autosavebackup_proto;

import java.nio.file.Path;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * 
 * @author Jonathan Croskell
 */
class Backup {
    private static final Logger logger = LogManager.getLogger();
    
    /**
     * Write a copy of a file to the directory it is located in
     * @param path java.nio.file.Path Location of the file
     */
    void write(Path path) {
        logger.traceEntry("path: {}", path);
        //todo
        logger.traceExit();
    }
    
    private static String generateBackupFileName(Path path, String originalFileName) {
        logger.traceEntry("path: {}, original filename: {}", path, originalFileName);
        //todo
        return "";
    }
}
