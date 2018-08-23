/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.joncros.github.autosavebackup_proto;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.commons.io.FilenameUtils;

/**
 * Provides a static method to write a copy of a file to the directory it exists
 * in. Does not overwrite previous copies. Uses the pattern 
 * [original filename}_copy[number].[original extension] and increments 
 * the number as necessary for a unique filename.
 * @author Jonathan Croskell
 */
class Backup {
    private static final Logger logger = LogManager.getLogger();
    
    /**
     * Write a copy of a file to the directory it is located in
     * @param path java.nio.file.Path Location of the file
     */
    static void write(Path path) {
        logger.traceEntry("path: {}", path);
        //todo exception for case where path points to a directory rather than file?
        Path folder = path.getParent();
        Path backupPath = generateBackupFilePath(folder, path.getFileName().toString());
        try {
            Files.copy(path,backupPath);
            logger.info("File {} backed up as {}", path, backupPath.getFileName());
        }
        catch (IOException e) {
            logger.catching(e);
        }
        logger.traceExit();
    }
    
    private static Path generateBackupFilePath(Path folder, String originalFilename) {
        logger.traceEntry("folder: {}, original filename: {}", folder, originalFilename);
        //get name and extension of original file
        String baseName = FilenameUtils.getBaseName(originalFilename) + "_copy";
        String extension = FilenameUtils.getExtension(originalFilename);
        int num = 1;
        boolean exists = true;
        Path outPath = Paths.get("");
        while (exists) {
            outPath = folder.resolve(baseName + num + "." + extension);
            if (Files.exists(outPath)) {
                num++;
                logger.trace("File {} exists, incrementing num to {}.", outPath, num);
            }
            else {
                exists = false;
            }
        }
        return logger.traceExit(outPath);
    }
}
