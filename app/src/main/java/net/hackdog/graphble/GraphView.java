/**
 * Created by jmiller on 7/29/16.
 */
package net.hackdog.graphble;

import android.content.Context;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;

import javax.microedition.khronos.opengles.GL10;

import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.TextView;

public class GraphView extends LinearLayout {
    private GLSurfaceView mGraphView;
    private TextView mChannelLabel;
    private TextView mChannelValue;
    private GraphRenderer mRenderer;

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
        //mGraphView.setEGLConfigChooser(new MultisampleConfigChooser());
        mRenderer = new GraphRenderer();
        mGraphView.setRenderer(mRenderer);
    }

    public void setLabel(CharSequence label) {
        mChannelLabel.setText(label);
    }

    public void setValue(CharSequence value) {
        mChannelValue.setText(value);
    }

    public void setData(byte[] data) { mRenderer.setData(data); }

    class GraphRenderer implements GLSurfaceView.Renderer {
        private int mWidth;
        private int mHeight;
        Grid mGrid;
        Lines mLines;

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
            GLES30.glClearColor(0.9f, 0.9f, 0.9f, 0.0f);
            GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT | GLES30.GL_DEPTH_BUFFER_BIT);
            if (mGrid == null) {
                mGrid = new Grid();
                mGrid.setViewPort(
                        new Grid.ViewPort(0.0f, 2.0f * mWidth/mHeight, -1.0f, 1.0f, 0.0f, 1.0f));
            }
            if (mLines == null) {
                mLines = new Lines();
                mLines.setViewPort(
                        new Lines.ViewPort(0.0f, 2.0f * mWidth/mHeight, -1.0f, 1.0f, 0.0f, 1.0f));
            }
            mGrid.draw();
            mLines.draw();
        }

        public void setData(byte[] data) {
            if (mLines != null) { // TODO: allow data to be set before first render
                mLines.setData(data);
            }
        }
    }
}