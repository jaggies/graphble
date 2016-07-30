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
    private FloatBuffer vertexBuffer;
    private int shader;
    private int elements;

    // Set color with red, green, blue and alpha (opacity) values
    final float color[] = { 0.0f, 0.0f, 0.0f, 1.0f };
    private Matrix4f matrix4f = new Matrix4f();

    @Override
    public float[] getColor() {
        return color;
    }

    public Grid(int rows, int cols) {
        float[] coords = createGrid(rows+1, cols+1);
        elements = coords.length/COORDS_PER_VERTEX;

        ByteBuffer bb = ByteBuffer.allocateDirect(coords.length * 4 /* sizeof(float) */);
        bb.order(ByteOrder.nativeOrder()); // use native hardware byte order
        vertexBuffer = bb.asFloatBuffer(); // floating point buffer from the ByteBuffer
        vertexBuffer.put(coords); // add the coordinates to the FloatBuffer
        vertexBuffer.position(0); // set the buffer to read the first coordinate

        // Set Viewport from 0 to 1 in x & y directions
        matrix4f.loadOrtho(0.0f, 1.0f, 0.0f, 1.0f, 0.0f, 1.0f);
    }

    @Override
    public void onDraw() {
        final int vertexStride = COORDS_PER_VERTEX * 4 /* sizeof(float) */;
        GLES30.glUniformMatrix4fv(getMatrixHandle(), 1, false, matrix4f.getArray(), 0);
        GLES30.glUniform4fv(getColorHandle(), 1, color, 0); // Set color
        GLES30.glVertexAttribPointer(getPositionHandle(), COORDS_PER_VERTEX,
                GLES30.GL_FLOAT, false, vertexStride, vertexBuffer);
        GLES30.glDrawArrays(GLES30.GL_LINES, 0, elements);
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