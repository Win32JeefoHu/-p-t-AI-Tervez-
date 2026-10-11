package com.nagyattila.epitoaitervezo.ar;

import android.opengl.GLES20;
import android.util.Log;
import com.google.ar.core.Coordinates2d;
import com.google.ar.core.Frame;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public final class BackgroundRenderer {
    private static final String FRAGMENT_SHADER = "#extension GL_OES_EGL_image_external : require\nprecision mediump float;\nvarying vec2 v_TexCoord;\nuniform samplerExternalOES sTexture;\nvoid main() {\n  gl_FragColor = texture2D(sTexture, v_TexCoord);\n}\n";
    private static final float[] QUAD_COORDS = {-1.0f, -1.0f, 1.0f, -1.0f, -1.0f, 1.0f, 1.0f, 1.0f};
    private static final String TAG = "BackgroundRenderer";
    private static final String VERTEX_SHADER = "attribute vec4 a_Position;\nattribute vec2 a_TexCoord;\nvarying vec2 v_TexCoord;\nvoid main() {\n  gl_Position = a_Position;\n  v_TexCoord = a_TexCoord;\n}\n";
    private FloatBuffer quadCoords;
    private int quadProgram;
    private FloatBuffer quadTexCoords;
    private boolean textureInitialized;
    private int positionAttrib = -1;
    private int texCoordAttrib = -1;
    private int textureId = -1;

    private static int loadShader(int i2, String str) {
        int glCreateShader = GLES20.glCreateShader(i2);
        GLES20.glShaderSource(glCreateShader, str);
        GLES20.glCompileShader(glCreateShader);
        int[] iArr = new int[1];
        GLES20.glGetShaderiv(glCreateShader, 35713, iArr, 0);
        if (iArr[0] != 0) {
            return glCreateShader;
        }
        String glGetShaderInfoLog = GLES20.glGetShaderInfoLog(glCreateShader);
        Log.e(TAG, "Shader compile failed: " + glGetShaderInfoLog);
        GLES20.glDeleteShader(glCreateShader);
        throw new RuntimeException("Shader compile failed: " + glGetShaderInfoLog);
    }

    public void createOnGlThread() {
        int[] iArr = new int[1];
        GLES20.glGenTextures(1, iArr, 0);
        int i2 = iArr[0];
        this.textureId = i2;
        GLES20.glBindTexture(36197, i2);
        GLES20.glTexParameteri(36197, 10242, 33071);
        GLES20.glTexParameteri(36197, 10243, 33071);
        GLES20.glTexParameteri(36197, 10241, 9729);
        GLES20.glTexParameteri(36197, 10240, 9729);
        float[] fArr = QUAD_COORDS;
        ByteBuffer allocateDirect = ByteBuffer.allocateDirect(fArr.length * 4);
        allocateDirect.order(ByteOrder.nativeOrder());
        FloatBuffer asFloatBuffer = allocateDirect.asFloatBuffer();
        this.quadCoords = asFloatBuffer;
        asFloatBuffer.put(fArr);
        this.quadCoords.position(0);
        ByteBuffer allocateDirect2 = ByteBuffer.allocateDirect(32);
        allocateDirect2.order(ByteOrder.nativeOrder());
        this.quadTexCoords = allocateDirect2.asFloatBuffer();
        int loadShader = loadShader(35633, VERTEX_SHADER);
        int loadShader2 = loadShader(35632, FRAGMENT_SHADER);
        int glCreateProgram = GLES20.glCreateProgram();
        this.quadProgram = glCreateProgram;
        GLES20.glAttachShader(glCreateProgram, loadShader);
        GLES20.glAttachShader(this.quadProgram, loadShader2);
        GLES20.glLinkProgram(this.quadProgram);
        int[] iArr2 = new int[1];
        GLES20.glGetProgramiv(this.quadProgram, 35714, iArr2, 0);
        if (iArr2[0] == 1) {
            this.positionAttrib = GLES20.glGetAttribLocation(this.quadProgram, "a_Position");
            this.texCoordAttrib = GLES20.glGetAttribLocation(this.quadProgram, "a_TexCoord");
            this.textureInitialized = true;
            Log.i(TAG, "BackgroundRenderer ready textureId=" + this.textureId);
            return;
        }
        String glGetProgramInfoLog = GLES20.glGetProgramInfoLog(this.quadProgram);
        Log.e(TAG, "Program link failed: " + glGetProgramInfoLog);
        throw new RuntimeException("Background program link failed: " + glGetProgramInfoLog);
    }

    public void draw(Frame frame) {
        if (frame == null || !this.textureInitialized) {
            return;
        }
        frame.hasDisplayGeometryChanged();
        frame.transformCoordinates2d(Coordinates2d.OPENGL_NORMALIZED_DEVICE_COORDINATES, this.quadCoords, Coordinates2d.TEXTURE_NORMALIZED, this.quadTexCoords);
        if (frame.getTimestamp() == 0) {
            return;
        }
        this.quadTexCoords.position(0);
        this.quadCoords.position(0);
        GLES20.glDisable(2929);
        GLES20.glDepthMask(false);
        GLES20.glActiveTexture(33984);
        GLES20.glBindTexture(36197, this.textureId);
        GLES20.glUseProgram(this.quadProgram);
        GLES20.glUniform1i(GLES20.glGetUniformLocation(this.quadProgram, "sTexture"), 0);
        GLES20.glVertexAttribPointer(this.positionAttrib, 2, 5126, false, 0, (Buffer) this.quadCoords);
        GLES20.glVertexAttribPointer(this.texCoordAttrib, 2, 5126, false, 0, (Buffer) this.quadTexCoords);
        GLES20.glEnableVertexAttribArray(this.positionAttrib);
        GLES20.glEnableVertexAttribArray(this.texCoordAttrib);
        GLES20.glDrawArrays(5, 0, 4);
        GLES20.glDisableVertexAttribArray(this.positionAttrib);
        GLES20.glDisableVertexAttribArray(this.texCoordAttrib);
        GLES20.glDepthMask(true);
        GLES20.glEnable(2929);
    }

    public int getTextureId() {
        return this.textureId;
    }

    public boolean isTextureInitialized() {
        return this.textureInitialized;
    }
}
