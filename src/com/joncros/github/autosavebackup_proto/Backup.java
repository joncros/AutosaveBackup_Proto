/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.joncros.github.autosavebackup_proto;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Files;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.commons.io.FilenameUtils;

/**
 * Provides a method to write a copy of a file to the directory it exists in. 
 * Does not overwrite previous copies. Uses the pattern 
 * [original filename}_copy[number].[original extension] and increments 
 * the number as necessary for a unique filename.
 * @author Jonathan Croskell
 */
class Backup {
    private static final Logger logger = LogManager.getLogger();
    
    /**
     * Writes a copy of a file to the directory it is located in without 
     * overwriting the original file or any previous copies.
     * @param originalFile java.nio.file.Path Location of the file
     * @throws InterruptedException
     * @throws IOException 
     */
    static void write(Path originalFile) throws InterruptedException, IOException {
        logger.traceEntry("path: {}", originalFile);
        assert(Files.isRegularFile(originalFile));
        Path backupPath = generateBackupPath(originalFile);
        awaitModifyCompletion(originalFile);
        Files.copy(originalFile,backupPath);
        logger.info("File {} backed up as {}", originalFile, backupPath.getFileName());
        logger.traceExit();
    }
    
    /**
     * Generates a Path for copying a file without overwriting the file or  
     * existing copies of that file.
     * @param originalFile Path to original file
     * @return Path pointing to the same folder, with filename in the format
     * originalBaseName_copyN.originalExtension, where
     * originalBaseName is the filename referred to by originalFile 
     * (excluding the "." and extension) and N is an integer that is one more 
     * than the number of other copies that exist in the folder
     */
    private static Path generateBackupPath(Path originalFile) {
        logger.traceEntry("original file: {}", originalFile);
        
        //get name and extension of original file
        String originalName = originalFile.getFileName().toString();
        String baseName = FilenameUtils.getBaseName(originalName) + "_copy";
        String extension = FilenameUtils.getExtension(originalName);
        Path folder = originalFile.getParent();
        int num = 1;  //Postfix to apply to copy filename
        Path outPath;
        do {
            outPath = folder.resolve(baseName + num + "." + extension);
            logger.trace("Checking if file {} exists", outPath);
            num++; //postfix to use next if file at outPath exists
        } while (Files.exists(outPath));
        return logger.traceExit(outPath);
    }
    
    /**
     * Windows-specific helper that returns once the modification to the file
     * completes.
     * @param path
     * @throws InterruptedException
     * @throws IOException 
     */
    private static void awaitModifyCompletion(Path path) 
            throws InterruptedException, IOException {
        logger.traceEntry("File: {}", path);
        File file = path.toFile();
        while (true) {
            if (file.renameTo(file)) {  
                logger.traceExit("File modify complete.");
                return;
            }
            else {
                Thread.sleep(250);
            }
        }
    }
}
