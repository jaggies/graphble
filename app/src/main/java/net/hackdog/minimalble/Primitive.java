package net.hackdog.minimalble;

import android.opengl.GLES30;

/**
 * Created by jmiller on 7/30/16.
 */
public abstract class Primitive {
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
    private int mProgram;

    public abstract void draw();
    public abstract float[] getColor();

    Primitive() {
        int vertexShader = loadVertexShader();
        int fragmentShader = loadFragmentShader();

        mProgram = GLES30.glCreateProgram(); // create empty OpenGL ES Program
        GLES30.glAttachShader(mProgram, vertexShader); // add the vertex shader to program
        GLES30.glAttachShader(mProgram, fragmentShader); // add the fragment shader to program
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
