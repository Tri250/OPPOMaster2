package com.silas.omaster.ui.theme

import androidx.compose.ui.unit.dp

object Spacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val base = 16.dp
    val lg = 24.dp
    val xl = 32.dp
    val xxl = 48.dp

    // 语义化间距 - 用于统一UI间距
    object Card {
        val padding = 16.dp
        val spacing = 12.dp
        val radius = 16.dp
    }

    object Screen {
        val horizontal = 16.dp
        val vertical = 8.dp
    }

    object Element {
        val tight = 4.dp
        val normal = 8.dp
        val loose = 16.dp
    }
}
