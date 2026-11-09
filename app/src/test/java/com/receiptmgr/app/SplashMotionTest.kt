package com.receiptmgr.app

import androidx.compose.ui.unit.dp
import com.receiptmgr.app.presentation.features.splash.SplashMotionSpec
import com.receiptmgr.app.presentation.features.splash.SplashState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SplashMotionTest {

    private val spec = SplashMotionSpec.Default

    @Test
    fun `initial state has correct bounds`() {
        val state = SplashState.initial(spec)

        assertEquals(0f, state.progress, 0.0001f)
        assertEquals(0f, state.logoAlpha, 0.0001f)
        assertEquals(0.9f, state.logoScale, 0.0001f)
        assertEquals(0f, state.titleAlpha, 0.0001f)
        assertEquals(-24.dp, state.titleOffsetY)
        assertEquals(0.dp, state.dividerWidth)
        assertEquals(0f, state.dividerAlpha, 0.0001f)
    }

    @Test
    fun `mid timeline maintains sequenced timing windows`() {
        // At t = 0.1: Before logo fade/scale
        val stateAt01 = SplashState.calculate(0.1f, spec)
        assertEquals(0f, stateAt01.logoAlpha, 0.0001f)
        assertEquals(0.9f, stateAt01.logoScale, 0.0001f)
        assertEquals(0f, stateAt01.titleAlpha, 0.0001f)

        // At t = 0.4: Logo is fading in and scaling, title has not unfolded yet
        val stateAt04 = SplashState.calculate(0.4f, spec)
        assertTrue("Logo alpha should be > 0", stateAt04.logoAlpha > 0f)
        assertTrue("Logo alpha should be < 1", stateAt04.logoAlpha < 1f)
        assertTrue("Logo scale should be > 0.9", stateAt04.logoScale > 0.9f)
        assertEquals(0f, stateAt04.titleAlpha, 0.0001f)

        // At t = 0.8: Logo is fully visible, title is unfolding
        val stateAt08 = SplashState.calculate(0.8f, spec)
        assertEquals(1f, stateAt08.logoAlpha, 0.0001f)
        assertEquals(1f, stateAt08.logoScale, 0.0001f)
        assertTrue("Title alpha should be > 0", stateAt08.titleAlpha > 0f)
        assertTrue("Title offset should be approaching 0", stateAt08.titleOffsetY.value > -24f)
    }

    @Test
    fun `completed state reaches target values`() {
        val state = SplashState.completed(spec)

        assertEquals(1f, state.progress, 0.0001f)
        assertEquals(1f, state.logoAlpha, 0.0001f)
        assertEquals(1.0f, state.logoScale, 0.0001f)
        assertEquals(1f, state.titleAlpha, 0.0001f)
        assertEquals(0f, state.titleOffsetY.value, 0.0001f)
        assertEquals(120.dp, state.dividerWidth)
        assertEquals(0.30f, state.dividerAlpha, 0.0001f)
    }

    @Test
    fun `motion properties are monotonically non-decreasing`() {
        var prevLogoAlpha = 0f
        var prevLogoScale = 0.9f
        var prevTitleAlpha = 0f
        var prevOffsetY = -24f

        for (step in 0..100) {
            val progress = step / 100f
            val state = SplashState.calculate(progress, spec)

            assertTrue("Logo alpha monotonic at $progress", state.logoAlpha >= prevLogoAlpha)
            assertTrue("Logo scale monotonic at $progress", state.logoScale >= prevLogoScale)
            assertTrue("Title alpha monotonic at $progress", state.titleAlpha >= prevTitleAlpha)
            assertTrue("Title offset monotonic at $progress", state.titleOffsetY.value >= prevOffsetY)

            prevLogoAlpha = state.logoAlpha
            prevLogoScale = state.logoScale
            prevTitleAlpha = state.titleAlpha
            prevOffsetY = state.titleOffsetY.value
        }
    }
}
