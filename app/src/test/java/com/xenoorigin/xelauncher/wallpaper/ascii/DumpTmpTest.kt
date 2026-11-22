package com.xenoorigin.xelauncher.wallpaper.ascii
import com.xenoorigin.xelauncher.settings.AsciiColor
import com.xenoorigin.xelauncher.wallpaper.skyAt
import org.junit.Test
fun dumpPpm(path: String, px: IntArray, w: Int, h: Int) {
    val out = java.io.ByteArrayOutputStream(); out.write("P6 $w $h 255\n".toByteArray())
    for (p in px) { out.write((p shr 16) and 255); out.write((p shr 8) and 255); out.write(p and 255) }
    java.io.File(path).writeBytes(out.toByteArray())
}
class DumpTmpTest {
    @Test fun scenes() {
        AsciiScene.entries.forEachIndexed { k, sc ->
            val f = AsciiField(72, 98); f.fill(sc, 2f)
            val px = IntArray(360 * 784); AsciiRaster.rasterize(f.cells, px)
            dumpPpm("/tmp/asc/scene$k.ppm", px, 360, 784)
        }
    }
    @Test fun dump() {
        val looks = listOf(null to AsciiColor.MONO, skyAt(12*60) to AsciiColor.MONO, skyAt(18*60+30) to AsciiColor.SKY, skyAt(0) to AsciiColor.ACCENT)
        looks.forEachIndexed { k, (lk, m) ->
            val f = AsciiField(72, 98); f.fill(AsciiScene.LAKE, 2f, lk, m, 0xFF2F8BFF)
            val px = IntArray(360 * 784); AsciiRaster.rasterize(f.cells, px)
            dumpPpm("/tmp/asc/lake$k.ppm", px, 360, 784)
        }
    }
}
