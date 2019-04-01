// Copyright © 2019 Barnaby Shearer <b@Zi.iS>

package is.zi.hue;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.PixelFormat;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import is.zi.NonNull;
import is.zi.Nullable;

public class HueColorPicker extends GLSurfaceView {

    private HueLightRenderer renderer;
    private OnColorListener listener;
    @Nullable
    private HueColor nextColor;
    private int interval;
    private boolean rateLimited;

    public HueColorPicker(Context context) {
        super(context);
        init();
    }

    public HueColorPicker(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        // Otherwise android:backgroundDimEnabled apples to us
        setZOrderOnTop(true);
        // Request translucent context
        setEGLConfigChooser(8, 8, 8, 8, 0, 0);
        getHolder().setFormat(PixelFormat.TRANSLUCENT);
        setEGLContextClientVersion(2);
        renderer = new HueLightRenderer();
        setRenderer(renderer);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

    @SuppressLint("ClickableViewAccessibility") // Alternative apps provide voice control
    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        super.onTouchEvent(event);
        renderer.onTouchEvent(event.getX(), event.getY());
        requestRender();
        return true;
    }

    private void onNext() {
        if (nextColor != null) {
            listener.onColorChange(nextColor);
            getHandler().postDelayed(this::onNext, interval);
            nextColor = null;
        } else {
            rateLimited = false;
        }
    }

    public void setOnColorListener(@NonNull OnColorListener listener, int interval) {
        this.listener = listener;
        this.interval = interval;
        renderer.setOnColorListener(color -> {
            if (!rateLimited) {
                listener.onColorChange(color);
                rateLimited = true;
                getHandler().postDelayed(this::onNext, interval);
            } else {
                nextColor = color;
            }
        });
    }

    public void setGamut(@NonNull float[] gamut) {
        renderer.setGamut(gamut);

    }

    public void setColor(@NonNull HueColor color) {
        renderer.setColor(color);
        requestRender();
    }

    public interface OnColorListener {
        void onColorChange(HueColor color);
    }


    static class HueLightRenderer implements GLSurfaceView.Renderer {
        @NonNull
        private final float[] vPMatrix = new float[16];
        @NonNull
        private final float[] projectionMatrix = new float[16];
        @NonNull
        private final float[] viewMatrix = new float[16];
        private float width = 1;
        private float height = 1;
        @NonNull
        private float[] gamut = new float[]{
                1, 0, 0, 0, 0, 1
        };
        private Gamut gamutDrawer;
        private Crosshair crosshairDrawer;
        private Luminescence luminescenceDrawer;
        private Bar barDrawer;
        private OnColorListener colorListener;
        private HueColor color;

        HueLightRenderer() {
            Matrix.setLookAtM(viewMatrix, 0, 0, 0, 2, 0f, 0f, 0f, 0.0f, 1.0f, 0.0f);
        }

        private static int loadShader(int type, String shaderCode) {
            int shader = GLES20.glCreateShader(type);
            GLES20.glShaderSource(shader, shaderCode);
            GLES20.glCompileShader(shader);
            int[] compiled = new int[1];
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
            if (compiled[0] == 0) {
                Log.e("HueColorPicker", "Could not compile shader " + shaderCode + ":");
                Log.e("HueColorPicker", " " + GLES20.glGetShaderInfoLog(shader));
                GLES20.glDeleteShader(shader);
                shader = 0;
            }
            return shader;
        }

        void onTouchEvent(float x, float y) {
            if (color != null) {
                if (mapY(y) < Math.min(gamut[1], Math.min(gamut[3], gamut[5]))) {
                    setColor(
                            new HueColor(
                                    color.x,
                                    color.y,
                                    mapLuminescence(x)
                            )
                    );
                } else {
                    setColor(
                            new HueColor(
                                    mapX(x),
                                    mapY(y),
                                    color.bri
                            )
                    );
                }
            }
        }


        float mapX(float v) {
            float min = Math.min(gamut[0], Math.min(gamut[2], gamut[4]));
            float max = Math.max(gamut[0], Math.max(gamut[2], gamut[4]));
            return min + (max - min) * v / width;
        }

        float mapY(float v) {
            float min = Math.min(gamut[1], Math.min(gamut[3], gamut[5]));
            float max = Math.max(gamut[1], Math.max(gamut[3], gamut[5]));
            return max + (min - max) * v / (height * 256 / 280);
        }

        int mapLuminescence(float v) {
            return Math.round(v * 256 / width);
        }

        void setGamut(float[] gamut) {
            // Note: We override the Z axis to be luminescence and map xy to CIE color-space
            this.gamut = gamut;
            Matrix.orthoM(
                    projectionMatrix,
                    0,
                    Math.min(gamut[0], Math.min(gamut[2], gamut[4])),
                    Math.max(gamut[0], Math.max(gamut[2], gamut[4])),
                    Math.min(gamut[1], Math.min(gamut[3], gamut[5])) - .109375f,
                    Math.max(gamut[1], Math.max(gamut[3], gamut[5])),
                    1,
                    3
            );
            Matrix.multiplyMM(vPMatrix, 0, projectionMatrix, 0, viewMatrix, 0);
        }

        void setColor(HueColor color) {
            this.color = color;
            colorListener.onColorChange(color);
        }

        void setOnColorListener(OnColorListener listener) {
            colorListener = listener;
        }

        @Override
        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            gamutDrawer = new Gamut();
            crosshairDrawer = new Crosshair();
            luminescenceDrawer = new Luminescence();
            barDrawer = new Bar();

            GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        }

        @Override
        public void onDrawFrame(GL10 unused) {
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
            gamutDrawer.draw(vPMatrix, gamut, 1.0f);
            luminescenceDrawer.draw();
            if (color != null) {
                crosshairDrawer.draw(vPMatrix, color.x, color.y);
                barDrawer.draw(color.bri / 256.0f);
            }
        }

        @Override
        public void onSurfaceChanged(GL10 unused, int width, int height) {
            GLES20.glViewport(0, 0, width, height);
            this.width = width;
            this.height = height;
        }


        static class Gamut {
            private final int program;
            private final int positionHandle;
            private final FloatBuffer vertexBuffer;
            private final int matrixHandle;

            Gamut() {
                ByteBuffer bb = ByteBuffer.allocateDirect(3 * 3 * 4);
                bb.order(ByteOrder.nativeOrder());
                vertexBuffer = bb.asFloatBuffer();

                program = GLES20.glCreateProgram();
                GLES20.glAttachShader(program, HueLightRenderer.loadShader(
                        GLES20.GL_VERTEX_SHADER,
                        "" +
                                "varying vec3 pos;\n" +
                                "attribute vec4 vPosition;\n" +
                                "uniform mat4 uMVPMatrix;\n" +
                                "void main() {\n" +
                                "    pos = vPosition.xyz;\n" +
                                "    gl_Position = uMVPMatrix * vPosition;\n" +
                                "}\n"
                ));
                GLES20.glAttachShader(program, HueLightRenderer.loadShader(
                        GLES20.GL_FRAGMENT_SHADER,
                        "" +
                                "precision mediump float;\n" +
                                "varying vec3 pos;\n" +
                                "vec3 Yxy2RGB(const vec3 xyY) {\n" +
                                "    vec3 v = xyY.z * vec3(\n" + // xyY -> XYZ
                                "       xyY.x / xyY.y,\n" +
                                "       1.0,\n" +
                                "       (1.0 - xyY.x - xyY.y) / xyY.y\n" +
                                "    ) * mat3(\n" + // D65 XYZ -> RGB matrix
                                "       3.2404542, -1.5371385, -0.4985314,\n" +
                                "       -0.9692660, 1.8760108, 0.0415560,\n" +
                                "       0.0556434, -0.2040259, 1.0572252\n" +
                                "    );\n" +
                                "   v /= max(v.x, max(v.y, v.z));" +
                                "   v *= xyY.z;" +
                                "   return v;" +
                                /* Empirically the sRGB gamma seems to be automatic
                                "    return vec3(\n" +
                                "        ( v.r > 0.0031308 ) ? (( 1.055 * pow( v.r, ( 1.0 / 2.4 ))) - 0.055 ) : 12.92 * v.r," +
                                "        ( v.g > 0.0031308 ) ? (( 1.055 * pow( v.g, ( 1.0 / 2.4 ))) - 0.055 ) : 12.92 * v.g," +
                                "        ( v.b > 0.0031308 ) ? (( 1.055 * pow( v.b, ( 1.0 / 2.4 ))) - 0.055 ) : 12.92 * v.b" +
                                "    );\n" + /*/
                                "}\n" +
                                "void main() {\n" +
                                "    gl_FragColor = vec4(Yxy2RGB(pos), 1.0);\n" +
                                "}\n"
                ));

                GLES20.glLinkProgram(program);
                positionHandle = GLES20.glGetAttribLocation(program, "vPosition");
                matrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix");

            }


            void draw(float[] vPMatrix, float[] gamut, @SuppressWarnings("SameParameterValue") float luminescence) {
                GLES20.glUseProgram(program);

                GLES20.glEnableVertexAttribArray(positionHandle);
                vertexBuffer.put(new float[]{
                        gamut[0], gamut[1], luminescence,
                        gamut[2], gamut[3], luminescence,
                        gamut[4], gamut[5], luminescence,
                });
                vertexBuffer.position(0);
                GLES20.glVertexAttribPointer(
                        positionHandle,
                        3,
                        GLES20.GL_FLOAT,
                        false,
                        3 * 4,
                        vertexBuffer
                );

                GLES20.glUniformMatrix4fv(matrixHandle, 1, false, vPMatrix, 0);

                GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 3);

                GLES20.glDisableVertexAttribArray(positionHandle);

            }

        }

        static class Luminescence {
            private final int program;
            private final int positionHandle;
            private final FloatBuffer vertexBuffer;


            Luminescence() {
                ByteBuffer bb = ByteBuffer.allocateDirect(4 * 3 * 4);
                bb.order(ByteOrder.nativeOrder());
                vertexBuffer = bb.asFloatBuffer();
                vertexBuffer.put(new float[]{
                        .9f, -1f + 0.01f, 1.0f,
                        .9f, -1f + 0.09f, 1.0f,
                        -.9f, -1f + 0.01f, 0.0f,
                        -.9f, -1f + 0.09f, 0.0f,
                });
                vertexBuffer.position(0);

                program = GLES20.glCreateProgram();
                GLES20.glAttachShader(program, HueLightRenderer.loadShader(
                        GLES20.GL_VERTEX_SHADER,
                        "" +
                                "attribute vec4 vPosition;\n" +
                                "uniform mat4 uMVPMatrix;\n" +
                                "varying float lum;\n" +
                                "void main() {\n" +
                                "    lum = vPosition.z;\n" +
                                "    gl_Position = vPosition;\n" +
                                "}\n"
                ));
                GLES20.glAttachShader(program, HueLightRenderer.loadShader(
                        GLES20.GL_FRAGMENT_SHADER,
                        "" +
                                "precision mediump float;\n" +
                                "varying float lum;\n" +
                                "void main() {\n" +
                                "    gl_FragColor = vec4(vec3(lum), 1.0);\n" +
                                "}\n"
                ));

                GLES20.glLinkProgram(program);
                positionHandle = GLES20.glGetAttribLocation(program, "vPosition");
            }


            void draw() {
                GLES20.glUseProgram(program);

                GLES20.glEnableVertexAttribArray(positionHandle);

                GLES20.glVertexAttribPointer(
                        positionHandle,
                        3,
                        GLES20.GL_FLOAT,
                        false,
                        3 * 4,
                        vertexBuffer
                );

                GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

                GLES20.glDisableVertexAttribArray(positionHandle);
            }

        }

        static class Crosshair {
            private final int program;
            private final int positionHandle;
            private final int textureHandle;
            private final FloatBuffer vertexBuffer;
            private final FloatBuffer textureBuffer;
            private final int matrixHandle;


            Crosshair() {
                ByteBuffer bb = ByteBuffer.allocateDirect(3 * 3 * 4);
                bb.order(ByteOrder.nativeOrder());
                vertexBuffer = bb.asFloatBuffer();

                ByteBuffer bb2 = ByteBuffer.allocateDirect(3 * 2 * 4);
                bb2.order(ByteOrder.nativeOrder());
                textureBuffer = bb2.asFloatBuffer();

                program = GLES20.glCreateProgram();
                GLES20.glAttachShader(program, HueLightRenderer.loadShader(
                        GLES20.GL_VERTEX_SHADER,
                        "" +
                                "attribute vec4 vPosition;\n" +
                                "attribute vec2 vTexture;\n" +
                                "uniform mat4 uMVPMatrix;\n" +
                                "varying vec2 texture;\n" +
                                "void main() {\n" +
                                "    texture = vTexture;\n" +
                                "    gl_Position = uMVPMatrix * vPosition;\n" +
                                "}\n"
                ));
                GLES20.glAttachShader(program, HueLightRenderer.loadShader(
                        GLES20.GL_FRAGMENT_SHADER,
                        "" +
                                "precision mediump float;\n" +
                                "varying vec2 texture;\n" +
                                "void main() {\n" +
                                "    float r = (texture.x * texture.x) + (texture.y * texture.y);\n" +
                                "    if (r <= 1.0 && r >= 0.5) {\n" +
                                "        gl_FragColor = vec4(0.2, 0.4, 0.2, 1.0);\n" +
                                "    } else {\n" +
                                "       discard;\n" +
                                "    }\n" +
                                "}\n"
                ));

                GLES20.glLinkProgram(program);
                positionHandle = GLES20.glGetAttribLocation(program, "vPosition");
                textureHandle = GLES20.glGetAttribLocation(program, "vTexture");
                matrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix");
            }


            void draw(float[] vPMatrix, float x, float y) {
                GLES20.glUseProgram(program);

                GLES20.glEnableVertexAttribArray(positionHandle);
                vertexBuffer.put(new float[]{
                        x + .02f, y + .02f, 1.0f,
                        x, y - .04f, 1.0f,
                        x - .02f, y + .02f, 1.0f,
                });
                vertexBuffer.position(0);
                GLES20.glVertexAttribPointer(
                        positionHandle,
                        3,
                        GLES20.GL_FLOAT,
                        false,
                        3 * 4,
                        vertexBuffer
                );

                GLES20.glEnableVertexAttribArray(textureHandle);
                textureBuffer.put(new float[]{
                        2, 2,
                        0, -4,
                        -2, 2
                });
                textureBuffer.position(0);
                GLES20.glVertexAttribPointer(
                        textureHandle,
                        2,
                        GLES20.GL_FLOAT,
                        false,
                        2 * 4,
                        textureBuffer
                );

                GLES20.glUniformMatrix4fv(matrixHandle, 1, false, vPMatrix, 0);

                GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 3);

                GLES20.glDisableVertexAttribArray(positionHandle);
                GLES20.glDisableVertexAttribArray(textureHandle);
            }

        }

        static class Bar {
            private final int program;
            private final int positionHandle;
            private final FloatBuffer vertexBuffer;

            Bar() {
                ByteBuffer bb = ByteBuffer.allocateDirect(2 * 3 * 4);
                bb.order(ByteOrder.nativeOrder());
                vertexBuffer = bb.asFloatBuffer();

                program = GLES20.glCreateProgram();
                GLES20.glAttachShader(program, HueLightRenderer.loadShader(
                        GLES20.GL_VERTEX_SHADER,
                        "" +
                                "attribute vec4 vPosition;\n" +
                                "void main() {\n" +
                                "    gl_Position = vPosition;\n" +
                                "}\n"
                ));
                GLES20.glAttachShader(program, HueLightRenderer.loadShader(
                        GLES20.GL_FRAGMENT_SHADER,
                        "" +
                                "precision mediump float;\n" +
                                "void main() {\n" +
                                "    gl_FragColor = vec4(1.0, 0.0, 0.0, 1.0);\n" +
                                "}\n"
                ));

                GLES20.glLinkProgram(program);
                positionHandle = GLES20.glGetAttribLocation(program, "vPosition");
            }


            void draw(float luminescence) {
                GLES20.glUseProgram(program);

                GLES20.glEnableVertexAttribArray(positionHandle);
                vertexBuffer.put(new float[]{
                        luminescence * 1.8f - .9f, -1f, 1.0f,
                        luminescence * 1.8f - .9f, -1f + 0.1f, 1.0f,
                });
                vertexBuffer.position(0);
                GLES20.glVertexAttribPointer(
                        positionHandle,
                        3,
                        GLES20.GL_FLOAT,
                        false,
                        3 * 4,
                        vertexBuffer
                );

                GLES20.glLineWidth(5f);
                GLES20.glDrawArrays(GLES20.GL_LINES, 0, 2);

                GLES20.glDisableVertexAttribArray(positionHandle);
            }

        }
    }

}

