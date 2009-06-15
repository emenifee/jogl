/*
 * Copyright (c) 2008 Sun Microsystems, Inc. All Rights Reserved.
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
 */

#ifdef _WIN32
  #include <windows.h>
#endif

#include <stdlib.h>
#include <stdio.h>
#include <string.h>

/* This typedef is apparently needed for Microsoft compilers before VC8,
   and on Windows CE */
#if (_MSC_VER < 1400) || defined(UNDER_CE)
    #ifdef _WIN64
        typedef long long intptr_t;
    #else
        typedef int intptr_t;
    #endif
#elif _MSC_VER <= 1500
    #ifdef _WIN64 // [
        typedef __int64           intptr_t;
    #else // _WIN64 ][
        typedef int               intptr_t;
    #endif // _WIN64 ]
#else
    #include <inttypes.h>
#endif

#include <KD/kd.h>
#include <KD/NV_extwindowprops.h>

#include "com_sun_javafx_newt_opengl_kd_KDWindow.h"

#include "EventListener.h"
#include "MouseEvent.h"
#include "KeyEvent.h"

// #define VERBOSE_ON 1

#ifdef VERBOSE_ON
    #define DBG_PRINT(...) fprintf(stdout, __VA_ARGS__)
#else
    #define DBG_PRINT(...)
#endif

#ifdef VERBOSE_ON
    #ifdef _WIN32_WCE
        #define STDOUT_FILE "\\Storage Card\\stdout.txt"
        #define STDERR_FILE "\\Storage Card\\stderr.txt"
    #endif
#endif

/**
 * Window
 */

static jmethodID sizeChangedID = NULL;
static jmethodID windowDestroyNotifyID = NULL;
static jmethodID windowDestroyedID = NULL;
static jmethodID sendMouseEventID = NULL;
static jmethodID sendKeyEventID = NULL;

JNIEXPORT jboolean JNICALL Java_com_sun_javafx_newt_kd_KDWindow_initIDs
  (JNIEnv *env, jclass clazz)
{
#ifdef VERBOSE_ON
    #ifdef _WIN32_WCE
        _wfreopen(TEXT(STDOUT_FILE),L"w",stdout);
        _wfreopen(TEXT(STDERR_FILE),L"w",stderr);
    #endif
#endif
    sizeChangedID = (*env)->GetMethodID(env, clazz, "sizeChanged", "(II)V");
    windowDestroyNotifyID = (*env)->GetMethodID(env, clazz, "windowDestroyNotify",    "()V");
    windowDestroyedID = (*env)->GetMethodID(env, clazz, "windowDestroyed", "()V");
    sendMouseEventID = (*env)->GetMethodID(env, clazz, "sendMouseEvent", "(IIIIII)V");
    sendKeyEventID = (*env)->GetMethodID(env, clazz, "sendKeyEvent", "(IIIC)V");
    if (sizeChangedID == NULL ||
        windowDestroyNotifyID == NULL ||
        windowDestroyedID == NULL ||
        sendMouseEventID == NULL ||
        sendKeyEventID == NULL) {
        DBG_PRINT( "initIDs failed\n" );
        return JNI_FALSE;
    }
    DBG_PRINT( "initIDs ok\n" );
    return JNI_TRUE;
}

JNIEXPORT jlong JNICALL Java_com_sun_javafx_newt_kd_KDWindow_CreateWindow
  (JNIEnv *env, jobject obj, jint owner, jlong display, jintArray jAttrs)
{
    jint * attrs = NULL;
    jsize attrsLen;
    EGLDisplay dpy  = (EGLDisplay)(intptr_t)display;
    KDWindow *window = 0;

    DBG_PRINT( "[CreateWindow]: owner %d\n", owner);

    if(dpy==NULL) {
        fprintf(stderr, "[CreateWindow] invalid display connection..\n");
        return 0;
    }

    attrsLen = (*env)->GetArrayLength(env, jAttrs);
    if(0==attrsLen) {
        fprintf(stderr, "[CreateWindow] attribute array size 0..\n");
        return 0;
    }
    attrs = (*env)->GetIntArrayElements(env, jAttrs, 0);
    if(NULL==attrs) {
        fprintf(stderr, "[CreateWindow] attribute array NULL..\n");
        return 0;
    }

    /* passing the KDWindow instance for the eventuserptr */
    window = kdCreateWindow(dpy, attrs, (void *)(intptr_t)owner);

    (*env)->ReleaseIntArrayElements(env, jAttrs, attrs, 0);

    if(NULL==window) {
        fprintf(stderr, "[CreateWindow] failed: 0x%X\n", kdGetError());
    }
    DBG_PRINT( "[CreateWindow] ok: %p, owner %d\n", window, (void *)(intptr_t)owner);
    return (jlong) (intptr_t) window;
}

JNIEXPORT jlong JNICALL Java_com_sun_javafx_newt_kd_KDWindow_RealizeWindow
  (JNIEnv *env, jobject obj, jlong window)
{
    KDWindow *w = (KDWindow*) (intptr_t) window;
    EGLNativeWindowType nativeWindow=0;

    jint res = kdRealizeWindow(w, &nativeWindow);
    if(res) {
        fprintf(stderr, "[RealizeWindow] failed: 0x%X, 0x%X\n", res, kdGetError());
        nativeWindow = NULL;
    }
    DBG_PRINT( "[RealizeWindow] ok: %p\n", nativeWindow);
    return (jlong) (intptr_t) nativeWindow;
}

JNIEXPORT jint JNICALL Java_com_sun_javafx_newt_kd_KDWindow_CloseWindow
  (JNIEnv *env, jobject obj, jlong window)
{
    KDWindow *w = (KDWindow*) (intptr_t) window;
    int res = kdDestroyWindow(w);

    DBG_PRINT( "[CloseWindow] res: %d\n", res);
    return res;
}

/*
 * Class:     com_sun_javafx_newt_kd_KDWindow
 * Method:    setVisible0
 * Signature: (JJZ)V
 */
JNIEXPORT void JNICALL Java_com_sun_javafx_newt_kd_KDWindow_setVisible0
  (JNIEnv *env, jobject obj, jlong window, jboolean visible)
{
    KDWindow *w = (KDWindow*) (intptr_t) window;
    KDboolean v = (visible==JNI_TRUE)?KD_TRUE:KD_FALSE;
    kdSetWindowPropertybv(w, KD_WINDOWPROPERTY_VISIBILITY, &v);
    DBG_PRINT( "[setVisible] v=%d\n", visible);
}

JNIEXPORT void JNICALL Java_com_sun_javafx_newt_kd_KDWindow_DispatchMessages
  (JNIEnv *env, jobject obj, jint owner, jlong window, jint eventMask)
{
    KDWindow *w = (KDWindow*) (intptr_t) window;
    const KDEvent * evt;

    // Periodically take a break
    while( NULL!=(evt=kdWaitEvent(0)) ) {
        jint src_owner = (jint)(intptr_t)evt->userptr;
        if(src_owner != owner) {
            DBG_PRINT( "event unrelated: src: %d, caller: %d, evt type: 0x%X\n", src_owner, owner, evt->type);
            continue;
        }
        DBG_PRINT( "[DispatchMessages]: caller %d, evt type: 0x%X\n", owner, evt->type);

        switch(evt->type) {
            case KD_EVENT_INPUT_POINTER:
                if( ! ( eventMask & EVENT_MOUSE ) ) {
                    DBG_PRINT( "event mouse ignored: src: %d\n", owner);
                    continue;
                }
                break;
            /*
            case KeyPress:
            case KeyRelease:
                if( ! ( eventMask & EVENT_KEY ) ) {
                    DBG_PRINT( "event key ignored: src: %d\n", owner);
                    continue;
                }
                break;
                */
            case KD_EVENT_WINDOW_FOCUS:
            case KD_EVENT_WINDOW_CLOSE:
            case KD_EVENT_WINDOWPROPERTY_CHANGE:
            case KD_EVENT_WINDOW_REDRAW:
                if( ! ( eventMask & EVENT_WINDOW ) ) {
                    DBG_PRINT( "event window ignored: src: %d\n", owner);
                    continue;
                }
                break;
        }
        
        // FIXME: support resize and window re-positioning events

        switch(evt->type) {
            case KD_EVENT_WINDOW_FOCUS:
                {
                    KDboolean hasFocus;
                    kdGetWindowPropertybv(w, KD_WINDOWPROPERTY_FOCUS, &hasFocus);
                    DBG_PRINT( "event window focus : src: %d\n", owner);
                }
                break;
            case KD_EVENT_WINDOW_CLOSE:
                {
                    DBG_PRINT( "event window close : src: %d\n", owner);
                    (*env)->CallVoidMethod(env, obj, windowDestroyNotifyID);
                    // Called by Window.java: DestroyWindow(wnd);
                    //  (*env)->CallVoidMethod(env, obj, windowDestroyedID);
                }
                break;
            case KD_EVENT_WINDOWPROPERTY_CHANGE:
                {
                    const KDEventWindowProperty* prop = &evt->data.windowproperty;
                    switch (prop->pname) {
                        case KD_WINDOWPROPERTY_SIZE:
                            {
                                KDint32 v[2];
                                if(!kdGetWindowPropertyiv(w, KD_WINDOWPROPERTY_SIZE, v)) {
                                    DBG_PRINT( "event window size change : src: %d %dx%d\n", owner, v[0], v[1]);
                                    (*env)->CallVoidMethod(env, obj, sizeChangedID, (jint) v[0], (jint) v[1]);
                                } else {
                                    DBG_PRINT( "event window size change error: src: %d %dx%d\n", owner, v[0], v[1]);
                                }
                            }
                            break;
                        case KD_WINDOWPROPERTY_FOCUS:
                            DBG_PRINT( "event window focus: src: %d\n", owner);
                            break;
                        case KD_WINDOWPROPERTY_VISIBILITY:
                            {
                                KDboolean visible;
                                kdGetWindowPropertybv(w, KD_WINDOWPROPERTY_VISIBILITY, &visible);
                                DBG_PRINT( "event window visibility: src: %d, v:%d\n", owner, visible);
                            }
                            break;
                        default:
                            break;
                    }
                }
                break;
            case KD_EVENT_INPUT_POINTER:
                {
                    const KDEventInputPointer* ptr = &(evt->data.inputpointer);
                    // button idx: evt->data.input.index
                    // pressed = ev->data.input.value.i
                    // time = ev->timestamp
                    if(KD_INPUT_POINTER_SELECT==ptr->index) {
                        DBG_PRINT( "event mouse click: src: %d, s:%d, (%d,%d)\n", owner, ptr->select, ptr->x, ptr->y);
                        (*env)->CallVoidMethod(env, obj, sendMouseEventID, 
                                              (ptr->select==0) ? (jint) EVENT_MOUSE_RELEASED : (jint) EVENT_MOUSE_PRESSED, 
                                              (jint) 0,
                                              (jint) ptr->x, (jint) ptr->y, 1, 0);
                    } else {
                        DBG_PRINT( "event mouse: src: %d, s:%d, i:0x%X (%d,%d)\n", owner, ptr->select, ptr->index, ptr->x, ptr->y);
                        (*env)->CallVoidMethod(env, obj, sendMouseEventID, (jint) EVENT_MOUSE_MOVED, 
                                              0,
                                              (jint) ptr->x, (jint) ptr->y, 0, 0);
                    }
                }
                break;
        }
    } 
}

JNIEXPORT void JNICALL Java_com_sun_javafx_newt_kd_KDWindow_setFullScreen0
  (JNIEnv *env, jobject obj, jlong window, jboolean fullscreen)
{
    KDWindow *w = (KDWindow*) (intptr_t) window;
    KDboolean v = fullscreen;

    int res = kdSetWindowPropertybv(w, KD_WINDOWPROPERTY_FULLSCREEN_NV, &v);
    DBG_PRINT( "[setFullScreen] v=%d, res=%d\n", fullscreen, res);
    (void)res;
}

JNIEXPORT void JNICALL Java_com_sun_javafx_newt_kd_KDWindow_setSize0
  (JNIEnv *env, jobject obj, jlong window, jint width, jint height)
{
    KDWindow *w = (KDWindow*) (intptr_t) window;
    KDint32 v[] = { width, height };

    int res = kdSetWindowPropertyiv(w, KD_WINDOWPROPERTY_SIZE, v);
    DBG_PRINT( "[setSize] v=%dx%d, res=%d\n", width, height, res);
    (void)res;

    (*env)->CallVoidMethod(env, obj, sizeChangedID, (jint) width, (jint) height);
}

