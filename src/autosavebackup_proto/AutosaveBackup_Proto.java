/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package autosavebackup_proto;

import java.nio.file.*;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.NameFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;

/**
 * Simple command-line application to monitor a folder for game Autosaves and 
 * create backups of them.
 * @author Jonathan Croskell
 */
public class AutosaveBackup_Proto {
    IOFileFilter filter;
    
    /**
     * @param args the command line arguments
     * First argument (required) is the path to the folder to watch
     * Second argument (optional) is the file name to watch for
     */
    public static void main(String[] args) {
        // TODO code application logic here
        
        // args empty or greater than 2: print usage text
        
        // set path to first arg
        
        // if second arg doesn't exist, make filter a TrueFileFilter
        // otherwise, create NameFileFilter from second arg
        
        // register WatchKey from path, watching create and modify events
        // while key is valid
            //for each event
                //if context matches filter
                    //wait for file modify to complete [FileCnannel lock()?]
                    //backup file [Backup.write()]
    }
    
}
