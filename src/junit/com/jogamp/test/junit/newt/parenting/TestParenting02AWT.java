/*
 * Copyright (c) 2010 Sven Gothel. All Rights Reserved.
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
 * Neither the name Sven Gothel or the names of
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
 * SVEN GOTHEL HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 */

package com.jogamp.test.junit.newt.parenting;

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Test;

import java.awt.Button;
import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Frame;

import javax.media.opengl.*;
import javax.media.nativewindow.*;

import com.jogamp.opengl.util.Animator;
import com.jogamp.newt.*;
import com.jogamp.newt.event.*;
import com.jogamp.newt.opengl.*;
import com.jogamp.newt.awt.NewtCanvasAWT;

import java.io.IOException;

import com.jogamp.test.junit.util.*;
import com.jogamp.test.junit.jogl.demos.es1.RedSquare;
import com.jogamp.test.junit.jogl.demos.gl2.gears.Gears;

public class TestParenting02AWT {
    static {
        GLProfile.initSingleton();
    }

    static int width, height;
    static long durationPerTest = 500;
    static long waitReparent = 300;
    static boolean verbose = false;

    @BeforeClass
    public static void initClass() {
        width  = 640;
        height = 480;
    }

    @Test
    public void testWindowParenting01NewtChildOnAWTParentLayouted() throws InterruptedException {
        runNewtChildOnAWTParent(true, false);
    }

    @Test
    public void testWindowParenting02NewtChildOnAWTParentLayoutedDef() throws InterruptedException {
        runNewtChildOnAWTParent(true, true);
    }

    @Test
    public void testWindowParenting03NewtChildOnAWTParentDirect() throws InterruptedException {
        runNewtChildOnAWTParent(false, false);
    }

    @Test
    public void testWindowParenting04NewtChildOnAWTParentDirectDef() throws InterruptedException {
        runNewtChildOnAWTParent(false, true);
    }

    public void runNewtChildOnAWTParent(boolean useLayout, boolean deferredPeer) throws InterruptedException {
        NEWTEventFiFo eventFifo = new NEWTEventFiFo();

        // setup NEWT GLWindow ..
        GLWindow glWindow = GLWindow.create(new GLCapabilities(null));
        Assert.assertNotNull(glWindow);
        glWindow.setTitle("NEWT - CHILD");
        glWindow.addKeyListener(new TraceKeyAdapter(new KeyAction(eventFifo)));
        glWindow.addWindowListener(new TraceWindowAdapter(new WindowAction(eventFifo)));
        GLEventListener demo = new Gears();
        setDemoFields(demo, glWindow, false);
        glWindow.addGLEventListener(demo);

        // attach NEWT GLWindow to AWT Canvas
        NewtCanvasAWT newtCanvasAWT = new NewtCanvasAWT(glWindow);
        Assert.assertNotNull(newtCanvasAWT);
        Assert.assertEquals(false, glWindow.isVisible());
        Assert.assertEquals(false, glWindow.isNativeWindowValid());
        Assert.assertNull(glWindow.getParentNativeWindow());

        Frame frame = new Frame("AWT Parent Frame");
        Assert.assertNotNull(frame);
        if(useLayout) {
            frame.setLayout(new BorderLayout());
            frame.add(new Button("North"), BorderLayout.NORTH);
            frame.add(new Button("South"), BorderLayout.SOUTH);
            frame.add(new Button("East"), BorderLayout.EAST);
            frame.add(new Button("West"), BorderLayout.WEST);
            if(!deferredPeer) {
                frame.add(newtCanvasAWT, BorderLayout.CENTER);
            }
        } else {
            if(!deferredPeer) {
                frame.add(newtCanvasAWT);
            }
        }

        frame.setSize(width, height);

        frame.setVisible(true);
        // X11: true, Windows: false - Assert.assertEquals(true, glWindow.isVisible());

        if(deferredPeer) {
            if(useLayout) {
                frame.add(newtCanvasAWT, BorderLayout.CENTER);
            } else {
                frame.add(newtCanvasAWT);
            }
        }

        // Since it is not defined when AWT's addNotify call happen
        // we just have to wait for it in this junit test
        // because we have assertions on the state.
        // Regular application shall not need to do that.
        do {
            Thread.yield();
            // 1st display .. creation
            glWindow.display();
        } while(!glWindow.isNativeWindowValid()) ;

        Assert.assertEquals(true, glWindow.isNativeWindowValid());
        Assert.assertNotNull(glWindow.getParentNativeWindow());
        if(verbose) {
            System.out.println("+++++++++++++++++++ 1st ADDED");
        }
        Thread.sleep(waitReparent);

        if(useLayout) {
            // test some fancy re-layout ..
            frame.remove(newtCanvasAWT);
            Assert.assertEquals(false, glWindow.isVisible());
            Assert.assertEquals(true, glWindow.isNativeWindowValid());
            Assert.assertNull(glWindow.getParentNativeWindow());
            if(verbose) {
                System.out.println("+++++++++++++++++++ REMOVED!");
            }
            Thread.sleep(waitReparent);

            // should recreate properly ..
            frame.add(newtCanvasAWT, BorderLayout.CENTER);
            glWindow.display();
            Assert.assertEquals(true, glWindow.isVisible());
            Assert.assertEquals(true, glWindow.isNativeWindowValid());
            Assert.assertNotNull(glWindow.getParentNativeWindow());
            if(verbose) {
                System.out.println("+++++++++++++++++++ 2nd ADDED");
            }
            Thread.sleep(waitReparent);
        }

        long duration = durationPerTest;
        long step = 20;
        NEWTEvent event;
        boolean shouldQuit = false;

        while (duration>0 && !shouldQuit) {
            glWindow.display();
            Thread.sleep(step);
            duration -= step;

            while( null != ( event = (NEWTEvent) eventFifo.get() ) ) {
                Window source = (Window) event.getSource();
                if(event instanceof KeyEvent) {
                    KeyEvent keyEvent = (KeyEvent) event;
                    switch(keyEvent.getKeyChar()) {
                        case 'q':
                            shouldQuit = true;
                            break;
                        case 'f':
                            source.setFullscreen(!source.isFullscreen());
                            break;
                    }
                } 
            }
        }
        if(verbose) {
            System.out.println("+++++++++++++++++++ END");
        }
        Thread.sleep(waitReparent);

        glWindow.destroy();
        if(useLayout) {
            frame.remove(newtCanvasAWT);
        }
        frame.dispose();
    }

    public static void setDemoFields(GLEventListener demo, GLWindow glWindow, boolean debug) {
        Assert.assertNotNull(demo);
        Assert.assertNotNull(glWindow);
        Window window = glWindow.getInnerWindow();
        if(debug) {
            MiscUtils.setFieldIfExists(demo, "glDebug", true);
            MiscUtils.setFieldIfExists(demo, "glTrace", true);
        }
        if(!MiscUtils.setFieldIfExists(demo, "window", window)) {
            MiscUtils.setFieldIfExists(demo, "glWindow", glWindow);
        }
    }

    static int atoi(String a) {
        int i=0;
        try {
            i = Integer.parseInt(a);
        } catch (Exception ex) { ex.printStackTrace(); }
        return i;
    }

    public static void main(String args[]) throws IOException {
        verbose = true;
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                durationPerTest = atoi(args[++i]);
            } else if(args[i].equals("-wait")) {
                waitReparent = atoi(args[++i]);
            }
        }
        String tstname = TestParenting02AWT.class.getName();
        org.apache.tools.ant.taskdefs.optional.junit.JUnitTestRunner.main(new String[] {
            tstname,
            "filtertrace=true",
            "haltOnError=false",
            "haltOnFailure=false",
            "showoutput=true",
            "outputtoformatters=true",
            "logfailedtests=true",
            "logtestlistenerevents=true",
            "formatter=org.apache.tools.ant.taskdefs.optional.junit.PlainJUnitResultFormatter",
            "formatter=org.apache.tools.ant.taskdefs.optional.junit.XMLJUnitResultFormatter,TEST-"+tstname+".xml" } );
    }

}
