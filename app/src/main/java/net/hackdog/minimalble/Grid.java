package net.hackdog.minimalble;

import android.opengl.GLES30;
import android.renderscript.Matrix4f;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * Created by jmiller on 7/30/16.
 */
public class Grid extends Primitive {
    private static final int COORDS_PER_VERTEX = 3;
    private FloatBuffer vertexBuffer;
    private float coords[];
    private int shader;
    private int mProgram;
    private int mPositionHandle;
    private int mColorHandle;

    // Set color with red, green, blue and alpha (opacity) values
    final float color[] = { 0.0f, 0.0f, 0.0f, 1.0f };
    Matrix4f matrix4f = new Matrix4f();

    @Override
    public float[] getColor() {
        return color;
    }

    public Grid(int rows, int cols) {
        int vertexShader = loadVertexShader();
        int fragmentShader = loadFragmentShader();

        mProgram = GLES30.glCreateProgram(); // create empty OpenGL ES Program
        GLES30.glAttachShader(mProgram, vertexShader); // add the vertex shader to program
        GLES30.glAttachShader(mProgram, fragmentShader); // add the fragment shader to program
        GLES30.glLinkProgram(mProgram); // creates OpenGL ES program executables

        // initialize vertex byte buffer for shape coordinates
        coords = createGrid(rows+1, cols+1);
        ByteBuffer bb = ByteBuffer.allocateDirect(coords.length * 4 /* sizeof(float) */);
        bb.order(ByteOrder.nativeOrder()); // use native hardware byte order
        vertexBuffer = bb.asFloatBuffer(); // floating point buffer from the ByteBuffer
        vertexBuffer.put(coords); // add the coordinates to the FloatBuffer
        vertexBuffer.position(0); // set the buffer to read the first coordinate

        // Set Viewport from 0 to 1 in x & y directions
        matrix4f.loadOrtho(0.0f, 1.0f, 0.0f, 1.0f, 0.0f, 1.0f);
    }

    @Override
    public void draw() {
        // Add program to OpenGL ES environment
        GLES30.glUseProgram(mProgram);

        // get handle to vertex shader's vPosition member
        mPositionHandle = GLES30.glGetAttribLocation(mProgram, "vPosition");

        // Enable a handle to the vertices
        GLES30.glEnableVertexAttribArray(mPositionHandle);

        // Prepare the triangle coordinate data
        final int vertexStride = COORDS_PER_VERTEX * 4 /* sizeof(float) */;

        GLES30.glVertexAttribPointer(mPositionHandle, COORDS_PER_VERTEX,
                GLES30.GL_FLOAT, false, vertexStride, vertexBuffer);

        int mtrxhandle = GLES30.glGetUniformLocation(mProgram, "matrix");

        // Apply the projection and view transformation
        // get handle to fragment shader's vColor member
        mColorHandle = GLES30.glGetUniformLocation(mProgram, "vColor");
        GLES30.glUniform4fv(mColorHandle, 1, color, 0); // Set color
        GLES30.glUniformMatrix4fv(mtrxhandle, 1, false, matrix4f.getArray(), 0);
        GLES30.glDrawArrays(GLES30.GL_LINES, 0, coords.length/COORDS_PER_VERTEX);
        GLES30.glDisableVertexAttribArray(mPositionHandle); // Disable vertex array
    }

    private static final float[] createGrid(int rows, int cols) {
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