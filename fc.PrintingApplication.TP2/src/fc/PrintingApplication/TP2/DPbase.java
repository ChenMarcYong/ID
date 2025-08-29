package fc.PrintingApplication.TP2;

// --- OpenGL LWJGL ---
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL14.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL32.*;

// --- Buffers utilitaires LWJGL ---
import org.lwjgl.BufferUtils;

import fc.PrintingApplication.TP2.Main.Render;

import java.awt.font.NumericShaper.Range;
// --- Java ---
import java.nio.IntBuffer;

abstract class DPbase {

    // ---- Params communs ----
    protected final int width, height;
    protected final float zMinWorld, zMaxWorld;
    protected final float epsZ = 1e-6f;
    protected final float maxDistColor = 0.80f; // mm

    // ---- Mesh ----
    protected int meshVao = 0;
    protected int meshIndexCount = 0;
    protected int meshMode = GL_TRIANGLES;

    // ---- GL objects communs ----
    protected int fbo = 0;
    protected int texZLower = 0;   // R32F
    protected int texZUpper = 0;   // R32F
    protected int colorTex = 0;    // RGBA8
    protected int rboStencil = 0;  // STENCIL8 (utile pour la variante stencil)

    protected int progMeshDiscardAbove = 0;
    protected int progCompose = 0;
    protected int vaoQuad = 0;

    protected DPbase(int width, int height, float zMinWorld, float zMaxWorld) {
        this.width = width; this.height = height;
        this.zMinWorld = zMinWorld; this.zMaxWorld = zMaxWorld;
    }

    public void setMesh(int vao, int indexCount, int mode) {
        this.meshVao = vao;
        this.meshIndexCount = indexCount;
        this.meshMode = mode;
    }

    /** Initialisation GL commune aux deux variantes (stencil/blend). */
    public void initGL() {
        // FBO + colorTex
        fbo = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, fbo);

        colorTex = createColorTexture(width, height);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, colorTex, 0);

        // ZLower/ZUpper (R32F)
        texZLower = createFloatR32Texture(width, height);
        texZUpper = createFloatR32Texture(width, height);

        // Stencil RBO (présent même si la variante blend ne l’utilise pas en D)
        rboStencil = glGenRenderbuffers();
        glBindRenderbuffer(GL_RENDERBUFFER, rboStencil);
        glRenderbufferStorage(GL_RENDERBUFFER, GL_STENCIL_INDEX8, width, height);
        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_STENCIL_ATTACHMENT, GL_RENDERBUFFER, rboStencil);

        checkFboComplete();
        glBindFramebuffer(GL_FRAMEBUFFER, 0);

        // Shaders communs
        progMeshDiscardAbove = compileProgram(VS_MESH, FS_MESH_DISCARD);
        progCompose          = compileProgram(VS_QUAD, FS_COMPOSE_COMMON);

        // Fullscreen triangle
        vaoQuad = createFullScreenQuad();
    }

    /** Pipeline commun : A (spécifique), B, C, D (spécifique). */
    public final void renderSlice(float z_s) {
        passA_Count(z_s);  // défini dans sous-classes
        passB_ZLower(z_s); // commun
        passC_ZUpper(z_s); // commun
        passD_Compose(z_s);// défini dans sous-classes
    }

    // -------- Passes communes B/C + utilitaires pour D --------

    protected void passB_ZLower(float z_s) {
        glBindFramebuffer(GL_FRAMEBUFFER, fbo);
        attachAsColor(texZLower);
        clearTo(zMinWorld - 1.0f);

        glEnable(GL_BLEND);
        glBlendEquation(GL_MAX);
        glBlendFunc(GL_ONE, GL_ONE);

        useMeshProgram(z_s, epsZ, 1); // uMode=1 => z < z_s - eps
        drawMesh();

        glDisable(GL_BLEND);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    protected void passC_ZUpper(float z_s) {
        glBindFramebuffer(GL_FRAMEBUFFER, fbo);
        attachAsColor(texZUpper);
        clearTo(zMaxWorld + 1.0f);

        glEnable(GL_BLEND);
        glBlendEquation(GL_MIN);
        glBlendFunc(GL_ONE, GL_ONE);

        useMeshProgram(z_s, epsZ, 2); // uMode=2 => z > z_s + eps
        drawMesh();

        glDisable(GL_BLEND);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    /** Renseigne les uniforms communs du composeur (ZLower/ZUpper, z_s, maxDist). */
    protected void composeCommonUniforms(float z_s) {
        glUseProgram(progCompose);
        glActiveTexture(GL_TEXTURE0); glBindTexture(GL_TEXTURE_2D, texZLower);
        glActiveTexture(GL_TEXTURE1); glBindTexture(GL_TEXTURE_2D, texZUpper);
        glUniform1i(glGetUniformLocation(progCompose, "uTexZLower"), 0);
        glUniform1i(glGetUniformLocation(progCompose, "uTexZUpper"), 1);
        glUniform1f(glGetUniformLocation(progCompose, "uZSlice"), z_s);
        glUniform1f(glGetUniformLocation(progCompose, "uMaxDist"), maxDistColor);
    }

    // --------- Méthodes abstraites à implémenter dans les sous-classes ---------
    protected abstract void passA_Count(float z_s);
    protected abstract void passD_Compose(float z_s);

    // ---------------- Helpers GL (manquants dans tes erreurs) ----------------

    protected static int createColorTexture(int w, int h) {
        int tex = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, tex);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, w, h, 0, GL_RGBA, GL_UNSIGNED_BYTE, 0L);
        glBindTexture(GL_TEXTURE_2D, 0);
        return tex;
    }

    protected static int createFloatR32Texture(int w, int h) {
        int tex = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, tex);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_R32F, w, h, 0, GL_RED, GL_FLOAT, 0L);
        glBindTexture(GL_TEXTURE_2D, 0);
        return tex;
    }

    protected static void checkFboComplete() {
        int status = glCheckFramebufferStatus(GL_FRAMEBUFFER);
        if (status != GL_FRAMEBUFFER_COMPLETE) {
            throw new IllegalStateException("FBO incomplete: status=0x" + Integer.toHexString(status));
        }
    }

    protected static int compileProgram(String vs, String fs) {
        int v = glCreateShader(GL_VERTEX_SHADER);
        glShaderSource(v, vs);
        glCompileShader(v);
        if (glGetShaderi(v, GL_COMPILE_STATUS) == GL_FALSE) {
            throw new IllegalStateException("VS error: " + glGetShaderInfoLog(v));
        }
        int f = glCreateShader(GL_FRAGMENT_SHADER);
        glShaderSource(f, fs);
        glCompileShader(f);
        if (glGetShaderi(f, GL_COMPILE_STATUS) == GL_FALSE) {
            throw new IllegalStateException("FS error: " + glGetShaderInfoLog(f));
        }
        int p = glCreateProgram();
        glAttachShader(p, v);
        glAttachShader(p, f);
        glLinkProgram(p);
        if (glGetProgrami(p, GL_LINK_STATUS) == GL_FALSE) {
            throw new IllegalStateException("Link error: " + glGetProgramInfoLog(p));
        }
        glDeleteShader(v);
        glDeleteShader(f);
        return p;
    }

    protected static int createFullScreenQuad() {
        // Fullscreen triangle (pas de VBO : positions générées dans le VS)
        int vao = glGenVertexArrays();
        glBindVertexArray(vao);
        glBindVertexArray(0);
        return vao;
    }

    // --- petits helpers utilisés dans le flux ---
    protected void attachAsColor(int tex) {
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, tex, 0);
        IntBuffer buf = BufferUtils.createIntBuffer(1);
        buf.put(GL_COLOR_ATTACHMENT0).flip();
        glDrawBuffers(buf);
        glColorMask(true, true, true, true);
    }

    protected void clearTo(float r) {
        glClearColor(r, 0f, 0f, 0f);


        glClear(GL_COLOR_BUFFER_BIT);

        //if(Main.renderType == Render.STENCIL)glClear(GL_COLOR_BUFFER_BIT);
        //if(Main.renderType == Render.BLENDING)glClear(GL_STENCIL_BUFFER_BIT);

        
    }

    protected void useMeshProgram(float z, float e, int mode) {
        glUseProgram(progMeshDiscardAbove);
        glUniform1f(glGetUniformLocation(progMeshDiscardAbove, "uZSlice"), z);
        glUniform1f(glGetUniformLocation(progMeshDiscardAbove, "uEps"),   e);
        glUniform1i(glGetUniformLocation(progMeshDiscardAbove, "uMode"),  mode);
    }

    protected void drawMesh() {
        if (meshIndexCount <= 0) throw new IllegalStateException("meshIndexCount == 0");
        glBindVertexArray(meshVao);
        glDrawElements(meshMode, meshIndexCount, GL_UNSIGNED_INT, 0L);
        glBindVertexArray(0);
    }

    public int getFbo() { return fbo; }
    public int getColorTexture() { return colorTex; }

    // ----------------- Shaders communs -----------------

    protected static final String VS_MESH =
        "#version 330 core\n" +
        "layout(location=0) in vec3 aPos;\n" +
        "out vec3 vPos;\n" +
        "void main(){ vPos=aPos; gl_Position = vec4(aPos.x, aPos.y, 0.0, 1.0); }\n";

    protected static final String FS_MESH_DISCARD =
        "#version 330 core\n" +
        "in vec3 vPos;\n" +
        "uniform float uZSlice;\n" +
        "uniform float uEps;\n" +
        "uniform int   uMode;\n" +
        "layout(location=0) out vec4 oColor;\n" +
        "void main(){\n" +
        "  float z = vPos.z;\n" +
        "  if (uMode==0) { if (z > uZSlice + uEps) discard; oColor = vec4(0); }        // stencil path\n" +
        "  else if (uMode==1){ if (z < uZSlice - uEps) oColor = vec4(z,0,0,1); else discard; } // ZLower\n" +
        "  else if (uMode==2){ if (z > uZSlice + uEps) oColor = vec4(z,0,0,1); else discard; } // ZUpper\n" +
        "  else if (uMode==3){ if (z <= uZSlice + uEps) oColor = vec4(1.0,0,0,1); else discard; } // BLEND count\n" +
        "}\n";

    protected static final String VS_QUAD =
        "#version 330 core\n" +
        "const vec2 v[3] = vec2[3]( vec2(-1,-1), vec2(3,-1), vec2(-1,3) );\n" +
        "void main(){ gl_Position = vec4(v[gl_VertexID], 0.0, 1.0); }\n";

    protected static final String FS_COMPOSE_COMMON =
        "#version 330 core\n" +
        "uniform sampler2D uTexZLower;\n" +
        "uniform sampler2D uTexZUpper;\n" +
        "uniform float uZSlice;\n" +
        "uniform float uMaxDist;\n" +
        "out vec4 oColor;\n" +
        "void main(){\n" +
        "  vec2 uv = gl_FragCoord.xy / vec2(textureSize(uTexZLower,0));\n" +
        "  float zL = texture(uTexZLower, uv).r;\n" +
        "  float zU = texture(uTexZUpper, uv).r;\n" +
        "  float d = min(uZSlice - zL, zU - uZSlice);\n" +
        "  if (!(d>0.0)) { oColor = vec4(0,0,0,1); return; }\n" +
        "  float dc = clamp(d, 0.0, uMaxDist);\n" +
        "  float R = 64.0 + (255.0-64.0) * (1.0 - dc/uMaxDist);\n" +
        "  oColor = vec4(R/255.0, 0.0, 0.0, 1.0);\n" +
        "}\n";
}
