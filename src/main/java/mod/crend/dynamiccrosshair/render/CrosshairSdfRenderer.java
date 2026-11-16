package mod.crend.dynamiccrosshair.render;

import mod.crend.dynamiccrosshair.bobbing.Bobbing;
import mod.crend.dynamiccrosshair.compat.CrosshairSelectionCompat.Tint;
import mod.crend.dynamiccrosshair.config.Config;
import mod.crend.dynamiccrosshair.config.Shape;
import mod.crend.dynamiccrosshair.config.StyleSetting;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;

/**
 * Crosshair-Selection-style vector renderer: every dynamic style is drawn as a procedural signed
 * distance field in the fragment shader, so the shape is genuinely resolution independent, smooth
 * and free of pixel jaggies. Colour, flowing rainbow, outline and the centre-gap animation are
 * driven by Crosshair-Selection's configuration.
 */
public final class CrosshairSdfRenderer {

    private static final String VERTEX = "#version 120\n"
            + "void main() { gl_Position = gl_Vertex; }\n";

    private static final String FRAGMENT = "#version 120\n"
            + "uniform vec2 uCentre;\n"
            + "uniform float uSize;\n"
            + "uniform float uScaleX;\n"
            + "uniform float uScaleY;\n"
            + "uniform float uRotation;\n"
            + "uniform vec4 uColour;\n"
            + "uniform int uShape;\n"
            + "uniform float uHalf;\n"
            + "uniform float uAa;\n"
            + "uniform float uGap;\n"
            + "uniform float uFlow;\n"
            + "uniform vec4 uFlowColour;\n"
            + "uniform float uFlowPhase;\n"
            + "uniform float uFlowDensity;\n"
            + "uniform float uFlowOpacity;\n"
            + "uniform float uOutline;\n"
            + "uniform vec4 uOutlineColour;\n"
            + "vec3 hsv2rgb(vec3 c) {\n"
            + "  vec3 p = abs(fract(c.xxx + vec3(1.0, 2.0/3.0, 1.0/3.0)) * 6.0 - 3.0);\n"
            + "  return c.z * mix(vec3(1.0), clamp(p - 1.0, 0.0, 1.0), clamp(c.y, 0.0, 1.0));\n"
            + "}\n"
            + "float sdBox(vec2 p, vec2 b) { vec2 d = abs(p) - b; return length(max(d, 0.0)) + min(max(d.x, d.y), 0.0); }\n"
            + "float sdSeg(vec2 p, vec2 a, vec2 b) { vec2 pa = p - a, ba = b - a; float h = clamp(dot(pa, ba) / dot(ba, ba), 0.0, 1.0); return length(pa - ba * h); }\n"
            + "float sdRoundBox(vec2 p, vec2 b, float r);\n"
            + "float crossD(vec2 p, float gap, float arm, float half) {\n"
            + "  float top = sdRoundBox(p - vec2(0.0, -(gap + arm * 0.5)), vec2(half, arm * 0.5), half);\n"
            + "  float bottom = sdRoundBox(p - vec2(0.0, gap + arm * 0.5), vec2(half, arm * 0.5), half);\n"
            + "  float left = sdRoundBox(p - vec2(-(gap + arm * 0.5), 0.0), vec2(arm * 0.5, half), half);\n"
            + "  float right = sdRoundBox(p - vec2(gap + arm * 0.5, 0.0), vec2(arm * 0.5, half), half);\n"
            + "  return min(min(top, bottom), min(left, right));\n"
            + "}\n"
            + "float sdRoundBox(vec2 p, vec2 b, float r) { vec2 q = abs(p) - b + r; return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - r; }\n"
            + "float bracketOne(vec2 p, float sx, float sy, float s, float r, float len, float t) {\n"
            + "  vec2 c = vec2(sx * (s - r), sy * (s - r));\n"
            + "  float arc = abs(length(p - c) - r) - t;\n"
            + "  vec2 q = vec2((p.x - c.x) * sx, (p.y - c.y) * sy);\n"
            + "  arc = max(arc, -min(q.x, q.y));\n"
            + "  float h = sdSeg(p, vec2(sx * (s - r), sy * s), vec2(sx * (s - len), sy * s)) - t;\n"
            + "  float v = sdSeg(p, vec2(sx * s, sy * (s - r)), vec2(sx * s, sy * (s - len))) - t;\n"
            + "  return min(arc, min(h, v));\n"
            + "}\n"
            + "float bracketRound(vec2 p, float s, float len, float r, float t) {\n"
            + "  return min(min(bracketOne(p, 1.0, 1.0, s, r, len, t), bracketOne(p, -1.0, 1.0, s, r, len, t)),\n"
            + "             min(bracketOne(p, 1.0, -1.0, s, r, len, t), bracketOne(p, -1.0, -1.0, s, r, len, t)));\n"
            + "}\n"
            + "float bracketCorner(vec2 q, float s, float len, float t) {\n"
            + "  float h = sdBox(q - vec2(s - len * 0.5, s), vec2(len * 0.5, t));\n"
            + "  float v = sdBox(q - vec2(s, s - len * 0.5), vec2(t, len * 0.5));\n"
            + "  return min(h, v);\n"
            + "}\n"
            + "float shapeDist(vec2 p) {\n"
            + "  float half = uHalf;\n"
            + "  if (uShape == 0) return crossD(p, uGap, 4.5, half);\n"
            + "  if (uShape == 1) return crossD(p, 0.9 + uGap, 4.5, half);\n"
            + "  if (uShape == 2) return crossD(p, 0.9 + uGap, 4.5, half);\n"
            + "  if (uShape == 3) return crossD(p, 1.4 + uGap, 2.8, half);\n"
            + "  if (uShape == 4) return length(p) - 0.75;\n"
            + "  if (uShape == 5) return abs(length(p) - 4.6) - uHalf;\n"
            + "  if (uShape == 6) return abs(length(p) - 6.0) - uHalf;\n"
            + "  if (uShape == 7) return abs(sdRoundBox(p, vec2(4.6), 2.0)) - uHalf;\n"
            + "  if (uShape == 8) return abs(sdRoundBox(p, vec2(6.0), 2.6)) - uHalf;\n"
            + "  if (uShape == 9) { vec2 q = vec2(p.x + p.y, p.y - p.x) * 0.70710678; return abs(sdRoundBox(q, vec2(5.8), 1.6)) - uHalf; }\n"
            + "  if (uShape == 10) { vec2 q = vec2(p.x + p.y, p.y - p.x) * 0.70710678; return abs(sdRoundBox(q, vec2(7.0), 2.0)) - uHalf; }\n"
            + "  if (uShape == 11) return bracketRound(p, 5.0, 3.0, 2.4, uHalf);\n"
            + "  if (uShape == 12) return bracketRound(p, 5.0, 3.0, 2.4, uHalf);\n"
            + "  if (uShape == 13) return max(bracketRound(p, 5.0, 3.2, 2.0, uHalf), p.y);\n"
            + "  if (uShape == 14) return max(bracketRound(p, 5.0, 3.2, 2.0, uHalf), -p.y);\n"
            + "  if (uShape == 15) return crossD(p, uGap, 7.0, half);\n"
            + "  if (uShape == 16) return sdBox(p - vec2(0.0, 3.0), vec2(half, 3.0));\n"
            + "  if (uShape == 17) return min(sdSeg(p, vec2(-4.0, 3.0), vec2(0.0, -3.0)), sdSeg(p, vec2(0.0, -3.0), vec2(4.0, 3.0)));\n"
            + "  return 1000.0;\n"
            + "}\n"
            + "void main() {\n"
            + "  vec2 p = gl_FragCoord.xy - uCentre;\n"
            + "  float c = cos(-uRotation);\n"
            + "  float s = sin(-uRotation);\n"
            + "  p = vec2(p.x * c - p.y * s, p.x * s + p.y * c);\n"
            + "  p.x /= uScaleX;\n"
            + "  p.y /= uScaleY;\n"
            + "  p = p / uSize * 15.0;\n"
            + "  float d = shapeDist(p);\n"
            + "  float aa = max(fwidth(d), uAa);\n"
            + "  float cov = 1.0 - smoothstep(-aa, aa, d);\n"
            + "  if (cov <= 0.001) discard;\n"
            + "  vec3 rgb = uColour.rgb;\n"
            + "  float alpha = uColour.a;\n"
            + "  if (uFlow > 0.5) {\n"
            + "    vec2 rel = gl_FragCoord.xy - uCentre;\n"
            + "    float band = dot(rel, vec2(0.01)) * uFlowDensity;\n"
            + "    float t = uFlowColour.x + fract(band + uFlowPhase) * uFlowColour.y;\n"
            + "    rgb = hsv2rgb(vec3(fract(t), uFlowColour.z, uFlowColour.w));\n"
            + "    alpha = uColour.a * uFlowOpacity;\n"
            + "  }\n"
            + "  if (uOutline > 0.0) {\n"
            + "    float outer = 1.0 - smoothstep(-aa, aa, d - uOutline);\n"
            + "    float ring = max(outer - cov, 0.0);\n"
            + "    rgb = mix(rgb, uOutlineColour.rgb, ring * uOutlineColour.a);\n"
            + "    alpha = max(alpha, uOutlineColour.a * ring);\n"
            + "  }\n"
            + "  gl_FragColor = vec4(rgb, alpha * cov);\n"
            + "}\n";

    private static boolean initialised;
    private static boolean available;
    private static int program;
    private static int quadVbo;
    private static int uCentre;
    private static int uSize;
    private static int uScaleX;
    private static int uScaleY;
    private static int uRotation;
    private static int uColour;
    private static int uShape;
    private static int uHalf;
    private static int uAa;
    private static int uGap;
    private static int uFlow;
    private static int uFlowColour;
    private static int uFlowPhase;
    private static int uFlowDensity;
    private static int uFlowOpacity;
    private static int uOutline;
    private static int uOutlineColour;

    private CrosshairSdfRenderer() {}

    public static boolean isAvailable() {
        ensure();
        return available;
    }

    private static void ensure() {
        if (initialised) return;
        initialised = true;
        try {
            int vertex = compile(GL20.GL_VERTEX_SHADER, VERTEX);
            int fragment = compile(GL20.GL_FRAGMENT_SHADER, FRAGMENT);
            program = GL20.glCreateProgram();
            GL20.glAttachShader(program, vertex);
            GL20.glAttachShader(program, fragment);
            GL20.glLinkProgram(program);
            if (GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) == 0) {
                GL20.glDeleteProgram(program);
                program = 0;
                return;
            }
            uCentre = location("uCentre");
            uSize = location("uSize");
            uScaleX = location("uScaleX");
            uScaleY = location("uScaleY");
            uRotation = location("uRotation");
            uColour = location("uColour");
            uShape = location("uShape");
            uHalf = location("uHalf");
            uAa = location("uAa");
            uGap = location("uGap");
            uFlow = location("uFlow");
            uFlowColour = location("uFlowColour");
            uFlowPhase = location("uFlowPhase");
            uFlowDensity = location("uFlowDensity");
            uFlowOpacity = location("uFlowOpacity");
            uOutline = location("uOutline");
            uOutlineColour = location("uOutlineColour");
            quadVbo = createQuad();
            available = true;
            mod.crend.dynamiccrosshair.DynamicCrosshair.LOGGER.info("Crosshair-Selection style vector renderer active");
        } catch (Throwable t) {
            available = false;
            program = 0;
            mod.crend.dynamiccrosshair.DynamicCrosshair.LOGGER.warn("Vector renderer unavailable, using pixel sprites: " + t);
        }
    }

    private static int compile(int type, String source) {
        int shader = GL20.glCreateShader(type);
        GL20.glShaderSource(shader, source);
        GL20.glCompileShader(shader);
        if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == 0) {
            String log = GL20.glGetShaderInfoLog(shader, 4096);
            GL20.glDeleteShader(shader);
            throw new IllegalStateException("crosshair shader compile failed: " + log);
        }
        return shader;
    }

    private static int createQuad() {
        try {
            java.nio.FloatBuffer buffer = org.lwjgl.BufferUtils.createFloatBuffer(8);
            buffer.put(new float[]{-1.0F, -1.0F, 1.0F, -1.0F, -1.0F, 1.0F, 1.0F, 1.0F});
            buffer.flip();
            int id = GL15.glGenBuffers();
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, id);
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buffer, GL15.GL_STATIC_DRAW);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
            return id;
        } catch (Throwable t) {
            return 0;
        }
    }

    private static int location(String name) {
        return GL20.glGetUniformLocation(program, name);
    }

    public static void draw(StyleSetting style, float centerX, float centerY,
                            Bobbing.Transform transform, Tint tint, int guiScale,
                            int guiWidth, int guiHeight, int frameWidth, int frameHeight) {
        if (!isAvailable() || style == null) return;
        int baseColour = tint != null ? tint.colour : style.color;
        int outlineColour = tint != null ? tint.outlineColour : 0;
        boolean flow = tint != null && tint.flow;

        float scaleX = frameWidth / (float) Math.max(1, guiWidth);
        float scaleY = frameHeight / (float) Math.max(1, guiHeight);
        float px = (centerX + transform.dx) * scaleX;
        float py = frameHeight - (centerY + transform.dy) * scaleY;
        float size = 15.0F * style.scale * scaleX;
        boolean diagonal = style.shape == Shape.CROSS_OPEN_DIAGONAL
                || style.shape == Shape.CROSS_DIAGONAL_SMALL;
        float rotation = (float) Math.toRadians(diagonal ? transform.rotation + 45.0F : transform.rotation);

        GL20.glUseProgram(program);
        GL20.glUniform2f(uCentre, px, py);
        GL20.glUniform1f(uSize, size);
        GL20.glUniform1f(uScaleX, Math.max(0.05F, transform.scaleX));
        GL20.glUniform1f(uScaleY, Math.max(0.05F, transform.scaleY));
        GL20.glUniform1f(uRotation, rotation);
        GL20.glUniform4f(uColour, red(baseColour), green(baseColour), blue(baseColour), alpha(baseColour));
        GL20.glUniform1i(uShape, style.shape.ordinal());
        float guiHalf = tint != null ? tint.thickness / 8.0F : 0.375F;
        float half = Math.max(0.2F, Math.min(2.0F, guiHalf * 1.25F / Math.max(style.scale, 0.1F)));
        GL20.glUniform1f(uHalf, half);
        GL20.glUniform1f(uAa, 0.5F / Math.max(1.0F, style.scale * scaleX));
        GL20.glUniform1f(uGap, transform.gap * 0.7F);
        GL20.glUniform1f(uFlow, flow ? 1.0F : 0.0F);
        if (flow) {
            GL20.glUniform4f(uFlowColour, tint.hueStart, tint.hueRange, tint.saturation, tint.brightness);
            GL20.glUniform1f(uFlowPhase, flowPhase(tint, System.currentTimeMillis()));
            GL20.glUniform1f(uFlowDensity, Math.max(tint.density, 0.01F));
            GL20.glUniform1f(uFlowOpacity, clamp01(tint.opacity));
        }
        boolean outline = tint != null && tint.outline && tint.outlineThickness > 0.01F;
        GL20.glUniform1f(uOutline, outline ? tint.outlineThickness / Math.max(style.scale, 0.1F) : 0.0F);
        GL20.glUniform4f(uOutlineColour, red(outlineColour), green(outlineColour), blue(outlineColour), alpha(outlineColour));

        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        if (quadVbo != 0) {
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, quadVbo);
            GL20.glEnableVertexAttribArray(0);
            GL20.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, 0, 0L);
            GL11.glDrawArrays(GL11.GL_TRIANGLE_STRIP, 0, 4);
            GL20.glDisableVertexAttribArray(0);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        } else {
            GL11.glBegin(GL11.GL_QUADS);
            GL11.glVertex2f(-1.0F, -1.0F);
            GL11.glVertex2f(1.0F, -1.0F);
            GL11.glVertex2f(1.0F, 1.0F);
            GL11.glVertex2f(-1.0F, 1.0F);
            GL11.glEnd();
        }
        GL20.glUseProgram(0);
    }

    private static float flowPhase(Tint tint, long now) {
        double value = now * 0.001D * (tint.speed / 1000.0D);
        if (tint.reverse) value = -value;
        value -= Math.floor(value);
        float x = (float) value;
        int mode = tint.motion;
        if (mode == 1) {
            float w = fract(x);
            return w - (float) Math.sin(6.28318530718F * w) / 6.28318530718F;
        }
        if (mode == 2) {
            return Math.abs(fract(x) * 2.0F - 1.0F);
        }
        return fract(x);
    }

    private static float fract(float value) {
        return value - (float) Math.floor(value);
    }

    private static float clamp01(float value) {
        return value < 0.0F ? 0.0F : (value > 1.0F ? 1.0F : value);
    }

    private static float red(int argb) { return ((argb >> 16) & 0xFF) / 255.0F; }
    private static float green(int argb) { return ((argb >> 8) & 0xFF) / 255.0F; }
    private static float blue(int argb) { return (argb & 0xFF) / 255.0F; }
    private static float alpha(int argb) { return ((argb >>> 24) & 0xFF) / 255.0F; }
}