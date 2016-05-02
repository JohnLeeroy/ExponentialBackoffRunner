package jli.com.exponentialbackoffrunner;

import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class ExponentialBackoffRunnerTest {

    ExponentialBackoffRunner mRunner;
    Runnable mRunnable;
    Runnable mSpyRunnable;

    int mInitialIntervalMs = 1000;
    int mMaxIntervalMs = 32000;
    int mResetIntervalMs = 1000;
    int mMultiplier = 2;

    @Before
    public void setup() {
        mRunnable = new Runnable() {
            @Override
            public void run() {
                long time = System.currentTimeMillis()/1000;
                System.out.println(time + " BackoffRunnerTest: I ran! ");
            }
        };
        mSpyRunnable = spy(mRunnable);
        mRunner = new ExponentialBackoffRunner.Builder()
                .setInitialIntervalMs(mInitialIntervalMs)
                .setMaxIntervalMs(mMaxIntervalMs)
                .setMultiplier(mMultiplier)
                .setResetIntervalMs(mResetIntervalMs)
                .setResetIntervalMs(1000)
                .build();
    }

    @Test
    public void runAndCheckIntervals() {
        final int totalRuns = 6;
        final int[] runCounter = {0};
        final long[] startTime = {System.currentTimeMillis()};
        final int elapsedTimeDiffThreshold = 100; //allow for 100ms of error

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                long timeDiff = now - startTime[0];
                long expectedElapsedTime = getExpectedInterval(runCounter[0]);
                System.out.println(String.format("Run %d - Elapsed Time: %d", runCounter[0], timeDiff));
                System.out.println(String.format("Run %d - Expected Elapsed Time: %d", runCounter[0],  (expectedElapsedTime)));

                //Compare the expected time and the time difference.
                assertTrue(Math.abs(timeDiff - expectedElapsedTime) < elapsedTimeDiffThreshold);

                //Update Start time to get a correct elapsed time
                startTime[0] = now;
                runCounter[0]++;
            }
        };

        //Run it [totalRuns] amount of time
        mSpyRunnable = spy(runnable);
        for(int i = 0; i < totalRuns; i++) {
            mRunner.run(mSpyRunnable);
        }
        verify(mSpyRunnable, times(totalRuns)).run();

        //Sleep and check if current back off timer is reset
        try {
            Thread.sleep(mRunner.getCurrentBackOffTimeMs() + mResetIntervalMs);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //Test that the current backoff time has reset
        assertEquals(mInitialIntervalMs, mRunner.getCurrentBackOffTimeMs());
    }

    private long getExpectedInterval(int runCount) {
        long totalTime = mInitialIntervalMs;
        for(int i = 0; i < runCount; i++) {
            totalTime *= mMultiplier;
        }

        if(totalTime > mMaxIntervalMs) {
            totalTime = mMaxIntervalMs;
        }
        return totalTime;
    }
}
