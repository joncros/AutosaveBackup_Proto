/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.joncros.github.autosavebackup_proto;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Waits for console input without blocking. Prompts program shutdown when
 * "quit" is typed.
 * @author Jonathan Croskell
 */
public class ConsoleInputTask implements Callable<String> {
    private static final Logger logger = LogManager.getLogger();
    private final CountDownLatch countDownLatch;
    
    /**
     * 
     * @param countDownLatch Used to signal other thread(s) when this task exits.
     */
    public ConsoleInputTask(CountDownLatch countDownLatch) {
        this.countDownLatch = countDownLatch;
    }
    
    @Override
    public String call() throws IOException {
        logger.traceEntry();
        String input = null;
        System.out.println("Type \"quit\" to exit");
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(System.in))) {
            while (true) {
                while (!br.ready()) {
                    Thread.sleep(1000);
                }
                input = br.readLine();
                if("quit".equals(input)) {
                    countDownLatch.countDown();
                    logger.traceExit(input);
                    return input;
                }
            }
        }
        catch (InterruptedException e) {
            logger.traceExit("Interrupted");
            return null;
        }
        catch (IOException e) {
            countDownLatch.countDown();
            throw e;
        }
    }
}
