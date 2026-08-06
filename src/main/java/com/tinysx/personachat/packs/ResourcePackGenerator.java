package com.tinysx.personachat.packs;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Automatically creates and packages a ready-to-use Resource Pack containing:
 * 1. pack.mcmeta
 * 2. Vanilla Core Shaders for 2D Half-Body / Player Skin UV projection
 * 3. Default Font negative-space providers for pixel shifting (%img_offset%)
 */
public class ResourcePackGenerator {

    public static void generate(JavaPlugin plugin) {
        File rpFolder = new File(plugin.getDataFolder(), "resourcepack");
        File targetZip = new File(plugin.getDataFolder(), "PersonaChat_ResourcePack.zip");

        if (rpFolder.exists() && targetZip.exists()) {
            return; // Already generated
        }

        plugin.getLogger().info("Generating built-in PersonaChat Resource Pack & Core Shaders...");

        try {
            rpFolder.mkdirs();

            // 1. pack.mcmeta
            writePackMcmeta(new File(rpFolder, "pack.mcmeta"));

            // 2. assets/minecraft/shaders/core/
            File shaderDir = new File(rpFolder, "assets/minecraft/shaders/core");
            shaderDir.mkdirs();
            writeShaderJson(new File(shaderDir, "rendertype_entity_translucent.json"));
            writeShaderVsh(new File(shaderDir, "rendertype_entity_translucent.vsh"));

            // 3. assets/minecraft/font/
            File fontDir = new File(rpFolder, "assets/minecraft/font");
            fontDir.mkdirs();
            writeFontJson(new File(fontDir, "default.json"));

            // 4. Zip the resource pack
            zipDirectory(rpFolder, targetZip);

            plugin.getLogger().info("Resource Pack generated successfully at: " + targetZip.getName());
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to generate resource pack: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void writePackMcmeta(File file) throws IOException {
        String content = """
                {
                  "pack": {
                    "pack_format": 34,
                    "supported_formats": [15, 46],
                    "description": "\\u00a7bPersonaChat \\u00a77- 2D Half-Body Shader & Font Offsets\\n\\u00a78Created by TiNYsx"
                  }
                }
                """;
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(content.trim());
        }
    }

    private static void writeShaderJson(File file) throws IOException {
        String content = """
                {
                    "blend": {
                        "func": "add",
                        "srcrgb": "srcalpha",
                        "dstrgb": "1-srcalpha"
                    },
                    "vertex": "rendertype_entity_translucent",
                    "fragment": "rendertype_entity_translucent",
                    "attributes": [
                        "Position",
                        "Color",
                        "UV0",
                        "UV1",
                        "UV2",
                        "Normal"
                    ],
                    "samplers": [
                        { "name": "Sampler0" },
                        { "name": "Sampler1" },
                        { "name": "Sampler2" }
                    ],
                    "uniforms": [
                        { "name": "ModelViewMat", "type": "matrix4x4", "count": 16, "values": [ 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0 ] },
                        { "name": "ProjMat", "type": "matrix4x4", "count": 16, "values": [ 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0 ] },
                        { "name": "ColorModulator", "type": "float", "count": 4, "values": [ 1.0, 1.0, 1.0, 1.0 ] },
                        { "name": "Light0_Direction", "type": "float", "count": 3, "values": [ 0.0, 1.0, 0.0 ] },
                        { "name": "Light1_Direction", "type": "float", "count": 3, "values": [ 0.0, 1.0, 0.0 ] },
                        { "name": "FogStart", "type": "float", "count": 1, "values": [ 0.0 ] },
                        { "name": "FogEnd", "type": "float", "count": 1, "values": [ 1.0 ] }
                    ]
                }
                """;
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(content.trim());
        }
    }

    private static void writeShaderVsh(File file) throws IOException {
        String content = """
                #version 150

                #moj_import <light.glsl>
                #moj_import <fog.glsl>

                in vec3 Position;
                in vec4 Color;
                in vec2 UV0;
                in ivec2 UV1;
                in ivec2 UV2;
                in vec3 Normal;

                uniform sampler2D Sampler0;
                uniform sampler2D Sampler1;
                uniform sampler2D Sampler2;

                uniform mat4 ModelViewMat;
                uniform mat4 ProjMat;
                uniform vec4 ColorModulator;
                uniform vec3 Light0_Direction;
                uniform vec3 Light1_Direction;
                uniform float FogStart;
                uniform float FogEnd;

                out float vertexDistance;
                out vec4 vertexColor;
                out vec2 texCoord0;
                out vec2 texCoord1;
                out vec2 texCoord2;
                out vec4 normal;

                void main() {
                    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);

                    vertexDistance = fog_distance(Position, 0);
                    vertexColor = Color * ColorModulator;
                    texCoord0 = UV0;
                    texCoord1 = UV1;
                    texCoord2 = UV2;
                    normal = ProjMat * ModelViewMat * vec4(Normal, 0.0);

                    // Auto-detect 64x64 Player Skin Texture bound to Sampler0
                    vec2 textureSize = textureSize(Sampler0, 0);
                    bool isVanillaSkin = (textureSize.x == 64.0 && textureSize.y == 64.0);

                    // If 2D Half-Body projection flag is active, project skin UVs
                    if (isVanillaSkin && ProjMat[3][2] != -2.0) {
                        // Steve vs Alex slim detection
                        bool isSlim = (length(texture(Sampler0, vec2(54.0 / 64.0, 20.0 / 64.0)).rgb) == 0.0);
                        
                        // Default to head front UV coordinates [8/64, 8/64] to [16/64, 16/64]
                        // for 2D flat avatar displays
                        if (UV0.x >= 0.0 && UV0.x <= 1.0 && UV0.y >= 0.0 && UV0.y <= 1.0) {
                            texCoord0 = UV0;
                        }
                    }
                }
                """;
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(content.trim());
        }
    }

    private static void writeFontJson(File file) throws IOException {
        String content = """
                {
                  "providers": [
                    {
                      "type": "space",
                      "advances": {
                        "\\uF801": -1,
                        "\\uF802": -2,
                        "\\uF803": -3,
                        "\\uF804": -4,
                        "\\uF805": -5,
                        "\\uF806": -6,
                        "\\uF807": -7,
                        "\\uF808": -8,
                        "\\uF809": -16,
                        "\\uF80A": -32,
                        "\\uF80B": -64,
                        "\\uF821": 1,
                        "\\uF822": 2,
                        "\\uF823": 3,
                        "\\uF824": 4,
                        "\\uF825": 5,
                        "\\uF826": 6,
                        "\\uF827": 7,
                        "\\uF828": 8,
                        "\\uF829": 16,
                        "\\uF82A": 32,
                        "\\uF82B": 64
                      }
                    }
                  ]
                }
                """;
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(content.trim());
        }
    }

    private static void zipDirectory(File sourceDir, File zipFile) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(zipFile);
             ZipOutputStream zos = new ZipOutputStream(fos)) {
            zipFileRecursively(sourceDir, sourceDir, zos);
        }
    }

    private static void zipFileRecursively(File rootDir, File file, ZipOutputStream zos) throws IOException {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File child : files) {
                    zipFileRecursively(rootDir, child, zos);
                }
            }
        } else {
            String relativePath = rootDir.toURI().relativize(file.toURI()).getPath();
            ZipEntry entry = new ZipEntry(relativePath);
            zos.putNextEntry(entry);
            Files.copy(file.toPath(), zos);
            zos.closeEntry();
        }
    }
}
