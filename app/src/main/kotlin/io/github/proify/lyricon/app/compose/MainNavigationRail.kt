package io.github.proify.lyricon.app.compose

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.proify.lyricon.app.R
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.blur
import top.yukonga.miuix.kmp.blur.drawBackdrop
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.basic.NavigationRail
import top.yukonga.miuix.kmp.basic.NavigationRailItem
import top.yukonga.miuix.kmp.basic.NavigationRailValue
import top.yukonga.miuix.kmp.basic.rememberNavigationRailState
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** Uses the same window breakpoints and expanded navigation as KnSQL's App.kt / Adaptive.kt. */
@Composable
fun shouldShowNavigationRail(): Boolean {
    val windowInfo = LocalWindowInfo.current
    return with(LocalDensity.current) {
        val width = windowInfo.containerSize.width.toDp()
        val height = windowInfo.containerSize.height.toDp()
        val ratio = if (width.value == 0f) 1f else height / width
        width >= 840.dp || (width >= 600.dp && ratio < 1.2f)
    }
}

@Composable
fun MainNavigationRail(
    items: List<MainBottomBarItem>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    backdrop: Backdrop? = null,
) {
    val state = rememberNavigationRailState(NavigationRailValue.Expanded)
    val surface = MiuixTheme.colorScheme.surface
    val dark = MiuixTheme.colorScheme.background.luminance() < 0.5f
    val blurSupported = remember { isRuntimeShaderSupported() }
    val railBackground = when {
        backdrop != null && blurSupported -> Modifier.drawBackdrop(
            backdrop = backdrop,
            shape = { RectangleShape },
            effects = { blur(24.dp.toPx()) },
            onDrawSurface = { drawRect(surface.copy(alpha = if (dark) 0.46f else 0.62f)) },
        )
        backdrop != null -> Modifier.background(surface.copy(alpha = 0.86f))
        else -> Modifier.background(surface)
    }
    NavigationRail(
        state = state,
        modifier = railBackground,
        defaultWindowInsetsPadding = true,
        color = Color.Transparent,
        expandContentDescription = stringResource(R.string.expand_navigation),
        collapseContentDescription = stringResource(R.string.collapse_navigation),
    ) {
        items.forEachIndexed { index, item ->
            NavigationRailItem(
                selected = selectedIndex == index,
                onClick = { onSelected(index) },
                icon = item.icon,
                label = item.label,
            )
        }
    }
}
