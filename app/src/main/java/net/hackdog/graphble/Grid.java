package net.hackdog.graphble;

import android.opengl.GLES30;
import android.renderscript.Matrix4f;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * Created by jmiller on 7/30/16.
 */
public class Grid extends Primitive {
    private static final int MAX_GRIDLINES = 100;
    private FloatBuffer mVertexBuffer;
    private int mArrayCount;
    private GridSpacing mGridSpacing = new GridSpacing(0.1f, 0.1f);
    private ViewPort mViewPort = new ViewPort(0.0f, 1.0f, 0.0f, 1.0f, 0.0f, 1.0f);
    private boolean configChanged = true;
    final float color[] = { 0.75f, 0.75f, 0.75f, 1.0f };
    private Matrix4f matrix4f = new Matrix4f();

    public static class GridSpacing {
        public GridSpacing(float dx, float dy) { this.dx = dx; this.dy = dy; }
        public float dx;
        public float dy;
    }

    public static class ViewPort {
        ViewPort(float l, float r, float b, float t, float n, float f) {
            left = l;
            right = r;
            top = t;
            bottom = b;
            near = n;
            far = f;
        }
        public float left;
        public float right;
        public float bottom;
        public float top;
        public float near;
        public float far;
    }

    @Override
    public float[] getColor() {
        return color;
    }

    public Grid() {

    }

    public void setViewPort(ViewPort v) {
        mViewPort = v;
        configChanged = true;
    }

    public void setGridSpacing(GridSpacing sp) {
        mGridSpacing = sp;
        configChanged = true;
    }

    private void recompute() {
        int cols = (int) Math.ceil( (mViewPort.right - mViewPort.left) / mGridSpacing.dx );
        int rows = (int) Math.ceil( (mViewPort.top - mViewPort.bottom) / mGridSpacing.dy );
        if (cols > MAX_GRIDLINES) cols = 0; // grid is too dense; don't draw it
        if (rows > MAX_GRIDLINES) rows = 0;
        final float[] coords = createGrid(rows, cols, mViewPort.left, mViewPort.bottom,
                mGridSpacing.dx, mGridSpacing.dy);
        mArrayCount = coords.length/COORDS_PER_VERTEX;

        ByteBuffer bb = ByteBuffer.allocateDirect(coords.length * 4 /* sizeof(float) */);
        bb.order(ByteOrder.nativeOrder()); // use native hardware byte order
        mVertexBuffer = bb.asFloatBuffer(); // floating point buffer from the ByteBuffer
        mVertexBuffer.put(coords); // add the coordinates to the FloatBuffer
        mVertexBuffer.position(0); // set the buffer to read the first coordinate
        matrix4f.loadOrtho(mViewPort.left, mViewPort.right, mViewPort.bottom, mViewPort.top,
                mViewPort.near, mViewPort.far);
    }

    @Override
    public void onDraw() {
        if (configChanged) {
            recompute();
            configChanged = false;
        }
        final int vertexStride = COORDS_PER_VERTEX * 4 /* sizeof(float) */;
        GLES30.glUniformMatrix4fv(getMatrixHandle(), 1, false, matrix4f.getArray(), 0);
        GLES30.glUniform4fv(getColorHandle(), 1, color, 0); // Set color
        GLES30.glVertexAttribPointer(getPositionHandle(), COORDS_PER_VERTEX,
                GLES30.GL_FLOAT, false, vertexStride, mVertexBuffer);
        GLES30.glDrawArrays(GLES30.GL_LINES, 0, mArrayCount);
    }

    private static final float[] createGrid(int rows, int cols, float left, float bottom,
                                            float dx, float dy) {
        float result[] = new float[2 * COORDS_PER_VERTEX * (rows + cols)];
        int idx = 0;
        // horizontal lines
        for (int row = 0; row < rows; row++) {
            float y = bottom + row * dy;
            result[idx++] = left;
            result[idx++] = y;
            result[idx++] = 0.0f;

            result[idx++] = left + cols*dx;
            result[idx++] = y;
            result[idx++] = 0.0f;
        }
        // vertical lines
        for (int col = 0; col < cols; col++) {
            float x = left + col * dx;
            result[idx++] = x;
            result[idx++] = bottom;
            result[idx++] = 0.0f;

            result[idx++] = x;
            result[idx++] = bottom + rows*dy;
            result[idx++] = 0.0f;
        }
        return result;
    }
}