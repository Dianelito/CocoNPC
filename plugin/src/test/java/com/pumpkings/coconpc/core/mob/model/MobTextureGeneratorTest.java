package com.pumpkings.coconpc.core.mob.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Map;
import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;

class MobTextureGeneratorTest {
    @Test
    void mapsEveryCubeFaceToItsPlayerHeadRegion(@TempDir Path folder) throws Exception {
        BufferedImage atlas = new BufferedImage(48, 8, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = atlas.createGraphics();
        Color[] colors = {
                Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW, Color.MAGENTA, Color.CYAN
        };
        for (int index = 0; index < colors.length; index++) {
            graphics.setColor(colors[index]);
            graphics.fillRect(index * 8, 0, 8, 8);
        }
        graphics.dispose();

        ByteArrayOutputStream png = new ByteArrayOutputStream();
        ImageIO.write(atlas, "png", png);
        String source = "data:image/png;base64,"
                + Base64.getEncoder().encodeToString(png.toByteArray());

        String json = """
                {
                  "meta": {"format_version": "4.10", "model_format": "free"},
                  "name": "face_test",
                  "resolution": {"width": 48, "height": 8},
                  "textures": [{"name": "atlas", "uuid": "tex", "source": "%s"}],
                  "elements": [
                    {
                      "name": "cube",
                      "uuid": "cube",
                      "from": [0, 0, 0],
                      "to": [8, 8, 8],
                      "faces": {
                        "north": {"texture": 0, "uv": [0, 0, 8, 8]},
                        "east":  {"texture": 0, "uv": [8, 0, 16, 8]},
                        "south": {"texture": 0, "uv": [16, 0, 24, 8]},
                        "west":  {"texture": 0, "uv": [24, 0, 32, 8]},
                        "up":    {"texture": 0, "uv": [32, 0, 40, 8]},
                        "down":  {"texture": 0, "uv": [40, 0, 48, 8]}
                      }
                    }
                  ]
                }
                """.formatted(source);

        Path modelFile = folder.resolve("face_test.bbmodel");
        Files.writeString(modelFile, json, StandardCharsets.UTF_8);

        BbModel model = BbModelParser.parse(modelFile, Logger.getLogger("test"));
        assertThat(model).isNotNull();

        Map<String, BufferedImage> atlases = MobTextureGenerator.loadAtlases(
                model, Logger.getLogger("test"));
        assertThat(atlases).containsKey("atlas");

        BbModel.Cube cube = model.getBones().get(0).getCubes().get(0);
        BufferedImage skin = MobTextureGenerator.composeSkin(model, cube, atlases);

        assertThat(skin.getWidth()).isEqualTo(MobTextureGenerator.SKIN_SIZE);
        assertThat(skin.getHeight()).isEqualTo(MobTextureGenerator.SKIN_SIZE);

        assertFace(skin, FaceDirection.NORTH, Color.RED);
        assertFace(skin, FaceDirection.EAST, Color.GREEN);
        assertFace(skin, FaceDirection.SOUTH, Color.BLUE);
        assertFace(skin, FaceDirection.WEST, Color.YELLOW);
        assertFace(skin, FaceDirection.UP, Color.MAGENTA);
        assertFace(skin, FaceDirection.DOWN, Color.CYAN);
    }

    @Test
    void averagesVisiblePixelsAndPicksRedConcrete() {
        BufferedImage atlas = new BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = atlas.createGraphics();
        graphics.setColor(Color.RED);
        graphics.fillRect(0, 0, 8, 8);
        graphics.dispose();

        String json = """
                {
                  "name": "avg",
                  "resolution": {"width": 8, "height": 8},
                  "textures": [{"name": "atlas", "uuid": "tex"}],
                  "elements": [
                    {
                      "name": "cube",
                      "from": [0, 0, 0],
                      "to": [8, 8, 8],
                      "faces": {"north": {"texture": 0, "uv": [0, 0, 8, 8]}}
                    }
                  ]
                }
                """;
        Path modelFile = null;
        try {
            modelFile = Files.createTempFile("avg", ".bbmodel");
            Files.writeString(modelFile, json, StandardCharsets.UTF_8);
            BbModel model = BbModelParser.parse(modelFile, Logger.getLogger("test"));
            BbModel.Cube cube = model.getBones().get(0).getCubes().get(0);

            int average = MobTextureGenerator.averageColor(model, cube,
                    Map.of("atlas", atlas));
            assertThat(average).isEqualTo(0xFF0000);
            assertThat(MobTextureGenerator.closestConcrete(average))
                    .isEqualTo(org.bukkit.Material.RED_CONCRETE);
        } catch (Exception exception) {
            throw new AssertionError(exception);
        } finally {
            if (modelFile != null) {
                try {
                    Files.deleteIfExists(modelFile);
                } catch (Exception ignored) {
                }
            }
        }
    }

    @Test
    void headRegionsMatchTheVanillaSkinLayout() {
        assertThat(FaceDirection.EAST.getSkinX()).isZero();
        assertThat(FaceDirection.NORTH.getSkinX()).isEqualTo(8);
        assertThat(FaceDirection.WEST.getSkinX()).isEqualTo(16);
        assertThat(FaceDirection.SOUTH.getSkinX()).isEqualTo(24);
        assertThat(FaceDirection.UP.getSkinY()).isZero();
        assertThat(FaceDirection.DOWN.getSkinY()).isZero();
    }

    private static void assertFace(BufferedImage skin, FaceDirection direction, Color expected) {
        int rgb = skin.getRGB(direction.getSkinX() + 4, direction.getSkinY() + 4) & 0xFFFFFF;
        assertThat(rgb)
                .as("face %s", direction)
                .isEqualTo(expected.getRGB() & 0xFFFFFF);
    }
}
