package com.aki.lnga.ui

import androidx.compose.runtime.Composable
import com.aki.lnga.data.PageControlPosition

internal enum class PageControlSide { Left, Right }

internal fun pageControlSide(position: PageControlPosition): PageControlSide =
    if (position == PageControlPosition.Left) PageControlSide.Left else PageControlSide.Right

@Composable
internal fun rememberPageControlSide(position: PageControlPosition): PageControlSide =
    pageControlSide(position)
