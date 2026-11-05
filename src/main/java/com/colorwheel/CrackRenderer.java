package com.colorwheel;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;

/**
 * La grieta gigante en lo más alto del cielo.
 * Se dibuja pegada a la cámara (como el cielo), así que se ve exactamente igual desde cualquier
 * lugar del mapa: basta con mirar hacia arriba. Aparece cerrada, se abre y dentro se ven estrellas.
 */
public final class CrackRenderer {
    private static final int FRAMES = 8;
    private static final Identifier[] TEX = new Identifier[FRAMES];

    static {
        for (int i = 0; i < FRAMES; i++) {
            TEX[i] = Identifier.of(ColorWheelMod.MOD_ID, "textures/gui/crack_" + i + ".png");
        }
    }

    // Un cuadro enorme y horizontal justo encima de la cabeza del jugador (apunta de norte a sur).
    // A esta altura cubre casi 90° de cielo: desde ~45° de altura por un lado hasta ~45° por el otro, pasando por arriba.
    private static final float HEIGHT = 220f;   // a qué altura (bloques) sobre la cámara
    private static final float HALF_W = 150f;   // medio ancho (este-oeste)
    private static final float HALF_L = 225f;   // medio largo (norte-sur)

    private CrackRenderer() {}

    public static void render(WorldRenderContext ctx) {
        float k = SkyTint.factor();
        if (k <= 0f) return;
        MatrixStack ms = ctx.matrixStack();
        if (ms == null) return;

        float pulse = 0.94f + 0.06f * (float) Math.sin(SkyTint.elapsedMs() * 0.005);
        float alpha = Math.min(1f, k * 1.3f) * SkyTint.crackAppear() * pulse;

        // Cerrada -> abierta: se mezclan dos cuadros seguidos de la animación
        float f = SkyTint.openProgress() * (FRAMES - 1);
        int i = Math.min(FRAMES - 1, (int) f);
        float fr = f - i;

        Matrix4f m = ms.peek().getPositionMatrix();
        RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        quad(m, TEX[i], alpha);
        if (fr > 0.01f && i < FRAMES - 1) {
            quad(m, TEX[i + 1], alpha * fr);
        }
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
    }

    private static void quad(Matrix4f m, Identifier tex, float a) {
        RenderSystem.setShaderTexture(0, tex);
        BufferBuilder b = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        // Posiciones relativas a la cámara: la grieta no se mueve aunque el jugador camine
        b.vertex(m, -HALF_W, HEIGHT, HALF_L).texture(0f, 1f).color(1f, 1f, 1f, a);
        b.vertex(m, HALF_W, HEIGHT, HALF_L).texture(1f, 1f).color(1f, 1f, 1f, a);
        b.vertex(m, HALF_W, HEIGHT, -HALF_L).texture(1f, 0f).color(1f, 1f, 1f, a);
        b.vertex(m, -HALF_W, HEIGHT, -HALF_L).texture(0f, 0f).color(1f, 1f, 1f, a);
        BufferRenderer.drawWithGlobalProgram(b.end());
    }
}
