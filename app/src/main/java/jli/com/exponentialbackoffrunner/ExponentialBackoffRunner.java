package jli.com.exponentialbackoffrunner;


import android.os.Handler;

public class ExponentialBackoffRunner {

    public static class Builder {

        protected int mInitialIntervalTimeMs = 0;
        protected int mMaxIntervalMs = 1000;
        protected int mResetIntervalMs = 2000;
        protected float mMultiplier = 2f;

        public Builder setInitialIntervalMs(int initialIntervalMs) {
            mInitialIntervalTimeMs = initialIntervalMs;
            return this;
        }

        public Builder setMaxIntervalMs(int maxIntervalTimeMs) {
            mMaxIntervalMs = maxIntervalTimeMs;
            return this;
        }

        public Builder setResetIntervalMs(int maxElapsedIntervalMs) {
            mResetIntervalMs = maxElapsedIntervalMs;
            return this;
        }

        public Builder setMultiplier(float multiplier) {
            mMultiplier = multiplier;
            return this;
        }

        public ExponentialBackoffRunner build() {
            return new ExponentialBackoffRunner(mInitialIntervalTimeMs, mMaxIntervalMs, mResetIntervalMs, mMultiplier);
        }
    }

    protected int mInitialIntervalTimeMs;
    protected int mMaxIntervalMs;
    protected int mResetIntervalMs;
    protected float mMultiplier;
    protected int mCurrentBackOffTimeMs;

    Thread mResetThread;

    protected ExponentialBackoffRunner(int initialIntervalMs, int maxIntervalTimeMs,
                                     int resetIntervalMs, float multiplier) {
        mInitialIntervalTimeMs = initialIntervalMs;
        mMaxIntervalMs = maxIntervalTimeMs;
        mResetIntervalMs = resetIntervalMs;
        mMultiplier = multiplier;
        mCurrentBackOffTimeMs = mInitialIntervalTimeMs;
    }

    public synchronized void run(final Runnable runnable) {
        post(runnable, mCurrentBackOffTimeMs);
        startResetBackOffTimer();   // start or reset
        incrementBackOffTime();
    }

    private void startResetBackOffTimer() {
        if (mResetThread != null) {
            mResetThread.interrupt();
            mResetThread = null;
        }

        mResetThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(mCurrentBackOffTimeMs + mResetIntervalMs);
                } catch (InterruptedException e) {
                    return;
                }
                mCurrentBackOffTimeMs = mInitialIntervalTimeMs;
            }
        });
        mResetThread.start();
    }

    private synchronized void post(final Runnable runnable, final int delayMs) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(delayMs, 0);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                runnable.run();
            }
        }).run();
    }

    protected void incrementBackOffTime() {
        mCurrentBackOffTimeMs *= mMultiplier;
        if (mCurrentBackOffTimeMs > mMaxIntervalMs) {
            mCurrentBackOffTimeMs = mMaxIntervalMs;
        }
    }

    public int getCurrentBackOffTimeMs() {
        return mCurrentBackOffTimeMs;
    }

}
