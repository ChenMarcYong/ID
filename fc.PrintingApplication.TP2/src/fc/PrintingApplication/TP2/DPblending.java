package fc.PrintingApplication.TP2;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL14.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

class DPblend extends DPbase {

    private int texCount = 0;     
    private int progComposeParity = 0; 

    public DPblend(int width, int height, float zMinWorld, float zMaxWorld) {
        super(width, height, zMinWorld, zMaxWorld);
    }

    @Override
    public void initGL() {
        super.initGL();
        texCount = createFloatR32Texture(width, height);

        final String FS_COMPOSE_PARITY =
            "#version 330 core\n" +
            "uniform sampler2D uTexZLower, uTexZUpper, uTexCount;\n" +
            "uniform float uZSlice, uMaxDist;\n" +
            "out vec4 oColor;\n" +
            "void main(){\n" +
            "  vec2 uv = gl_FragCoord.xy / vec2(textureSize(uTexZLower,0));\n" +
            "  float c  = texture(uTexCount, uv).r;              // nombre d'intersections\n" +
            "  int parity = int(floor(c + 0.5)) & 1;             // parité robuste (arrondis)\n" +
            "  if (parity == 0) { oColor = vec4(0,0,0,1); return; }\n" +
            "  float zL = texture(uTexZLower, uv).r;\n" +
            "  float zU = texture(uTexZUpper, uv).r;\n" +
            "  float d  = min(uZSlice - zL, zU - uZSlice);\n" +
            "  if (!(d>0.0)) { oColor = vec4(0,0,0,1); return; }\n" +
            "  float dc = clamp(d, 0.0, uMaxDist);\n" +
            "  float R  = 64.0 + (255.0-64.0) * (1.0 - dc/uMaxDist);\n" +
            "  oColor   = vec4(R/255.0, 0.0, 0.0, 1.0);\n" +
            "}\n";

        progComposeParity = compileProgram(VS_QUAD, FS_COMPOSE_PARITY);
    }

    @Override
    protected void passA_Count(float z_s) {
        glBindFramebuffer(GL_FRAMEBUFFER, fbo);
        attachAsColor(texCount);

        glDisable(GL_DEPTH_TEST);
        glDisable(GL_CULL_FACE);
        glDisable(GL_STENCIL_TEST);

        glClearColor(0f, 0f, 0f, 0f);
        glClear(GL_COLOR_BUFFER_BIT);

        glEnable(GL_BLEND);
        glBlendEquation(GL_FUNC_ADD);
        glBlendFunc(GL_ONE, GL_ONE);

        glUseProgram(progMeshDiscardAbove);
        glUniform1f(glGetUniformLocation(progMeshDiscardAbove, "uZSlice"), z_s);
        glUniform1f(glGetUniformLocation(progMeshDiscardAbove, "uEps"),   epsZ);
        glUniform1i(glGetUniformLocation(progMeshDiscardAbove, "uMode"),  3); 

        drawMesh();

        glDisable(GL_BLEND);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    @Override
    protected void passD_Compose(float z_s) {
        glBindFramebuffer(GL_FRAMEBUFFER, fbo);
        attachAsColor(colorTex);

        glClearColor(0f, 0f, 0f, 1f);
        glClear(GL_COLOR_BUFFER_BIT);

        glUseProgram(progComposeParity);

        glActiveTexture(GL_TEXTURE0); glBindTexture(GL_TEXTURE_2D, texZLower);
        glActiveTexture(GL_TEXTURE1); glBindTexture(GL_TEXTURE_2D, texZUpper);
        glUniform1i(glGetUniformLocation(progComposeParity, "uTexZLower"), 0);
        glUniform1i(glGetUniformLocation(progComposeParity, "uTexZUpper"), 1);
        glUniform1f(glGetUniformLocation(progComposeParity, "uZSlice"), z_s);
        glUniform1f(glGetUniformLocation(progComposeParity, "uMaxDist"), maxDistColor);

        glActiveTexture(GL_TEXTURE2); glBindTexture(GL_TEXTURE_2D, texCount);
        glUniform1i(glGetUniformLocation(progComposeParity, "uTexCount"), 2);

        glBindVertexArray(vaoQuad);
        glDrawArrays(GL_TRIANGLES, 0, 3);
        glBindVertexArray(0);

        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }
}
