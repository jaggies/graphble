/**
 * Created by jmiller on 7/29/16.
 */
package net.hackdog.minimalble;

import android.content.Context;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.widget.LinearLayout;
import android.widget.TextView;

import javax.microedition.khronos.opengles.GL10;

public class GraphView extends LinearLayout {
    private GLSurfaceView mGraphView;
    private TextView mChannelLabel;
    private TextView mChannelValue;

    public GraphView(Context context) {
        super(context);
    }

    public GraphView(Context context,
                     AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mGraphView = (GLSurfaceView) findViewById(R.id.gl_surface_view);
        mChannelLabel = (TextView) findViewById(R.id.label);
        mChannelValue = (TextView) findViewById(R.id.value);
        mGraphView.setEGLContextClientVersion(3);
        mGraphView.setRenderer(new GraphRenderer());
    }

    public void setLabel(CharSequence label) {
        mChannelLabel.setText(label);
    }

    public void setValue(CharSequence value) {
        mChannelValue.setText(value);
    }

    static class GraphRenderer implements GLSurfaceView.Renderer {
        private int mWidth;
        private int mHeight;

        @Override
        public void onSurfaceCreated(GL10 gl10, javax.microedition.khronos.egl.EGLConfig eglConfig) {

        }

        @Override
        public void onSurfaceChanged(GL10 unused, int w, int h) {
            GLES30.glViewport(0, 0, w, h);
            mWidth = w;
            mHeight = h;
        }

        @Override
        public void onDrawFrame(GL10 unused) {
            GLES30.glClearColor(0.5f, 0.5f, 0.5f, 0.0f);
            GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT | GLES30.GL_DEPTH_BUFFER_BIT);
        }

        public void drawGrid(GL10 unused, float minimumY, float maximumY)
        {

        }
    }
}