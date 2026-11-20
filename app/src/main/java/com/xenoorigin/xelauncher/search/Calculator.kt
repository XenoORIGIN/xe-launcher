package com.xenoorigin.xelauncher.search

import java.math.BigDecimal
import java.math.RoundingMode

/** Small recursive-descent calculator: + − × ÷ * / ^ % ( ) and decimals. Invalid input → null. */
object Calculator {
    fun eval(expr: String): Double? {
        val s = expr.replace('×', '*').replace('÷', '/').replace('−', '-').replace(",", "").filterNot { it.isWhitespace() }
        if (s.isEmpty() || s.any { it !in "0123456789.+-*/^%()" }) return null
        return runCatching {
            val p = Parser(s); val v = p.expr()
            if (p.i != s.length || v.isNaN() || v.isInfinite()) null else v
        }.getOrNull()
    }

    fun format(v: Double): String =
        BigDecimal(v).setScale(10, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString().let { if (it == "-0") "0" else it }

    private class Parser(val s: String) {
        var i = 0
        fun expr(): Double {
            var v = term()
            while (i < s.length && (s[i] == '+' || s[i] == '-')) { val op = s[i++]; val r = term(); v = if (op == '+') v + r else v - r }
            return v
        }
        fun term(): Double {
            var v = power()
            while (i < s.length && (s[i] == '*' || s[i] == '/')) {
                val op = s[i++]; val r = power()
                v = if (op == '*') v * r else { if (r == 0.0) throw ArithmeticException(); v / r }
            }
            return v
        }
        fun power(): Double {
            val b = unary()
            return if (i < s.length && s[i] == '^') { i++; Math.pow(b, power()) } else b
        }
        fun unary(): Double = if (i < s.length && s[i] == '-') { i++; -unary() } else if (i < s.length && s[i] == '+') { i++; unary() } else postfix()
        fun postfix(): Double {
            var v = atom()
            while (i < s.length && s[i] == '%') { i++; v /= 100.0 }
            return v
        }
        fun atom(): Double {
            if (i < s.length && s[i] == '(') { i++; val v = expr(); require(i < s.length && s[i] == ')'); i++; return v }
            val start = i
            while (i < s.length && (s[i].isDigit() || s[i] == '.')) i++
            require(i > start)
            return s.substring(start, i).toDouble()
        }
    }
}
