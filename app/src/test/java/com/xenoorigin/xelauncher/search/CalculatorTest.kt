package com.xenoorigin.xelauncher.search

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CalculatorTest {
    @Test fun precedence() { assertThat(Calculator.eval("2+3×4")).isEqualTo(14.0) }
    @Test fun parens() { assertThat(Calculator.eval("(2+3)*4")).isEqualTo(20.0) }
    @Test fun division_andDecimals() { assertThat(Calculator.eval("7.5 ÷ 2.5")).isEqualTo(3.0) }
    @Test fun power_andPercent() {
        assertThat(Calculator.eval("2^10")).isEqualTo(1024.0)
        assertThat(Calculator.eval("50%")).isEqualTo(0.5)
    }
    @Test fun unaryMinus() { assertThat(Calculator.eval("-3+5")).isEqualTo(2.0) }
    @Test fun unbalanced_isNull() { assertThat(Calculator.eval("(1+2")).isNull() }
    @Test fun divideByZero_isNull() { assertThat(Calculator.eval("5/0")).isNull() }
    @Test fun words_isNull() { assertThat(Calculator.eval("hello")).isNull() }
    @Test fun format_dropsTrailingZeros() {
        assertThat(Calculator.format(14.0)).isEqualTo("14")
        assertThat(Calculator.format(1.0 / 3)).isEqualTo("0.3333333333")
    }
}
