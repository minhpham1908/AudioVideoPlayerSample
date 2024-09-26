package com.serenegiant.media;

import static java.lang.Math.abs;

import android.util.Log;

public class SpeedController {
    private static final String TAG = "SpeedController";
    Long mPrevMonoUsec = 0L;
    Long mPrevPresentUsec = 0L;
    private long ONE_MILLION = 1000000L;
    private boolean CHECK_SLEEP_TIME = false;

    public void preRender(Long presentationTimeUsec) {
        // For the first frame, we grab the presentation time from the video
        // and the current monotonic clock time.  For subsequent frames, we
        // sleep for a bit to try to ensure that we're rendering frames at the
        // pace dictated by the video stream.
        //
        // If the frame rate is faster than vsync we should be dropping frames.  On
        // Android 4.4 this may not be happening.

        if (mPrevMonoUsec == 0L) {
            // Latch current values, then return immediately.
            mPrevMonoUsec = System.nanoTime() / 1000;
            mPrevPresentUsec = presentationTimeUsec;
        } else {
            // Compute the desired time delta between the previous frame and this frame.
            Long frameDelta;
//            if (mLoopReset) {
//                // We don't get an indication of how long the last frame should appear
//                // on-screen, so we just throw a reasonable value in.  We could probably
//                // do better by using a previous frame duration or some sort of average;
//                // for now we just use 30fps.
//                mPrevPresentUsec = presentationTimeUsec - com.android.grafika.SpeedControlCallback.ONE_MILLION / 30
//                mLoopReset = false
//            }
//            if (mFixedFrameDurationUsec != 0L) {
//                // Caller requested a fixed frame rate.  Ignore PTS.
//                frameDelta = mFixedFrameDurationUsec
//            } else {
//            frameDelta = presentationTimeUsec - mPrevPresentUsec;
            frameDelta = ONE_MILLION / 30;
//            }
            if (frameDelta < 0L) {
                Log.w(TAG, "Weird, video times went backward");
                frameDelta = 0L;
            } else if (frameDelta == 0L) {
                // This suggests a possible bug in movie generation.
                Log.i(TAG, "Warning: current frame and previous frame had same timestamp");
            } else if (frameDelta > 10 * ONE_MILLION) {
                // Inter-frame times could be arbitrarily long.  For this player, we want
                // to alert the developer that their movie might have issues (maybe they
                // accidentally output timestamps in nsec rather than usec).
                Log.i(TAG, "Inter-frame pause was " + (frameDelta / ONE_MILLION) + "sec, capping at 5 sec");
                frameDelta = 5 * ONE_MILLION;
            }

            Long desiredUsec = mPrevMonoUsec + frameDelta; // when we want to wake up
            long nowUsec = System.nanoTime() / 1000;
            Log.d(TAG, "preRender: "+frameDelta);
            while (nowUsec < (desiredUsec - 100) /*&& mState == RUNNING*/) {
                // Sleep until it's time to wake up.  To be responsive to "stop" commands
                // we're going to wake up every half a second even if the sleep is supposed
                // to be longer (which should be rare).  The alternative would be
                // to interrupt the thread, but that requires more work.
                //
                // The precision of the sleep call varies widely from one device to another;
                // we may wake early or late.  Different devices will have a minimum possible
                // sleep time. If we're within 100us of the target time, we'll probably
                // overshoot if we try to sleep, so just go ahead and continue on.
                long sleepTimeUsec = desiredUsec - nowUsec;
                if (sleepTimeUsec > 500000) {
                    sleepTimeUsec = 500000;
                }
                try {
                    if (CHECK_SLEEP_TIME) {
                        long startNsec = System.nanoTime();
                        Thread.sleep(sleepTimeUsec / 1000, (int) (sleepTimeUsec % 1000) * 1000);
                        long actualSleepNsec = System.nanoTime() - startNsec;
                        Log.d(TAG, "sleep=" + sleepTimeUsec + " actual=" + (actualSleepNsec / 1000) + " diff=" + (abs((actualSleepNsec / 1000 - sleepTimeUsec))) + " (usec)");
                    } else {
                        Thread.sleep(sleepTimeUsec / 1000, (int) (sleepTimeUsec % 1000) * 1000);
                    }
                } catch (InterruptedException ie) {
                }
                nowUsec = System.nanoTime() / 1000;
            }

            // Advance times using calculated time values, not the post-sleep monotonic
            // clock time, to avoid drifting.
            mPrevMonoUsec += frameDelta;
            mPrevPresentUsec += frameDelta;
        }
    }
}
