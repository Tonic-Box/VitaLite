package com.tonic.services.ground;

import com.tonic.Logger;
import net.runelite.api.Client;
import net.runelite.api.FloatProjection;
import net.runelite.api.Projection;
import net.runelite.api.WorldView;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.lwjgl.opengl.GL33C.*;

/**
 * Draws ground triangles into the GPU plugin's scene framebuffer once the scene is drawn: through its own projection, tested against its depth without writing it, and blended.
 */
final class GroundGl
{
    private static final int BYTES_PER_VERTEX = 16;
    private static final int CORNERS = 3;
    private static final float[] IDENTITY = {1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1};

    private static final String VERTEX = "#version 330\n"
            + "layout (location = 0) in vec3 position;\n"
            + "layout (location = 1) in uint colour;\n"
            + "uniform mat4 worldProj;\n"
            + "uniform mat4 entityProj;\n"
            + "out vec4 fragColour;\n"
            + "void main()\n"
            + "{\n"
            + "  uint argb = colour;\n"
            + "  fragColour = vec4(float((argb >> 16u) & 255u), float((argb >> 8u) & 255u), float(argb & 255u), float(argb >> 24u)) / 255.0;\n"
            + "  gl_Position = worldProj * (entityProj * vec4(position, 1.0));\n"
            + "}\n";

    private static final String FRAGMENT = "#version 330\n"
            + "in vec4 fragColour;\n"
            + "out vec4 colour;\n"
            + "void main()\n"
            + "{\n"
            + "  colour = fragColour;\n"
            + "}\n";

    private GLCapabilities context;
    private boolean failed;
    private int program;
    private int vao;
    private long pass;
    private int uniformWorld;
    private int uniformEntity;
    private ByteBuffer vertices = BufferUtils.createByteBuffer(64 * 1024);
    private final FloatBuffer worldMatrix = BufferUtils.createFloatBuffer(16);
    private final FloatBuffer entityMatrix = BufferUtils.createFloatBuffer(16);
    private final IntBuffer saved = BufferUtils.createIntBuffer(16);
    private final Map<GroundTriangles, Buffer> buffers = new IdentityHashMap<>();

    void draw(Client client, List<GroundLayer> layers, int sceneFramebuffer, int sceneProgram, int sceneWorldProjection)
    {
        if (!ready())
        {
            return;
        }

        clearErrors();

        worldMatrix.clear();
        glGetUniformfv(sceneProgram, sceneWorldProjection, worldMatrix);

        int previousProgram = glGetInteger(GL_CURRENT_PROGRAM);
        int previousFramebuffer = glGetInteger(GL_DRAW_FRAMEBUFFER_BINDING);
        int previousVao = glGetInteger(GL_VERTEX_ARRAY_BINDING);
        boolean depthTest = glIsEnabled(GL_DEPTH_TEST);
        boolean blend = glIsEnabled(GL_BLEND);
        boolean cull = glIsEnabled(GL_CULL_FACE);
        boolean offset = glIsEnabled(GL_POLYGON_OFFSET_FILL);
        saved.clear();
        glGetIntegerv(GL_DEPTH_FUNC, saved);
        int depthFunction = saved.get(0);
        boolean depthMask = glGetBoolean(GL_DEPTH_WRITEMASK);

        try
        {
            glBindFramebuffer(GL_DRAW_FRAMEBUFFER, sceneFramebuffer);
            glUseProgram(program);
            glBindVertexArray(vao);
            glUniformMatrix4fv(uniformWorld, false, worldMatrix);
            glEnable(GL_DEPTH_TEST);
            glDepthFunc(GL_GEQUAL);
            glDepthMask(false);
            glEnable(GL_BLEND);
            glBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ONE);
            glDisable(GL_CULL_FACE);
            glEnable(GL_POLYGON_OFFSET_FILL);
            glPolygonOffset(1f, 4f);

            pass++;
            for (GroundLayer layer : layers)
            {
                for (GroundTriangles triangles : layer.views())
                {
                    if (triangles.count() > 0)
                    {
                        drawView(client, triangles);
                    }
                }
            }
            sweep();
        }
        finally
        {
            glPolygonOffset(0f, 0f);
            setEnabled(GL_POLYGON_OFFSET_FILL, offset);
            setEnabled(GL_CULL_FACE, cull);
            setEnabled(GL_BLEND, blend);
            setEnabled(GL_DEPTH_TEST, depthTest);
            glDepthFunc(depthFunction);
            glDepthMask(depthMask);
            glBindVertexArray(previousVao);
            glUseProgram(previousProgram);
            glBindFramebuffer(GL_DRAW_FRAMEBUFFER, previousFramebuffer);
        }

        int error = glGetError();
        if (error != GL_NO_ERROR)
        {
            throw new IllegalStateException("GL error " + error);
        }
    }

    private void drawView(Client client, GroundTriangles triangles)
    {
        WorldView worldView = client.getWorldView(triangles.worldViewId());
        if (worldView == null)
        {
            return;
        }

        float[] entity = IDENTITY;
        if (!worldView.isTopLevel())
        {
            Projection projection = worldView.getMainWorldProjection();
            if (projection instanceof FloatProjection)
            {
                entity = ((FloatProjection) projection).getProjection();
            }
        }
        entityMatrix.clear();
        entityMatrix.put(entity).flip();
        glUniformMatrix4fv(uniformEntity, false, entityMatrix);

        int count = triangles.count();
        Buffer buffer = buffers.get(triangles);
        if (buffer == null)
        {
            buffer = new Buffer(glGenBuffers());
            buffers.put(triangles, buffer);
        }
        buffer.drawnIn = pass;
        glBindBuffer(GL_ARRAY_BUFFER, buffer.vbo);
        if (buffer.revision != triangles.revision())
        {
            upload(triangles, count);
            buffer.revision = triangles.revision();
        }
        glVertexAttribPointer(0, 3, GL_FLOAT, false, BYTES_PER_VERTEX, 0);
        glVertexAttribIPointer(1, 1, GL_UNSIGNED_INT, BYTES_PER_VERTEX, 3L * Float.BYTES);
        glDrawArrays(GL_TRIANGLES, 0, count * CORNERS);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
    }

    private void upload(GroundTriangles triangles, int count)
    {
        int bytes = count * CORNERS * BYTES_PER_VERTEX;
        if (vertices.capacity() < bytes)
        {
            vertices = BufferUtils.createByteBuffer(Math.max(bytes, vertices.capacity() * 2));
        }
        vertices.clear();
        for (int triangle = 0; triangle < count; triangle++)
        {
            int colour = triangles.colour(triangle);
            for (int corner = 0; corner < CORNERS; corner++)
            {
                vertices.putFloat(triangles.x(triangle, corner));
                vertices.putFloat(triangles.height(triangle, corner));
                vertices.putFloat(triangles.y(triangle, corner));
                vertices.putInt(colour);
            }
        }
        vertices.flip();
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_DYNAMIC_DRAW);
    }

    void release()
    {
        if (buffers.isEmpty())
        {
            return;
        }
        if (ready())
        {
            for (Buffer buffer : buffers.values())
            {
                glDeleteBuffers(buffer.vbo);
            }
        }
        buffers.clear();
    }

    private void sweep()
    {
        for (Iterator<Buffer> it = buffers.values().iterator(); it.hasNext(); )
        {
            Buffer buffer = it.next();
            if (buffer.drawnIn != pass)
            {
                glDeleteBuffers(buffer.vbo);
                it.remove();
            }
        }
    }

    private boolean ready()
    {
        GLCapabilities current;
        try
        {
            current = GL.getCapabilities();
        }
        catch (IllegalStateException e)
        {
            return false;
        }
        if (current == context)
        {
            return !failed;
        }

        context = current;
        buffers.clear();
        program = link(compile(GL_VERTEX_SHADER, VERTEX), compile(GL_FRAGMENT_SHADER, FRAGMENT));
        failed = program == 0;
        if (failed)
        {
            return false;
        }
        uniformWorld = glGetUniformLocation(program, "worldProj");
        uniformEntity = glGetUniformLocation(program, "entityProj");

        int previousVao = glGetInteger(GL_VERTEX_ARRAY_BINDING);
        vao = glGenVertexArrays();
        glBindVertexArray(vao);
        glEnableVertexAttribArray(0);
        glEnableVertexAttribArray(1);
        glBindVertexArray(previousVao);
        return true;
    }

    private static void clearErrors()
    {
        int remaining = 16;
        while (remaining > 0 && glGetError() != GL_NO_ERROR)
        {
            remaining--;
        }
    }

    private static int compile(int type, String source)
    {
        int shader = glCreateShader(type);
        glShaderSource(shader, source);
        glCompileShader(shader);
        if (glGetShaderi(shader, GL_COMPILE_STATUS) == GL_FALSE)
        {
            Logger.error("Ground overlay shader failed: " + glGetShaderInfoLog(shader));
            glDeleteShader(shader);
            return 0;
        }
        return shader;
    }

    private static int link(int vertex, int fragment)
    {
        if (vertex == 0 || fragment == 0)
        {
            if (vertex != 0)
            {
                glDeleteShader(vertex);
            }
            if (fragment != 0)
            {
                glDeleteShader(fragment);
            }
            return 0;
        }
        int linked = glCreateProgram();
        glAttachShader(linked, vertex);
        glAttachShader(linked, fragment);
        glLinkProgram(linked);
        glDeleteShader(vertex);
        glDeleteShader(fragment);
        if (glGetProgrami(linked, GL_LINK_STATUS) == GL_FALSE)
        {
            Logger.error("Ground overlay shader program failed: " + glGetProgramInfoLog(linked));
            glDeleteProgram(linked);
            return 0;
        }
        return linked;
    }

    private static void setEnabled(int capability, boolean enabled)
    {
        if (enabled)
        {
            glEnable(capability);
        }
        else
        {
            glDisable(capability);
        }
    }

    private static final class Buffer
    {
        private final int vbo;
        private int revision = -1;
        private long drawnIn;

        Buffer(int vbo)
        {
            this.vbo = vbo;
        }
    }
}
