/*
 * Copyright (c) 2003 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 * 
 * - Redistribution of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 * 
 * - Redistribution in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 * 
 * Neither the name of Sun Microsystems, Inc. or the names of
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 * 
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES,
 * INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN
 * MICROSYSTEMS, INC. ("SUN") AND ITS LICENSORS SHALL NOT BE LIABLE FOR
 * ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR
 * DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL SUN OR
 * ITS LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR
 * DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE
 * DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY,
 * ARISING OUT OF THE USE OF OR INABILITY TO USE THIS SOFTWARE, EVEN IF
 * SUN HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 * 
 * You acknowledge that this software is not designed or intended for use
 * in the design, construction, operation or maintenance of any nuclear
 * facility.
 * 
 * Sun gratefully acknowledges that this software was originally authored
 * and developed by Kenneth Bradley Russell and Christopher John Kline.
 */

package com.jogamp.nativewindow.impl.jawt.windows;

import com.jogamp.nativewindow.impl.*;
import com.jogamp.nativewindow.impl.jawt.*;

import javax.media.nativewindow.*;

public class WindowsJAWTWindow extends JAWTWindow {

  public static final boolean PROFILING = false; // FIXME
  public static final int PROFILING_TICKS = 600; // FIXME

  public WindowsJAWTWindow(Object comp, AbstractGraphicsConfiguration config) {
    super(comp, config);
  }

  protected void initNative() throws NativeWindowException {
  }

  public synchronized void invalidate() {
    super.invalidate();
    windowHandle = 0;
  }

  protected int lockSurfaceImpl() throws NativeWindowException {
    int ret = NativeWindow.LOCK_SUCCESS;
    long startTime;
    if (PROFILING) {
      startTime = System.currentTimeMillis();
    }
    ds = JAWT.getJAWT().GetDrawingSurface(component);
    if (ds == null) {
      // Widget not yet realized
      unlockSurface();
      return LOCK_SURFACE_NOT_READY;
    }
    int res = ds.Lock();
    dsLocked = ( 0 == ( res & JAWTFactory.JAWT_LOCK_ERROR ) ) ;
    if (!dsLocked) {
      unlockSurface();
      throw new NativeWindowException("Unable to lock surface");
    }
    // See whether the surface changed and if so destroy the old
    // OpenGL context so it will be recreated (NOTE: removeNotify
    // should handle this case, but it may be possible that race
    // conditions can cause this code to be triggered -- should test
    // more)
    if ((res & JAWTFactory.JAWT_LOCK_SURFACE_CHANGED) != 0) {
      ret = LOCK_SURFACE_CHANGED;
    }
    dsi = ds.GetDrawingSurfaceInfo();
    if (dsi == null) {
      unlockSurface();
      return LOCK_SURFACE_NOT_READY;
    }
    win32dsi = (JAWT_Win32DrawingSurfaceInfo) dsi.platformInfo();
    if (win32dsi == null) {
      unlockSurface();
      return LOCK_SURFACE_NOT_READY;
    }
    windowHandle = win32dsi.getHandle();
    drawable = win32dsi.getHdc();
    if (windowHandle == 0 || drawable == 0) {
      unlockSurface();
      return LOCK_SURFACE_NOT_READY;
    } else {
      updateBounds(dsi.getBounds());
    }
    if (PROFILING) {
      long endTime = System.currentTimeMillis();
      profilingLockSurfaceTime += (endTime - startTime);
      if (++profilingLockSurfaceTicks == PROFILING_TICKS) {
        System.err.println("LockSurface calls: " + profilingLockSurfaceTime + " ms / " + PROFILING_TICKS + " calls (" +
                           ((float) profilingLockSurfaceTime / (float) PROFILING_TICKS) + " ms/call)");
        profilingLockSurfaceTime = 0;
        profilingLockSurfaceTicks = 0;
      }
    }
    return ret;
  }

  protected void unlockSurfaceImpl() throws NativeWindowException {
    long startTime = 0;
    if (PROFILING) {
      startTime = System.currentTimeMillis();
    }
    if(null!=ds) {
        if (null!=dsi) {
            ds.FreeDrawingSurfaceInfo(dsi);
        }
        if (dsLocked) {
            ds.Unlock();
        }
        JAWT.getJAWT().FreeDrawingSurface(ds);
    }
    ds = null;
    dsi = null;
    win32dsi = null;
    if (PROFILING) {
      long endTime = System.currentTimeMillis();
      profilingUnlockSurfaceTime += (endTime - startTime);
      if (++profilingUnlockSurfaceTicks == PROFILING_TICKS) {
        System.err.println("UnlockSurface calls: " + profilingUnlockSurfaceTime + " ms / " + PROFILING_TICKS + " calls (" +
                           ((float) profilingUnlockSurfaceTime / (float) PROFILING_TICKS) + " ms/call)");
        profilingUnlockSurfaceTime = 0;
        profilingUnlockSurfaceTicks = 0;
      }
    }
  }

  public long getWindowHandle() {
    return windowHandle;
  }

  // Variables for lockSurface/unlockSurface
  private JAWT_DrawingSurface ds;
  private boolean dsLocked;
  private JAWT_DrawingSurfaceInfo dsi;
  private JAWT_Win32DrawingSurfaceInfo win32dsi;
  private long profilingLockSurfaceTime = 0;
  private int  profilingLockSurfaceTicks = 0;
  private long profilingUnlockSurfaceTime = 0;
  private int  profilingUnlockSurfaceTicks = 0;

  // lifetime: valid after lock, forever until invalidate
  protected long windowHandle;
}

