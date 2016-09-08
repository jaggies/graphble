package net.hackdog.graphble;

import android.opengl.GLES30;
import android.renderscript.Matrix4f;

/**
 * Created by jmiller on 7/30/16.
 */
public abstract class Primitive {
    public static final int COORDS_PER_VERTEX = 3;

    private final String defaultVertexShader =
            "attribute vec4 vPosition;" +
                    "uniform mat4 matrix;" +
                    "void main() {" +
                    "gl_Position = matrix * vPosition;" +
                    "}";

    private final String defaultFragmentShader =
            "precision mediump float;" +
                    "uniform vec4 vColor;" +
                    "void main() {" +
                    "  gl_FragColor = vColor;" +
                    "}";

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

    private int mProgram;
    private int mPositionHandle;
    private int mColorHandle;
    private int mMatrixHandle;

    public abstract void onDraw();
    public abstract float[] getColor();

    Primitive() {
        mProgram = GLES30.glCreateProgram(); // create empty OpenGL ES Program
        GLES30.glAttachShader(mProgram, loadVertexShader()); // add the vertex shader to program
        GLES30.glAttachShader(mProgram, loadFragmentShader()); // add the fragment shader to program
        GLES30.glLinkProgram(mProgram); // creates OpenGL ES program executables
    }

    public String getVertexShader() {
        return defaultVertexShader;
    }

    public String getFragmentShader() {
        return defaultFragmentShader;
    }

    public int loadVertexShader() {
        return loadShader(GLES30.GL_VERTEX_SHADER, getVertexShader());
    }

    public int loadFragmentShader() {
        return loadShader(GLES30.GL_FRAGMENT_SHADER, getFragmentShader());
    }

    public void draw() {
        // Setup
        GLES30.glUseProgram(mProgram); // Add program to OpenGL ES environment
        mPositionHandle = GLES30.glGetAttribLocation(mProgram, "vPosition");
        mMatrixHandle = GLES30.glGetUniformLocation(mProgram, "matrix");
        mColorHandle = GLES30.glGetUniformLocation(mProgram, "vColor");
        GLES30.glEnableVertexAttribArray(mPositionHandle);

        // Client draw
        onDraw();

        // Cleanup
        GLES30.glDisableVertexAttribArray(mPositionHandle); // Disable vertex array
    }

    public int getPositionHandle() {
        return mPositionHandle;
    }

    public int getColorHandle() {
        return mColorHandle;
    }

    public int getMatrixHandle() {
        return mMatrixHandle;
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
}
