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

package com.jogamp.opengl.impl.macosx.cgl;

import java.nio.*;
import javax.media.nativewindow.*;
import javax.media.opengl.*;
import com.jogamp.common.JogampRuntimeException;
import com.jogamp.common.util.*;
import com.jogamp.opengl.impl.*;
import com.jogamp.nativewindow.impl.NullWindow;

public class MacOSXCGLDrawableFactory extends GLDrawableFactoryImpl {
  private static final DesktopGLDynamicLookupHelper macOSXCGLDynamicLookupHelper;

  static {
        DesktopGLDynamicLookupHelper tmp = null;
        try {
            tmp = new DesktopGLDynamicLookupHelper(new MacOSXCGLDynamicLibraryBundleInfo());
        } catch (GLException gle) {
            if(DEBUG) {
                gle.printStackTrace();
            }
        }
        macOSXCGLDynamicLookupHelper = tmp;
        /** FIXME ?? 
        if(null!=macOSXCGLDynamicLookupHelper) {
            CGL.getCGLProcAddressTable().reset(macOSXCGLDynamicLookupHelper);
        } */
  }

  public GLDynamicLookupHelper getGLDynamicLookupHelper(int profile) {
      return macOSXCGLDynamicLookupHelper;
  }

  public MacOSXCGLDrawableFactory() {
    super();

    // Register our GraphicsConfigurationFactory implementations
    // The act of constructing them causes them to be registered
    new MacOSXCGLGraphicsConfigurationFactory();

    try {
      ReflectionUtil.createInstance("com.jogamp.opengl.impl.macosx.cgl.awt.MacOSXAWTCGLGraphicsConfigurationFactory", 
                                    new Object[] {}, getClass().getClassLoader());
    } catch (JogampRuntimeException jre) { /* n/a .. */ }
  }

  protected void shutdown() {}
  protected final GLDrawableImpl getSharedDrawable() { return null; }
  protected final GLContextImpl getSharedContext() { return null; }

  public GLDrawableImpl createOnscreenDrawable(NativeWindow target) {
    if (target == null) {
      throw new IllegalArgumentException("Null target");
    }
    return new MacOSXOnscreenCGLDrawable(this, target);
  }

  protected GLDrawableImpl createOffscreenDrawable(NativeWindow target) {
    return new MacOSXOffscreenCGLDrawable(this, target);
  }

  public boolean canCreateGLPbuffer(AbstractGraphicsDevice device) {
    return true;
  }

  protected GLDrawableImpl createGLPbufferDrawableImpl(final NativeWindow target) {
    /** 
     * FIXME: Think about this ..
     * should not be necessary ? ..
    final List returnList = new ArrayList();
    final GLDrawableFactory factory = this;
    Runnable r = new Runnable() {
        public void run() {
          returnList.add(new MacOSXPbufferCGLDrawable(factory, target));
        }
      };
    maybeDoSingleThreadedWorkaround(r);
    return (GLDrawableImpl) returnList.get(0);
    */
    return new MacOSXPbufferCGLDrawable(this, target);
  }

  protected NativeWindow createOffscreenWindow(GLCapabilities capabilities, GLCapabilitiesChooser chooser, int width, int height) {
    AbstractGraphicsScreen screen = DefaultGraphicsScreen.createDefault();
    NullWindow nw = new NullWindow(MacOSXCGLGraphicsConfigurationFactory.chooseGraphicsConfigurationStatic(capabilities, chooser, screen, true));
    nw.setSize(width, height);
    return nw;
  }

  public GLContext createExternalGLContext() {
    return MacOSXExternalCGLContext.create(this, null);
  }

  public boolean canCreateExternalGLDrawable(AbstractGraphicsDevice device) {
    return false;
  }

  public GLDrawable createExternalGLDrawable() {
    // FIXME
    throw new GLException("Not yet implemented");
  }

  public boolean canCreateContextOnJava2DSurface(AbstractGraphicsDevice device) {
    return false;
  }

  public GLContext createContextOnJava2DSurface(Object graphics, GLContext shareWith)
    throws GLException {
    throw new GLException("not supported in non AWT enviroment");
  }
  
  //------------------------------------------------------
  // Gamma-related functionality
  //

  private static final int GAMMA_RAMP_LENGTH = 256;

  /** Returns the length of the computed gamma ramp for this OS and
      hardware. Returns 0 if gamma changes are not supported. */
  protected int getGammaRampLength() {
    return GAMMA_RAMP_LENGTH;
  }

  protected boolean setGammaRamp(float[] ramp) {
    return CGL.setGammaRamp(ramp.length,
                            ramp, 0,
                            ramp, 0,
                            ramp, 0);
  }

  protected Buffer getGammaRamp() {
    return null;
  }

  protected void resetGammaRamp(Buffer originalGammaRamp) {
    CGL.resetGammaRamp();
  }
}
