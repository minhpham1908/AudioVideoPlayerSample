package com.serenegiant.media;

import androidx.annotation.NonNull;


/**
 * An interface to redefine the time between video or audio frames.
 */
public interface TimeInterpolator {


    long interpolate(long time);
}
