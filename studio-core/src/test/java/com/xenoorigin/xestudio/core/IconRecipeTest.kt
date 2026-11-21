package com.xenoorigin.xestudio.core

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class IconRecipeTest {
    @Test fun roundTripKeepsEveryField() {
        val r = IconRecipe(
            shape = TileShape.HEXAGON, background = Fill(listOf(0xFF102030.toInt(), 0xFF405060.toInt()), 45f),
            treatment = Treatment.DUOTONE, tint = 0xFF00FF00.toInt(), tint2 = 0xFF0000FF.toInt(),
            outlineWidth = 2f, outlineColor = 0xFFFF0000.toInt(), shadow = .5f, glyphScale = .7f,
            overrides = mapOf(
                "a/b#0" to IconOverride.Image("o1.webp"),
                "c/d#0" to IconOverride.Letter("X", 0xFFFFFFFF.toInt(), Fill(listOf(0xFF000000.toInt()))),
            ),
        )
        assertThat(IconRecipe.decode(IconRecipe.encode(r))).isEqualTo(r)
    }

    @Test fun unknownFieldsAreIgnored() {
        val json = IconRecipe.encode(IconRecipe()).dropLast(1) + ",\"sparkle\":1}"
        assertThat(IconRecipe.decode(json)).isEqualTo(IconRecipe())
    }

    @Test fun newerSchemaIsRejected() {
        val json = IconRecipe.encode(IconRecipe()).replace("\"schemaVersion\":1", "\"schemaVersion\":2")
        assertThat(json).contains("\"schemaVersion\":2")
        assertThat(IconRecipe.decode(json)).isNull()
    }

    @Test fun garbageIsRejected() {
        assertThat(IconRecipe.decode("{nope")).isNull()
        assertThat(IconRecipe.decode("")).isNull()
    }

    @Test fun defaultsAreMonoSquircle() {
        val r = IconRecipe()
        assertThat(r.shape).isEqualTo(TileShape.SQUIRCLE)
        assertThat(r.treatment).isEqualTo(Treatment.MONO)
        assertThat(r.schemaVersion).isEqualTo(IconRecipe.CURRENT_SCHEMA)
    }
}
