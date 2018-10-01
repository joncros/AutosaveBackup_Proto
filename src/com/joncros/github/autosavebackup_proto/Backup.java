/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.joncros.github.autosavebackup_proto;

import java.io.IOException;
import java.nio.file.FileSystemException;
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
     * Writes a copy of a file to the directory it is located in without 
     * overwriting the original file or any previous copies. 
     * Uses a filename of the format originalBaseName_copyN.originalExtension, 
     * where N is an integer.
     * @param path java.nio.file.Path Location of the file
     */
    static void write(Path path) throws InterruptedException {
        logger.traceEntry("path: {}", path);
        //todo exception for case where path points to a directory rather than file?
        Path folder = path.getParent();
        Path backupPath = generateBackupPath(path);
        boolean copied = false;
        while (!copied) {
            try {
                Files.copy(path,backupPath);
                copied = true;
                logger.info("File {} backed up as {}", path, backupPath.getFileName());
            }
            catch (IOException e) {
                if (e.getMessage()
                    .contains("another process has locked a portion of the file.")
                        ||
                    e.getMessage().contains("being used by another process")) {
                    logger.trace("Copy failed, waiting...");
                    Thread.sleep(500);
                }
                else {
                    //todo consider whether to throw IOException
                    logger.catching(e);
                    return;
                }
            }
        }
        logger.traceExit();
    }
    
    /**
     * Generates a Path for copying a file without overwriting the file or  
     * previous copies of that file
     * @param originalPath Path to original file
     * @return Path pointing to the same folder, with filename in the format
     *      originalBaseName_copyN.originalExtension, 
     * where
     *      originalBaseName is the filename referred to by originalPath 
     *          (excluding the "." and extension)
     *      N is an integer that is one more than the number of other copies
     *          that exist in the folder
     */
    private static Path generateBackupPath(Path originalPath) {
        logger.traceEntry("original file: {}", originalPath);
        //get name and extension of original file
        String originalName = originalPath.getFileName().toString();
        String baseName = FilenameUtils.getBaseName(originalName) + "_copy";
        String extension = FilenameUtils.getExtension(originalName);
        Path folder = originalPath.getParent();
        int num = 1;  //Postfix to apply to copy filename
        Path outPath;
        do {
            outPath = folder.resolve(baseName + num++ + "." + extension);
            logger.trace("Checking if file {} exists", outPath);
        } while (Files.exists(outPath));
        return logger.traceExit(outPath);
    }
}
