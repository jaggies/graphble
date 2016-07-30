/**
 * Created by jmiller on 7/29/16.
 */
package net.hackdog.minimalble;

import android.content.Context;
import android.graphics.Canvas;
import android.opengl.EGLConfig;
import android.opengl.GLES10;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.SurfaceHolder;

import javax.microedition.khronos.opengles.GL10;

public class GraphView extends GLSurfaceView {
    public GraphView(Context context) {
        super(context);
    }

    public GraphView(Context context,
                     AttributeSet attrs) {
        super(context, attrs);
        setEGLContextClientVersion(2);
        setRenderer(new GraphRenderer());
    }

    static class GraphRenderer implements GLSurfaceView.Renderer {
        @Override
        public void onSurfaceCreated(GL10 gl10, javax.microedition.khronos.egl.EGLConfig eglConfig) {

        }

        @Override
        public void onSurfaceChanged(GL10 gl10, int w, int h) {
            gl10.glViewport(0, 0, w, h);
        }

        @Override
        public void onDrawFrame(GL10 gl10) {
            gl10.glClearColor(1.0f, 0.0f, 0.0f, 0.0f);
            gl10.glClear(GLES10.GL_COLOR_BUFFER_BIT | GLES10.GL_DEPTH_BUFFER_BIT);
        }
    }
}