package com.xenoorigin.xelauncher

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class LocalisationTest {
    private fun keys(path: String): Set<String> {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(File(path))
        val list = doc.getElementsByTagName("string")
        return (0 until list.length).map { list.item(it) }
            .filter { it.attributes.getNamedItem("translatable")?.nodeValue != "false" }
            .map { it.attributes.getNamedItem("name").nodeValue }.toSet()
    }

    @Test fun thaiAndEnglish_haveTheSameKeys() {
        val en = keys("src/main/res/values/strings.xml")
        val th = keys("src/main/res/values-th/strings.xml")
        assertThat(en - th).isEmpty()
        assertThat(th - en).isEmpty()
        assertThat(en.size).isGreaterThan(100)
    }
}
