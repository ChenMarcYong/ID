
package fc.PrintingApplication.TP2;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL14.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL32.*;

import java.nio.IntBuffer;
import org.lwjgl.BufferUtils;

/**
 * DepthPeelingGPU
 *
 * Raster depth peeling in orthographic Z to directly produce solid bitmaps:
 *  - Pass A: stencil even-odd under z_s (inside/outside mask)
 *  - Pass B: blend MAX to grab ZLower (largest z < z_s)
 *  - Pass C: blend MIN to grab ZUpper (smallest z > z_s)
 *  - Pass D: compose final color (black if outside, red in [64..255] based on distance)
 *
 * Requirements:
 *  - OpenGL 3.3+
 *  - Mesh must be provided as a VAO + draw call (indexed triangles recommended)
 *
 * Notes:
 *  - We keep Z values in world units (mm). Use orthographic projection.
 *  - We do NOT use depth test in passes A/B/C (we want all fragments).
 *  - Composition uses stencil test to draw only inside pixels.
 */
public class DepthPeelingGPU {

    // --- Configuration ---
    private final int width, height;
    private final float sXY;    // mm per pixel in XY (not used directly by GL; for your own mapping)
    private final float zMinWorld, zMaxWorld; // world Z bounds (mm) from your AABB
    private final float epsZ = 1e-6f;        // epsilon for z comparisons (in mm)
    private final float maxDistColor = 0.80f;// mm -> 255 when >= 0.80

    // Mesh (must be set by user)
    private int meshVao = 0;
    private int meshIndexCount = 0; // 0 if non-indexed
    private int meshMode = GL_TRIANGLES;

    // GL objects
    private int fbo = 0;
    private int texZLower = 0; // R32F
    private int texZUpper = 0; // R32F
    private int colorTex = 0;  // RGBA8 (optional final target for readback)
    private int rboStencil = 0; // STENCIL index renderbuffer

    private int progMeshDiscardAbove = 0; // draws mesh fragments, discarding based on z_s (for A/B/C)
    private int progCompose = 0;          // full-screen composition shader (reads ZLower/Upper, writes red)
    private int vaoQuad = 0;              // fullscreen quad

    public DepthPeelingGPU(int width, int height, float sXY, float zMinWorld, float zMaxWorld) {
        this.width = width; this.height = height;
        this.sXY = sXY;
        this.zMinWorld = zMinWorld; this.zMaxWorld = zMaxWorld;
    }

    // --------------------- Public API ---------------------

    /** Provide the mesh to render. VAO must bind positions in world space with Z in mm. */
    public void setMesh(int vao, int indexCount, int mode) {
        this.meshVao = vao;
        this.meshIndexCount = indexCount;
        this.meshMode = mode;
    }

    /** Create FBO, textures, renderbuffers and compile shaders. Call once after GL context is ready. */
    public void initGL() {
        // FBO
        fbo = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, fbo);

        // Color target (optional; you can also render to default framebuffer in compose)
        colorTex = createColorTexture(width, height);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, colorTex, 0);

        // ZLower / ZUpper textures (R32F)
        texZLower = createFloatR32Texture(width, height);
        texZUpper = createFloatR32Texture(width, height);
        // Not attached as color now; we bind them as draw targets for B/C passes

        // Stencil renderbuffer
        rboStencil = glGenRenderbuffers();
        glBindRenderbuffer(GL_RENDERBUFFER, rboStencil);
        glRenderbufferStorage(GL_RENDERBUFFER, GL_STENCIL_INDEX8, width, height);
        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_STENCIL_ATTACHMENT, GL_RENDERBUFFER, rboStencil);

        checkFboComplete();
        glBindFramebuffer(GL_FRAMEBUFFER, 0);

        // Shaders
        progMeshDiscardAbove = compileProgram(VS_MESH, FS_MESH_DISCARD);
        progCompose = compileProgram(VS_QUAD, FS_COMPOSE);

        // Fullscreen quad
        vaoQuad = createFullScreenQuad();
    }

    /** Render one slice at world Z = z_s. Result left in colorTex (RGBA8). */
    public void renderSlice(float z_s) {
        if (meshVao == 0) throw new IllegalStateException("mesh VAO not set");

        // Bind FBO
        glBindFramebuffer(GL_FRAMEBUFFER, fbo);
        IntBuffer drawBuf = BufferUtils.createIntBuffer(1);

        // ---------------- Pass A: Stencil even-odd under z_s ----------------
        glDisable(GL_DEPTH_TEST);
        glDisable(GL_CULL_FACE);
        glDisable(GL_BLEND);
        glColorMask(false, false, false, false);

        glEnable(GL_STENCIL_TEST);
        glStencilMask(0xFF);
        glClearStencil(0);
        glClear(GL_STENCIL_BUFFER_BIT);

        // Comptage winding: faces avant ++, faces arrière --
        // (le fragment doit être sous la coupe z_s)
        glUseProgram(progMeshDiscardAbove);
        glUniform1f(glGetUniformLocation(progMeshDiscardAbove, "uZSlice"), z_s);
        glUniform1f(glGetUniformLocation(progMeshDiscardAbove, "uEps"), epsZ);
        glUniform1i(glGetUniformLocation(progMeshDiscardAbove, "uMode"), 0); // 0 => z <= z_s+eps

        // IMPORTANT : ops séparés pour FRONT/BACK
        glStencilFuncSeparate(GL_FRONT_AND_BACK, GL_ALWAYS, 0, 0xFF);
        glStencilOpSeparate(GL_FRONT, GL_KEEP, GL_KEEP, GL_INCR_WRAP);
        glStencilOpSeparate(GL_BACK,  GL_KEEP, GL_KEEP, GL_DECR_WRAP);

        // assure la même convention que tes données (souvent CCW)
        glFrontFace(GL_CCW);

        glBindVertexArray(meshVao);
        drawMesh();
        glBindVertexArray(0);

        // ---------------- Pass B: ZLower (max of z < z_s) ----------------
        // Attach texZLower as color target
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, texZLower, 0);
        drawBuf.clear(); drawBuf.put(GL_COLOR_ATTACHMENT0).flip();
        glDrawBuffers(drawBuf);

        glDisable(GL_STENCIL_TEST);
        glStencilMask(0x00);
        glColorMask(true, true, true, true);
        glClearColor(zMinWorld - 1.0f, 0f, 0f, 0f); // clear to very low z in R channel
        glClear(GL_COLOR_BUFFER_BIT);

        glEnable(GL_BLEND);
        glBlendEquation(GL_MAX);
        glBlendFunc(GL_ONE, GL_ONE);

        glUseProgram(progMeshDiscardAbove);
        glUniform1f(glGetUniformLocation(progMeshDiscardAbove, "uZSlice"), z_s);
        glUniform1f(glGetUniformLocation(progMeshDiscardAbove, "uEps"), epsZ);
        glUniform1i(glGetUniformLocation(progMeshDiscardAbove, "uMode"), 1); // 1: write z if z < z_s

        glBindVertexArray(meshVao);
        drawMesh();
        glBindVertexArray(0);

        glDisable(GL_BLEND);

        // ---------------- Pass C: ZUpper (min of z > z_s) ----------------
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, texZUpper, 0);
        drawBuf.clear(); drawBuf.put(GL_COLOR_ATTACHMENT0).flip();
        glDrawBuffers(drawBuf);

        glClearColor(zMaxWorld + 1.0f, 0f, 0f, 0f); // clear to very high z
        glClear(GL_COLOR_BUFFER_BIT);

        glEnable(GL_BLEND);
        glBlendEquation(GL_MIN);
        glBlendFunc(GL_ONE, GL_ONE);

        glUseProgram(progMeshDiscardAbove);
        glUniform1f(glGetUniformLocation(progMeshDiscardAbove, "uZSlice"), z_s);
        glUniform1f(glGetUniformLocation(progMeshDiscardAbove, "uEps"), epsZ);
        glUniform1i(glGetUniformLocation(progMeshDiscardAbove, "uMode"), 2); // 2: write z if z > z_s

        glBindVertexArray(meshVao);
        drawMesh();
        glBindVertexArray(0);

        glDisable(GL_BLEND);
        glStencilMask(0xFF);
        // ---------------- Pass D: Compose using stencil==1 ----------------
        // Attach colorTex as output
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, colorTex, 0);
        drawBuf = BufferUtils.createIntBuffer(1);
        drawBuf.put(GL_COLOR_ATTACHMENT0).flip();
        glDrawBuffers(drawBuf);

        glColorMask(true, true, true, true);
        glClearColor(0f, 0f, 0f, 1f);
        glClear(GL_COLOR_BUFFER_BIT);

        // dessiner uniquement où le winding != 0 (intérieur)
        glEnable(GL_STENCIL_TEST);
        glStencilFunc(GL_NOTEQUAL, 0, 0xFF);
        glStencilOp(GL_KEEP, GL_KEEP, GL_KEEP);

        // binder les textures B/C
        glActiveTexture(GL_TEXTURE0); glBindTexture(GL_TEXTURE_2D, texZLower);
        glActiveTexture(GL_TEXTURE1); glBindTexture(GL_TEXTURE_2D, texZUpper);

        // lancer le shader de composition
        glUseProgram(progCompose);
        glUniform1i(glGetUniformLocation(progCompose, "uTexZLower"), 0);
        glUniform1i(glGetUniformLocation(progCompose, "uTexZUpper"), 1);
        glUniform1f(glGetUniformLocation(progCompose, "uZSlice"), z_s);
        glUniform1f(glGetUniformLocation(progCompose, "uMaxDist"), maxDistColor);

        // fullscreen triangle
        glBindVertexArray(vaoQuad);
        glDrawArrays(GL_TRIANGLES, 0, 3);
        glBindVertexArray(0);

        // clean-up
        glDisable(GL_STENCIL_TEST);

        // laisser colorTex rempli, puis débinder le FBO
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    /** Return texture id containing the last composed slice (RGBA8). */
    public int getColorTexture() { return colorTex; }

    // --------------------- Helpers ---------------------

    private void drawMesh() {
        if (meshIndexCount > 0) {
            glDrawElements(meshMode, meshIndexCount, GL_UNSIGNED_INT, 0L);
        } else {
            // You can change this if your VAO is non-indexed and you know the vertex count
            throw new IllegalStateException("Provide indexCount or implement non-indexed draw");
        }
    }

    private static int createFullScreenQuad() {
        // Fullscreen triangle (less state): clip-space positions in VS
        int vao = glGenVertexArrays();
        glBindVertexArray(vao);
        glBindVertexArray(0);
        return vao;
    }

    private static int createColorTexture(int w, int h) {
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

    private static int createFloatR32Texture(int w, int h) {
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

    private static void checkFboComplete() {
        int status = glCheckFramebufferStatus(GL_FRAMEBUFFER);
        if (status != GL_FRAMEBUFFER_COMPLETE) {
            throw new IllegalStateException("FBO incomplete: status=0x" + Integer.toHexString(status));
        }
    }

    private static int compileProgram(String vs, String fs) {
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

    	public int getFbo() { return fbo; }

    // --------------------- Shaders ---------------------

    // Mesh VS: output world position (pos) to FS via flat varying (Z is mm)
    private static final String VS_MESH = "#version 330 core\n" +
        "layout(location=0) in vec3 aPos;\n" +
        "out vec3 vPos;\n" +
        "void main(){ vPos=aPos; gl_Position = vec4( (aPos.x), (aPos.y), 0.0, 1.0 ); }\n";
    // NOTE: We assume you've already applied an orthographic XY-to-NDC transform when feeding positions
    // If not, replace with a proper MVP and pass worldZ separately.

    // Mesh FS: discard policy controlled by uMode
    // uMode = 0: stencil pass -> accept only fragments with vPos.z <= uZSlice+eps (they toggle stencil)
    // uMode = 1: ZLower pass -> write vPos.z if vPos.z <  uZSlice-eps, else discard
    // uMode = 2: ZUpper pass -> write vPos.z if vPos.z >  uZSlice+eps, else discard
    private static final String FS_MESH_DISCARD = "#version 330 core\n" +
        "in vec3 vPos;\n" +
        "uniform float uZSlice;\n" +
        "uniform float uEps;\n" +
        "uniform int uMode;\n" +
        "layout(location=0) out vec4 oColor;\n" +
        "void main(){\n" +
        "  float z = vPos.z;\n" +
        "  if (uMode==0){ if (z > uZSlice + uEps) discard; oColor = vec4(0); }\n" +
        "  else if (uMode==1){ if (z < uZSlice - uEps) oColor = vec4(z,0,0,1); else discard; }\n" +
        "  else { if (z > uZSlice + uEps) oColor = vec4(z,0,0,1); else discard; }\n" +
        "}\n";

    // Fullscreen quad VS: single triangle covering the screen
    private static final String VS_QUAD = "#version 330 core\n" +
        "const vec2 verts[3] = vec2[3]( vec2(-1,-1), vec2(3,-1), vec2(-1,3) );\n" +
        "void main(){ gl_Position = vec4(verts[gl_VertexID], 0.0, 1.0); }\n";

    // Compose FS: sample ZLower (tex0) & ZUpper (tex1), compute distance to nearest boundary, output red
    private static final String FS_COMPOSE = "#version 330 core\n" +
        "uniform sampler2D uTexZLower;\n" +
        "uniform sampler2D uTexZUpper;\n" +
        "uniform float uZSlice;\n" +
        "uniform float uMaxDist;\n" +
        "out vec4 oColor;\n" +
        "void main(){\n" +
        "  vec2 uv = gl_FragCoord.xy / textureSize(uTexZLower,0);\n" +
        "  float zL = texture(uTexZLower, uv).r;\n" +
        "  float zU = texture(uTexZUpper, uv).r;\n" +
        "  float d = min(uZSlice - zL, zU - uZSlice);\n" +
        "  if (!(d>0.0)) { oColor = vec4(0,0,0,1); return; }\n" +
        "  float dc = clamp(d, 0.0, uMaxDist);\n" +
        "  float R = 64.0 + (255.0-64.0) * (1.0 - dc/uMaxDist);\n" +
        "  oColor = vec4(R/255.0, 0.0, 0.0, 1.0);\n" +
        "}\n";
}
