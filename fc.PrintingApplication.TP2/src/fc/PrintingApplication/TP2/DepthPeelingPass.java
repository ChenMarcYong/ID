package fc.PrintingApplication.TP2;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL15.GL_QUERY_RESULT;
import static org.lwjgl.opengl.GL15.glBeginQuery;
import static org.lwjgl.opengl.GL15.glDeleteQueries;
import static org.lwjgl.opengl.GL15.glEndQuery;
import static org.lwjgl.opengl.GL15.glGenQueries;
import static org.lwjgl.opengl.GL15.glGetQueryObjectuiv;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL32.*;
import static org.lwjgl.opengl.GL33.*;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import org.lwjgl.BufferUtils;

public final class DepthPeelingPass {
    // FBO & textures
    private int fbo = 0;
    private int texZ = 0;       // Z courant (R32F)
    private int texPrevZ = 0;   // Z précédent (R32F)
    private int texMask = 0;    // masque slice (R8)
    private int rboDS = 0;      // depth+stencil
    private int W, H;

    // Shaders
    private int progPeel1 = 0;
    private int progPeelK = 0;
    private int progSlice = 0;

    // Uniforms communs
    private int locM, locV, locP; // dans peel1/peelK
    private int locTexPrevZ, locResolution, locZsliceDepth; // peelK, slice
    private int locTexZ0, locTexZ1, locResSlice, locZslice; // slice

    // Matrices (column-major float[16])
    private float[] uModel = identity();
    private float[] uView  = identity();
    private float[] uProj  = identity();

    public void init(int width, int height) {
        this.W = width; this.H = height;

        // --- FBO + textures ---
        fbo = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, fbo);

        // texZ
        texZ = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, texZ);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_R32F, W, H, 0, GL_RED, GL_FLOAT, (ByteBuffer) null);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, texZ, 0);

        // texMask
        texMask = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, texMask);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_R8, W, H, 0, GL_RED, GL_UNSIGNED_BYTE, (ByteBuffer) null);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT1, GL_TEXTURE_2D, texMask, 0);

        // depth-stencil
        rboDS = glGenRenderbuffers();
        glBindRenderbuffer(GL_RENDERBUFFER, rboDS);
        glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH24_STENCIL8, W, H);
        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_STENCIL_ATTACHMENT, GL_RENDERBUFFER, rboDS);

        glDrawBuffers(new int[]{ GL_COLOR_ATTACHMENT0, GL_COLOR_ATTACHMENT1 });

        if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE)
            throw new IllegalStateException("FBO incomplete");
        glBindFramebuffer(GL_FRAMEBUFFER, 0);

        // texPrevZ initialisé à 0.0
        texPrevZ = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, texPrevZ);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_R32F, W, H, 0, GL_RED, GL_FLOAT, (ByteBuffer) null);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        FloatBuffer zeros = BufferUtils.createFloatBuffer(W * H);
        for (int i = 0; i < W * H; i++) zeros.put(0f);
        zeros.flip();
        glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, W, H, GL_RED, GL_FLOAT, zeros);

        // --- Shaders ---
        progPeel1 = link(vertMeshSrc, fragPeel1Src);
        progPeelK = link(vertMeshSrc, fragPeelKSrc);
        progSlice = link(vertFullSrc, fragSliceSrc);

        // cache des locations
        glUseProgram(progPeel1);
        locM = glGetUniformLocation(progPeel1, "uModel");
        locV = glGetUniformLocation(progPeel1, "uView");
        locP = glGetUniformLocation(progPeel1, "uProj");

        glUseProgram(progPeelK);
        locTexPrevZ   = glGetUniformLocation(progPeelK, "texPrevZ");
        locResolution = glGetUniformLocation(progPeelK, "uResolution");
        locZsliceDepth= glGetUniformLocation(progPeelK, "uZsliceDepth");

        glUseProgram(progSlice);
        locTexZ0   = glGetUniformLocation(progSlice, "texZk");
        locTexZ1   = glGetUniformLocation(progSlice, "texZk1");
        locResSlice= glGetUniformLocation(progSlice, "uResolution");
        locZslice  = glGetUniformLocation(progSlice, "uZsliceDepth");

        glUseProgram(0);
    }

    public void setMatrices(float[] model, float[] view, float[] proj) {
        this.uModel = model != null ? model : identity();
        this.uView  = view  != null ? view  : identity();
        this.uProj  = proj  != null ? proj  : identity();
    }

    /** Passe 1 : rend la première couche (plus proche) et écrit gl_FragCoord.z dans texZ. */
    public void peelFirstPass(int vao, int indexCount) {
        glBindFramebuffer(GL_FRAMEBUFFER, fbo);
        glViewport(0, 0, W, H);
        glDisable(GL_CULL_FACE);
        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LESS);

        glDrawBuffers(new int[]{ GL_COLOR_ATTACHMENT0, GL_COLOR_ATTACHMENT1 });
        glClearColor(0, 0, 0, 0);
        glClearDepth(1.0);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT | GL_STENCIL_BUFFER_BIT);

        glUseProgram(progPeel1);
        uploadMats(progPeel1, uModel, uView, uProj);

        glBindVertexArray(vao);
        glDrawElements(GL_TRIANGLES, indexCount, GL_UNSIGNED_INT, 0);
        glBindVertexArray(0);
        glUseProgram(0);

        // copie texZ -> texPrevZ (pour préparer la passe suivante)
        copyTexture(texZ, texPrevZ);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    /** Passe k≥2 : évince (discard) tout fragment avec z <= texPrevZ, écrit les nouveaux z dans texZ. */
    public boolean peelNextPass(int vao, int indexCount, float zSliceDepth, boolean accumulateSlice) {
        glBindFramebuffer(GL_FRAMEBUFFER, fbo);
        glViewport(0, 0, W, H);
        glClearDepth(1.0);
        glClear(GL_DEPTH_BUFFER_BIT);

        glUseProgram(progPeelK);
        // Uniforms
        glUniform2f(locResolution, W, H);
        glUniform1f(locZsliceDepth, zSliceDepth);
        uploadMats(progPeelK, uModel, uView, uProj);

        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, texPrevZ);
        glUniform1i(locTexPrevZ, 0);

        // Si on n'accumule pas la slice ici, on peut désactiver l'écriture sur COLOR1.
        glDrawBuffers(new int[]{ GL_COLOR_ATTACHMENT0, GL_COLOR_ATTACHMENT1 });

        // Query pour savoir si des fragments passent (arrêt si 0)
        int query = glGenQueries();
        glBeginQuery(GL_ANY_SAMPLES_PASSED, query);

        glBindVertexArray(vao);
        glDrawElements(GL_TRIANGLES, indexCount, GL_UNSIGNED_INT, 0);
        glBindVertexArray(0);

        glEndQuery(GL_ANY_SAMPLES_PASSED);
        int[] any = new int[1];
        glGetQueryObjectuiv(query, GL_QUERY_RESULT, any);
        glDeleteQueries(query);

        // maj texPrevZ
        copyTexture(texZ, texPrevZ);
        glUseProgram(0);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        return any[0] != 0;
    }

    /** Passe slice (optionnelle) : lit deux cartes Z et écrit le masque dans texMask. */
    public void sliceBetweenLastTwo(float zSliceDepth) {
        glBindFramebuffer(GL_FRAMEBUFFER, fbo);
        glViewport(0, 0, W, H);

        // On veut écrire uniquement le masque
        glDrawBuffers(new int[]{ GL_COLOR_ATTACHMENT1 });

        glDisable(GL_DEPTH_TEST); // full-screen triangle
        glUseProgram(progSlice);

        glUniform2f(locResSlice, W, H);
        glUniform1f(locZslice, zSliceDepth);

        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, texPrevZ);
        glUniform1i(locTexZ0, 0);

        glActiveTexture(GL_TEXTURE1);
        glBindTexture(GL_TEXTURE_2D, texZ);
        glUniform1i(locTexZ1, 1);

        drawFullscreenTriangle();

        glUseProgram(0);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    /** Lecture CPU du masque (R8) → ByteBuffer (0/255). */
    public ByteBuffer readMask() {
        ByteBuffer buf = BufferUtils.createByteBuffer(W * H);
        glBindFramebuffer(GL_FRAMEBUFFER, fbo);
        glReadBuffer(GL_COLOR_ATTACHMENT1);
        glReadPixels(0, 0, W, H, GL_RED, GL_UNSIGNED_BYTE, buf);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        return buf;
    }

    public void dispose() {
        if (progPeel1 != 0) glDeleteProgram(progPeel1);
        if (progPeelK != 0) glDeleteProgram(progPeelK);
        if (progSlice != 0) glDeleteProgram(progSlice);
        if (texZ != 0) glDeleteTextures(texZ);
        if (texPrevZ != 0) glDeleteTextures(texPrevZ);
        if (texMask != 0) glDeleteTextures(texMask);
        if (rboDS != 0) glDeleteRenderbuffers(rboDS);
        if (fbo != 0) glDeleteFramebuffers(fbo);
    }

    // ========= Helpers =========


    public void resetForNewSlice() {
        // clear mask (COLOR1)
        glBindFramebuffer(GL_FRAMEBUFFER, fbo);
        glDrawBuffers(new int[]{ GL_COLOR_ATTACHMENT1 });
        glClearColor(0,0,0,0);
        glClear(GL_COLOR_BUFFER_BIT);

        // clear texPrevZ à 0 (via FBO temporaire)
        int tmp = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, tmp);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, texPrevZ, 0);
        glDrawBuffers(new int[]{ GL_COLOR_ATTACHMENT0 });
        glClearColor(0,0,0,0);
        glClear(GL_COLOR_BUFFER_BIT);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glDeleteFramebuffers(tmp);
    }


    private static void uploadMats(int program, float[] M, float[] V, float[] P) {
        int locM = glGetUniformLocation(program, "uModel");
        int locV = glGetUniformLocation(program, "uView");
        int locP = glGetUniformLocation(program, "uProj");
        FloatBuffer fb = org.lwjgl.BufferUtils.createFloatBuffer(16);

        // upload M
        fb.clear();          // returns Buffer, but we ignore the return value
        fb.put(M);           // now OK: method of FloatBuffer
        fb.flip();
        glUniformMatrix4fv(locM, false, fb);
        // upload V
        fb.clear();
        fb.put(V);
        fb.flip();
        glUniformMatrix4fv(locV, false, fb);
        // upload P
        fb.clear();
        fb.put(P);
        fb.flip();
        glUniformMatrix4fv(locP, false, fb);
    }

    private static void copyTexture(int srcTex, int dstTex) {
        // Méthode portable via FS full-screen :
        int progCopy = link(vertFullSrc, fragCopySrc);
        glUseProgram(progCopy);
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, srcTex);
        int loc = glGetUniformLocation(progCopy, "srcTex");
        glUniform1i(loc, 0);

        // Attache temporairement dstTex sur COLOR0 puis draw fullscreen
        int tmpFbo = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, tmpFbo);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, dstTex, 0);
        glDrawBuffers(new int[]{ GL_COLOR_ATTACHMENT0 });
        drawFullscreenTriangle();
        glBindFramebuffer(GL_FRAMEBUFFER, 0);

        glDeleteFramebuffers(tmpFbo);
        glUseProgram(0);
        glDeleteProgram(progCopy);
    }

    private static void drawFullscreenTriangle() {
        // Pas de VAO nécessaire si core profile 3.3+ avec gl_VertexID
        glBindVertexArray(0);
        glDrawArrays(GL_TRIANGLES, 0, 3);
    }

    private static int link(String vs, String fs) {
        int v = glCreateShader(GL_VERTEX_SHADER);
        glShaderSource(v, vs); glCompileShader(v);
        if (glGetShaderi(v, GL_COMPILE_STATUS) == GL_FALSE)
            throw new RuntimeException(glGetShaderInfoLog(v));

        int f = glCreateShader(GL_FRAGMENT_SHADER);
        glShaderSource(f, fs); glCompileShader(f);
        if (glGetShaderi(f, GL_COMPILE_STATUS) == GL_FALSE)
            throw new RuntimeException(glGetShaderInfoLog(f));

        int p = glCreateProgram();
        glAttachShader(p, v); glAttachShader(p, f);
        glLinkProgram(p);
        if (glGetProgrami(p, GL_LINK_STATUS) == GL_FALSE)
            throw new RuntimeException(glGetProgramInfoLog(p));
        glDeleteShader(v); glDeleteShader(f);
        return p;
    }

    private static float[] identity() {
        return new float[]{
            1,0,0,0,
            0,1,0,0,
            0,0,1,0,
            0,0,0,1
        };
    }

    // ===== Shaders =====

    // VS mesh (positions en location=0)
    private static final String vertMeshSrc = "#version 330 core\n" +
        "layout(location=0) in vec3 inPos;\n" +
        "uniform mat4 uModel, uView, uProj;\n" +
        "void main(){ gl_Position = uProj * uView * uModel * vec4(inPos,1.0); }\n";

    // FS pass1 : écrire gl_FragCoord.z dans Color0, rien dans Color1
    private static final String fragPeel1Src = "#version 330 core\n" +
        "layout(location=0) out float outZ;\n" +
        "layout(location=1) out float outMask;\n" +
        "void main(){ outZ = gl_FragCoord.z; outMask = 0.0; }\n";

    // FS pass k≥2 : discard si z <= zPrev, écrire z courant (+ option slice live via uZsliceDepth)
    private static final String fragPeelKSrc = "#version 330 core\n" +
        "uniform sampler2D texPrevZ;\n" +
        "uniform vec2 uResolution;\n" +
        "uniform float uZsliceDepth; // si tu veux slice live, sinon inutile\n" +
        "layout(location=0) out float outZ;\n" +
        "layout(location=1) out float outMask;\n" +
        "void main(){\n" +
        "  ivec2 p = ivec2(gl_FragCoord.xy);\n" +
        "  float zPrev = texelFetch(texPrevZ, p, 0).r;\n" +
        "  if (gl_FragCoord.z <= zPrev + 1e-7) discard;\n" +
        "  outZ = gl_FragCoord.z;\n" +
        "  bool inside = (uZsliceDepth > zPrev + 1e-7) && (uZsliceDepth < gl_FragCoord.z - 1e-7);\n" +
        "  outMask = inside ? 1.0 : 0.0;\n" + // si tu n'accumules pas ici, mets 0.0
        "}\n";

    // VS plein écran via gl_VertexID
    private static final String vertFullSrc = "#version 330 core\n" +
        "const vec2 verts[3]=vec2[3](vec2(-1,-1), vec2(3,-1), vec2(-1,3));\n" +
        "out vec2 uv;\n" +
        "void main(){ gl_Position = vec4(verts[gl_VertexID],0,1); uv = 0.5*(gl_Position.xy+1.0); }\n";

    // FS slice : garde pixel si zPrev < zSlice < zCurr → écrit 1 dans le masque
    private static final String fragSliceSrc = "#version 330 core\n" +
        "uniform sampler2D texZk;\n" +
        "uniform sampler2D texZk1;\n" +
        "uniform vec2 uResolution;\n" +
        "uniform float uZsliceDepth;\n" +
        "layout(location=1) out float outMask;\n" +
        "void main(){\n" +
        "  ivec2 p = ivec2(gl_FragCoord.xy);\n" +
        "  float z0 = texelFetch(texZk,  p, 0).r;\n" +
        "  float z1 = texelFetch(texZk1, p, 0).r;\n" +
        "  if (!(uZsliceDepth > z0 + 1e-7 && uZsliceDepth < z1 - 1e-7)) discard;\n" +
        "  outMask = 1.0;\n" +
        "}\n";

    // FS copy R32F -> R32F
    private static final String fragCopySrc = "#version 330 core\n" +
        "uniform sampler2D srcTex;\n" +
        "layout(location=0) out float outZ;\n" +
        "void main(){ outZ = texelFetch(srcTex, ivec2(gl_FragCoord.xy), 0).r; }\n";
}
