package fc.PrintingApplication.TP2;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL14.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

class DPstencil extends DPbase {

    public DPstencil(int width, int height, float zMinWorld, float zMaxWorld) {
        super(width, height, zMinWorld, zMaxWorld);
    }

    @Override
    protected void passA_Count(float z_s) {
        glBindFramebuffer(GL_FRAMEBUFFER, fbo);

        // On attache n'importe quelle cible couleur (on n'écrit pas dedans)
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, colorTex, 0);

        // Pas de depth/cull/blend pour compter TOUTES les faces sous la coupe
        glDisable(GL_DEPTH_TEST);
        glDisable(GL_CULL_FACE);
        glDisable(GL_BLEND);

        // On n'écrit pas dans la couleur pendant le comptage
        glColorMask(false, false, false, false);

        // Stencil : winding count (front ++, back --)
        glEnable(GL_STENCIL_TEST);
        glStencilMask(0xFF);
        glClearStencil(0);
        glClear(GL_STENCIL_BUFFER_BIT);

        glUseProgram(progMeshDiscardAbove);
        glUniform1f(glGetUniformLocation(progMeshDiscardAbove, "uZSlice"), z_s);
        glUniform1f(glGetUniformLocation(progMeshDiscardAbove, "uEps"),   epsZ);
        glUniform1i(glGetUniformLocation(progMeshDiscardAbove, "uMode"),  0); // fragments sous la coupe

        glStencilFuncSeparate(GL_FRONT_AND_BACK, GL_ALWAYS, 0, 0xFF);
        glStencilOpSeparate(GL_FRONT, GL_KEEP, GL_KEEP, GL_INCR_WRAP);
        glStencilOpSeparate(GL_BACK,  GL_KEEP, GL_KEEP, GL_DECR_WRAP);
        glFrontFace(GL_CCW);

        drawMesh();

        // Nettoyage
        glDisable(GL_STENCIL_TEST);
        glColorMask(true, true, true, true);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    @Override
    protected void passD_Compose(float z_s) {
        glBindFramebuffer(GL_FRAMEBUFFER, fbo);
        attachAsColor(colorTex);

        glClearColor(0f, 0f, 0f, 1f);
        glClear(GL_COLOR_BUFFER_BIT);

        // Masque intérieur via stencil != 0
        glEnable(GL_STENCIL_TEST);
        glStencilFunc(GL_NOTEQUAL, 0, 0xFF);
        glStencilOp(GL_KEEP, GL_KEEP, GL_KEEP);

        composeCommonUniforms(z_s);

        glBindVertexArray(vaoQuad);
        glDrawArrays(GL_TRIANGLES, 0, 3);
        glBindVertexArray(0);

        glDisable(GL_STENCIL_TEST);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

}
