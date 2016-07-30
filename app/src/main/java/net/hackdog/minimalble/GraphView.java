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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;

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
        Grid mGrid;

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
            GLES30.glClearColor(0.75f, 0.75f, 0.75f, 0.0f);
            GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT | GLES30.GL_DEPTH_BUFFER_BIT);
            if (mGrid == null) {
                mGrid = new Grid(10, 10);
            }
            mGrid.draw();
        }
    }

    public static int loadShader(int type, String shaderCode){

        // create a vertex shader type (GLES20.GL_VERTEX_SHADER)
        // or a fragment shader type (GLES20.GL_FRAGMENT_SHADER)
        int shader = GLES30.glCreateShader(type);

        // add the source code to the shader and compile it
        GLES30.glShaderSource(shader, shaderCode);
        GLES30.glCompileShader(shader);

        return shader;
    }

    public static class Grid {
        private static final int COORDS_PER_VERTEX = 3;
        private final String vertexShaderCode =
            "attribute vec4 vPosition;" +
                "void main() {" +
                "  gl_Position = vec4(2.0f,2.0f,2.0f,1.0f) " +
                    "   * (vPosition - vec4(0.5,0.5,0.0,0.0));" +
                "}";

        private final String fragmentShaderCode =
            "precision mediump float;" +
                "uniform vec4 vColor;" +
                "void main() {" +
                "  gl_FragColor = vColor;" +
                "}";
        private FloatBuffer vertexBuffer;
        private float coords[];
        private int shader;
        private int mProgram;
        private int mPositionHandle;
        private int mColorHandle;

        // Set color with red, green, blue and alpha (opacity) values
        float color[] = { 0.0f, 0.0f, 0.0f, 1.0f };

        public Grid(int rows, int cols) {
            int vertexShader = GraphView.loadShader(GLES30.GL_VERTEX_SHADER, vertexShaderCode);
            int fragmentShader = GraphView.loadShader(GLES30.GL_FRAGMENT_SHADER, fragmentShaderCode);

            mProgram = GLES30.glCreateProgram(); // create empty OpenGL ES Program
            GLES30.glAttachShader(mProgram, vertexShader); // add the vertex shader to program
            GLES30.glAttachShader(mProgram, fragmentShader); // add the fragment shader to program
            GLES30.glLinkProgram(mProgram); // creates OpenGL ES program executables

            // initialize vertex byte buffer for shape coordinates
            coords = createGrid(rows, cols);
            ByteBuffer bb = ByteBuffer.allocateDirect(coords.length * 4 /* sizeof(float) */);
            bb.order(ByteOrder.nativeOrder()); // use native hardware byte order
            vertexBuffer = bb.asFloatBuffer(); // floating point buffer from the ByteBuffer
            vertexBuffer.put(coords); // add the coordinates to the FloatBuffer
            vertexBuffer.position(0); // set the buffer to read the first coordinate
        }

        public void draw() {
            // Add program to OpenGL ES environment
            GLES30.glUseProgram(mProgram);

            // get handle to vertex shader's vPosition member
            mPositionHandle = GLES30.glGetAttribLocation(mProgram, "vPosition");

            // Enable a handle to the triangle vertices
            GLES30.glEnableVertexAttribArray(mPositionHandle);

            // Prepare the triangle coordinate data
            final int vertexStride = COORDS_PER_VERTEX * 4 /* sizeof(float) */;

            GLES30.glVertexAttribPointer(mPositionHandle, COORDS_PER_VERTEX,
                    GLES30.GL_FLOAT, false,
                    vertexStride, vertexBuffer);

            // get handle to fragment shader's vColor member
            mColorHandle = GLES30.glGetUniformLocation(mProgram, "vColor");
            GLES30.glUniform4fv(mColorHandle, 1, color, 0); // Set color for drawing the triangle
            GLES30.glDrawArrays(GLES30.GL_LINES, 0, coords.length/COORDS_PER_VERTEX);
            GLES30.glDisableVertexAttribArray(mPositionHandle); // Disable vertex array
        }

        float[] createGrid(int rows, int cols) {
            final float dx = 1.0f / (cols-1);
            final float dy = 1.0f / (rows-1);
            float result[] = new float[2 * COORDS_PER_VERTEX * (rows + cols)];
            int idx = 0;
            // horizontal lines
            for (int row = 0; row < rows; row++) {
                float y = row * dy;
                result[idx++] = 0.0f;
                result[idx++] = y;
                result[idx++] = 0.0f;

                result[idx++] = 1.0f;
                result[idx++] = y;
                result[idx++] = 0.0f;
            }
            // vertical lines
            for (int col = 0; col < cols; col++) {
                float x = col * dx;
                result[idx++] = x;
                result[idx++] = 0.0f;
                result[idx++] = 0.0f;

                result[idx++] = x;
                result[idx++] = 1.0f;
                result[idx++] = 0.0f;
            }
            return result;
        }
    }
}