/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.callable;

import java.util.concurrent.ThreadFactory;

/**
 *
 * @author david
 */
public class CronJobCallableThreadFactory implements ThreadFactory {

    private final ThreadGroup tGroup1 = new ThreadGroup("CallableExecutor");

    @Override
    public Thread newThread(Runnable r) {
        return new Thread(tGroup1, r, "CronJob-Callable-Executors");
    }
}
