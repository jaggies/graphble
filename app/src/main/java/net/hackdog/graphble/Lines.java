package net.hackdog.graphble;

import android.opengl.GLES30;
import android.renderscript.Matrix4f;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Arrays;

/**
 * Created by jmiller on 7/30/16.
 */
public class Lines extends Primitive {
    private static final int MAX_GRIDLINES = 100;
    private FloatBuffer mVertexBuffer;
    private int mArrayCount;
    private ViewPort mViewPort = new ViewPort(0.0f, 1.0f, 0.0f, 1.0f, 0.0f, 1.0f);
    private boolean configChanged = true;
    final float color[] = { 0.75f, 0.0f, 0.0f, 1.0f };
    private Matrix4f matrix4f = new Matrix4f();
    byte mData[] = {0};

    @Override
    public float[] getColor() {
        return color;
    }

    public Lines() {

    }

    public void setViewPort(ViewPort v) {
        mViewPort = v;
        configChanged = true;
    }

    public void setData(byte[] data) {
        mData = Arrays.copyOf(data, data.length);
        configChanged = true;
    }

    private void recompute() {
        float dx = (mViewPort.right - mViewPort.left);
        float dy = (mViewPort.top - mViewPort.bottom);

        final float[] coords = createLines(dx, dy, mData);
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
        GLES30.glLineWidth(2.0f);
        GLES30.glUniformMatrix4fv(getMatrixHandle(), 1, false, matrix4f.getArray(), 0);
        GLES30.glUniform4fv(getColorHandle(), 1, color, 0); // Set color
        GLES30.glVertexAttribPointer(getPositionHandle(), COORDS_PER_VERTEX,
                GLES30.GL_FLOAT, false, vertexStride, mVertexBuffer);
        GLES30.glDrawArrays(GLES30.GL_LINE_STRIP, 0, mArrayCount);
    }

    private static final float[] createLines(float dx, float dy, byte[] data) {
        float result[] = new float[COORDS_PER_VERTEX * data.length];
        int idx = 0;
        for (int i = 0; i < data.length; i++) {
            result[idx++] = dx * i / (data.length - 1); // 0.0 .. 1.0
            result[idx++] = dy * data[i] / 255.0f; // -1.0 .. 1.0
            result[idx++] = 0.0f;
        }
        return result;
    }
}