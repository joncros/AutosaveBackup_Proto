# AutosaveBackup_Proto #
This is a command-line program designed to watch a folder for a specified file and back up that file whenever it is created or modified.

## Usage ##
[ ] TODO

## Classes ##

#### AutosaveBackup ####
This is the main class. It parses the command line arguments and then launches two new program threads: one which uses the Watcher class to monitor the specified folder, and one which uses the ConsoleInputTask to wait for the user to ask the program to end by typing "quit" into the console.

It passes a CountDownLatch instance (with a count of 1) to both of these threads and then waits for the CountDownLatch to be signalled. Whichever thread completes first will call countDown() on the CountDownLatch. This prompts AutosaveBackup to shut down the remaining thread and exit.

#### ConsoleInputTask ####
This class opens a BufferedReader to listen for input from the console the program was opened from. It runs in a separate thread to prevent blocking the rest of the program. 

It calls the BufferedReader ready() method to check if anything has been typed into the console. If nothing was typed, it sleeps for 1 second and then calls ready() again, repeating until something is typed into the console. If there is input to the console  matching the string "quit," it will signal the main thread using the CountDownLatch and then return.

#### Watcher ####

#### Backup ####
