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
 * 1. pack.mcmeta (compatible with Minecraft 26.1+ / 1.21.4+)
 * 2. Vanilla Core Shaders for 2D Half-Body / Player Skin UV projection
 * 3. Default Font negative-space providers for pixel shifting (%img_offset%)
 */
public class ResourcePackGenerator {

    public static void generate(JavaPlugin plugin) {
        File rpFolder = new File(plugin.getDataFolder(), "resourcepack");
        File targetZip = new File(plugin.getDataFolder(), "PersonaChat_ResourcePack.zip");

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

            // 3. assets/minecraft/models/item/
            File modelDir = new File(rpFolder, "assets/minecraft/models/item");
            modelDir.mkdirs();
            writePlayerBoneModel(new File(modelDir, "player_bone.json"));

            // 4. assets/minecraft/font/
            File fontDir = new File(rpFolder, "assets/minecraft/font");
            fontDir.mkdirs();
            writeFontJson(new File(fontDir, "default.json"));

            // 5. Zip the resource pack
            zipDirectory(rpFolder, targetZip);

            plugin.getLogger().info("Resource Pack generated successfully at: " + targetZip.getName());
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to generate resource pack: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void writePackMcmeta(File file) throws IOException {
        String content = "{\n" +
                "  \"pack\": {\n" +
                "    \"pack_format\": 84,\n" +
                "    \"min_format\": [84, 0],\n" +
                "    \"max_format\": [999, 0],\n" +
                "    \"description\": \"Custom Emotes & Player Skin Shader Resource Pack\"\n" +
                "  }\n" +
                "}";
        try (FileWriter writer = new FileWriter(file, java.nio.charset.StandardCharsets.UTF_8)) {
            writer.write(content);
        }
    }

    private static void writeShaderJson(File file) throws IOException {
        String content = """
                {
                  "vertex": "core/rendertype_entity_translucent",
                  "fragment": "core/rendertype_entity_translucent",
                  "samplers": [
                    { "name": "Sampler0" },
                    { "name": "Sampler1" },
                    { "name": "Sampler2" }
                  ],
                  "defines": {
                    "values": {
                      "ALPHA_CUTOUT": "0.1"
                    },
                    "flags": [
                      "NO_OVERLAY"
                    ]
                  },
                  "uniforms": [
                    { "name": "ModelViewMat", "type": "matrix4x4", "count": 16, "values": [ 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1 ] },
                    { "name": "ProjMat", "type": "matrix4x4", "count": 16, "values": [ 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1 ] },
                    { "name": "ColorModulator", "type": "float", "count": 4, "values": [ 1, 1, 1, 1 ] },
                    { "name": "Light0_Direction", "type": "float", "count": 3, "values": [ 0, 0, 0 ] },
                    { "name": "Light1_Direction", "type": "float", "count": 3, "values": [ 0, 0, 0 ] },
                    { "name": "FogStart", "type": "float", "count": 1, "values": [ 0 ] },
                    { "name": "FogEnd", "type": "float", "count": 1, "values": [ 1 ] },
                    { "name": "FogColor", "type": "float", "count": 4, "values": [ 0, 0, 0, 0 ] }
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

                #moj_import <minecraft:light.glsl>
                #moj_import <minecraft:fog.glsl>

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
                uniform mat4 TextureMat;
                uniform int FogShape;

                uniform vec3 Light0_Direction;
                uniform vec3 Light1_Direction;

                out float vertexDistance;
                out vec4 vertexColor;
                out vec4 lightMapColor;
                out vec4 overlayColor;
                out vec2 texCoord0;

                uniform mat3 IViewRotMat;
                out vec4 normal;
                out vec2 origTexCoord0;
                flat out int isTopLayer;
                flat out int isHead;

                #define VANILLA_SKIN_TEX_SIZE 64.
                #define BONE_TEX_SIZE 16.
                #define BONE_TEX_SIZE_NORM BONE_TEX_SIZE / VANILLA_SKIN_TEX_SIZE
                #define PER_BONE_VTXS 24
                #define PER_BONE_VTXS_BOTH_LAYERS 48

                #define IS_GUI ProjMat[3][2] == -2.
                #define MIGHT_BE_TOP_LAYER UV0.x > 0.5
                #define b1 gl_VertexID % 12

                #define handle_top_layer(index) if (a2 == index && MIGHT_BE_TOP_LAYER) isTopLayer = 1;
                #define b2 1. - sign(length(texture(Sampler0, vec2(54. / 64., 20. / 64.)).rgb)) == 1.

                #define NIGHT_COLOR normalize(vec3(42.0 / 255.0, 42.0 / 255.0, 72.0 / 255.0))
                #define DAY_COLOR normalize(vec3(1.0, 1.0, 1.0))
                float qq1(sampler2D lightMap) {
                    vec3 sunLight = normalize(texture(lightMap, vec2(0.5 / 16.0, 15.5 / 16.0)).rgb);
                    return clamp(pow(length(sunLight - NIGHT_COLOR) / length(DAY_COLOR - NIGHT_COLOR), 4.0), 0.0, 1.0);
                }

                #define SKIP_EMOTE { texCoord0=UV0; return; }

                bool isBlankTransparent(sampler2D s, int x, int y) {
                    vec4 v = texelFetch(s, ivec2(x, y), 0);
                    return v.r == 0 && v.g == 0 && v.b == 0 && v.a == 0;
                }

                void main() {
                    isTopLayer = 0;
                    isHead = 0;
                    origTexCoord0 = UV0;

                    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);

                    vertexDistance = fog_distance(Position, FogShape);
                    vertexColor = minecraft_mix_light(Light0_Direction, Light1_Direction, Normal, Color);
                    lightMapColor = texelFetch(Sampler2, UV2 / 16, 0);
                    overlayColor = texelFetch(Sampler1, UV1, 0);
                    texCoord0 = UV0;

                    vec2 ss = textureSize(Sampler0, 0);
                    bool xx2 = ss.x != VANILLA_SKIN_TEX_SIZE || ss.y != VANILLA_SKIN_TEX_SIZE;
                    bool xx1 = UV0.y <= 0.25;
                    if(!xx1 || IS_GUI || xx2) {
                        texCoord0 = UV0;
                        return;
                    }

                    if (texelFetch(Sampler0, ivec2(0, 0), 0).a != 1.0) SKIP_EMOTE
                    if (texelFetch(Sampler0, ivec2(24, 0), 0).a != 1.0) SKIP_EMOTE
                    if (!isBlankTransparent(Sampler0, 56, 0)) SKIP_EMOTE
                    if (!isBlankTransparent(Sampler0, 19, 32)) SKIP_EMOTE

                    vec2 aa0 = UV0;
                    int a2 = (gl_VertexID / PER_BONE_VTXS) % 14;
                    int a1 = gl_VertexID / PER_BONE_VTXS_BOTH_LAYERS;
                    int a0 = a2 % 2;

                    if(a1 == 6) {
                        isHead = 1;
                    } else if (a1 == 7) {
                        handle_top_layer(1);
                        aa0.x = UV0.x * 2 - a0;
                        if (b2) {
                            if (aa0.x < 0.375) aa0.x *= 8. / 7.;
                            else if (aa0.x > 0.625 && aa0.x < 0.875) {
                                int i = b1;
                                aa0.x = (i == 0 || i == 3) ? (10. / 14.) : (11. / 14.);
                            }
                            aa0.x *= 0.21875;
                        } else {
                            aa0.x *= BONE_TEX_SIZE_NORM;
                        }

                        aa0.y = UV0.y / 0.25;
                        if (aa0.y < 0.75) aa0.y *= 0.5;
                        aa0.x = aa0.x + (16. + a0 * BONE_TEX_SIZE) / VANILLA_SKIN_TEX_SIZE;
                        aa0.y = aa0.y * BONE_TEX_SIZE_NORM + 0.75;
                        aa0.x = aa0.x - (16. / 64.);

                        if(a2 == 1 && UV0.x >= 0.5) isTopLayer = 1;
                    } else if (a1 == 8) {
                        handle_top_layer(2);
                        aa0.y = UV0.y / (BONE_TEX_SIZE_NORM);
                        if (aa0.y < 0.75) aa0.y *= 0.5;

                        float i1 = UV0.x;
                        if(a2 != 2) i1 = i1 - 0.5;
                        i1 /= 0.5;

                        if (i1 < 0.375) i1 /= 1.5;
                        else if (i1 > 0.625 && i1 < 0.875) i1 /= 1.125;

                        if (a2 == 2) aa0.y = aa0.y * BONE_TEX_SIZE_NORM + BONE_TEX_SIZE_NORM;
                        else aa0.y = aa0.y * BONE_TEX_SIZE_NORM + 0.5;

                        aa0.x = i1 * 0.375 + BONE_TEX_SIZE_NORM;
                    } else if (a1 == 9) {
                        handle_top_layer(6);
                        aa0.x = UV0.x * 2 - a0;
                        if (b2) {
                            if (aa0.x < 0.375) aa0.x *= 8. / 7.;
                            else if (aa0.x > 0.625 && aa0.x < 0.875) {
                                int i = b1;
                                aa0.x = (i == 0 || i == 3) ? (10. / 14.) : (11. / 14.);
                            }
                            aa0.x *= 0.21875;
                        } else {
                            aa0.x *= BONE_TEX_SIZE_NORM;
                        }

                        aa0.y = UV0.y / 0.25;
                        if (aa0.y < 0.75) aa0.y *= 0.5;
                        aa0.x = aa0.x + (32. + a0 * BONE_TEX_SIZE) / VANILLA_SKIN_TEX_SIZE;
                        aa0.y = aa0.y * BONE_TEX_SIZE_NORM + 0.75;

                        if(a2 == 6 && UV0.x >= 0.5 && UV0.y == 0.25) isTopLayer = 1;
                    } else if (a1 == 10) {
                        handle_top_layer(8);
                        aa0.x = UV0.x * 2 - a0;
                        if (b2) {
                            if (aa0.x < 0.375) aa0.x *= 8. / 7.;
                            else if (aa0.x > 0.625 && aa0.x < 0.875) {
                                int i = b1;
                                aa0.x = (i == 0 || i == 3) ? (10. / 14.) : (11. / 14.);
                            }
                            aa0.x *= 0.21875;
                        } else {
                            aa0.x *= (BONE_TEX_SIZE_NORM);
                        }

                        aa0.y = UV0.y / 0.25;
                        if (aa0.y < 0.75) aa0.y *= 0.5;
                        aa0.x = aa0.x + 0.625;
                        aa0.y = aa0.y * (BONE_TEX_SIZE_NORM) + (a0 + 1) * 0.25;
                    } else if (a1 == 11) {
                        handle_top_layer(8);
                        aa0.y = UV0.y / (BONE_TEX_SIZE_NORM);
                        if (aa0.y < 0.75) aa0.y *= 0.5;

                        if (a2 == 8) {
                            aa0.x = UV0.x / 0.5 * BONE_TEX_SIZE_NORM;
                            aa0.y = aa0.y * BONE_TEX_SIZE_NORM + BONE_TEX_SIZE_NORM;
                        } else {
                            aa0.x = (UV0.x - 0.5) / 0.5 * BONE_TEX_SIZE_NORM;
                            aa0.y = aa0.y * BONE_TEX_SIZE_NORM + 0.5;
                        }
                    } else {
                        texCoord0 = UV0;
                        return;
                    }
                    texCoord0 = aa0;

                    float dayFactor = qq1(Sampler2);
                    if (dayFactor >= 0.2) {
                        lightMapColor.rgb = mix(lightMapColor.rgb, vec3(1.0), 0.6);
                    }
                }
                """;
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(content.trim());
        }
    }

    private static void writePlayerBoneModel(File file) throws IOException {
        String content = """
                {
                  "parent": "builtin/entity",
                  "display": {
                    "thirdperson_righthand": {
                      "translation": [-1, -2, 21.5],
                      "scale": [1, 1.407, 0.47],
                      "rotation": [90, 180, 0]
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
