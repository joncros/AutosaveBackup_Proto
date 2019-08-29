# AutosaveBackup_Proto #
This is a command-line program that watches a folder for a specified file and backs up that file whenever it is created or modified. Currently, it is designed to work only on a Windows OS.

## Usage ##
Download AutosaveBackup_Proto.jar from [the releases page](https://github.com/joncros/AutosaveBackup_Proto/releases "releases").
Then open a command prompt, navigate to the folder you downloaded the jar to and type

`java -jar AutosaveBackup.jar [path to file]`

Where `[path to file]` is the absolute path to the file you want backed up, such as `c:\save folder\save.sav.` Do not surround the file path with quotation marks.

When you want to stop watching for the file, type "quit" in the command prompt and then hit enter. Closing the command prompt will also stop the program.

## Classes ##

#### AutosaveBackup ####
This is the main class. It parses the command line arguments and then launches two new program threads: one which uses the `Watcher` class to monitor the specified folder, and one which uses the `ConsoleInputTask` to wait for the user to ask the program to end by typing "quit" into the console.

It passes a [CountDownLatch](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/CountDownLatch.html "CountDownLatch") instance (with a count of 1) to both of these threads and then waits for the CountDownLatch to be signalled. Whichever thread completes first will call `countDown()` on the CountDownLatch. This prompts AutosaveBackup to shut down the remaining thread and exit.

#### ConsoleInputTask ####
This class opens a BufferedReader to listen for input from the console the program was opened from. It runs in a separate thread to prevent blocking the rest of the program. 

It calls the BufferedReader `ready()` method to check if anything has been typed into the console. If nothing was typed, it sleeps for 1 second before calling `ready()` again, repeating until something is typed into the console. If there is input to the console matching the string "quit," it will signal the main thread using the CountDownLatch and then return.

#### Watcher ####
Starts a new [WatchService (package java.nio.file)](https://docs.oracle.com/javase/8/docs/api/java/nio/file/WatchService.html "WatchService") which watches the folder for ENTRY_MODIFY filesystem events. 

ENTRY_MODIFY events occur whenever an entry in the folder is modified. In the case of file creation, the filesystem would send an ENTRY_CREATE event followed by one or more ENTRY_MODIFY events as data is written to the new file. Thus, the Watcher will be notified of the creation of a new file provided something is also written to the file at that time.

When a modification event is detected, the Watcher checks the modified file and confirms whether it has the correct filename. If it does, the Watcher performs a backup using the Backup class.

The Watcher continues to monitor the folder unless an IOException occurs. This could happen, for example, if the folder is deleted. If this occurs, the Watcher catches the resulting IOException, signals the CountDownLatch, then rethrows the exception so it is available to the main thread. The main thread would then log the exception and shut down the ConsoleInputTask thread.

#### Backup ####
Contains the static method `write()` for backing up the file. `write()` constructs a filename for the backup by appending "_copy" followed by an integer to the original file name. The number is incremented to prevent overwritting previous backups. For example, subsequent backups of a file named "save.sav" would be named:
> save_copy1.sav\
> save_copy2.sav\
> save_copy3.sav

`write()` then copies the file once the file modification has completed. It confirms whether file modification is complete by attempting to rename the original file using `file.renameTo(file).` The renameTo operation would not change the filename of the original file; however, on a Windows system, this rename operation will fail (returning false) if the file is currently opened by another program. (Mac and Linux systems, however, allow renaming of a file even when it is opened).
