/*
 * Copyright 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.matrix.vector.ui

import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.LastBaseline
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Adapted from AndroidX RectListIntegrationTest (b/549552303, change Icec9e).
 * Exercise the failing measurement path, not just whether the manager can cold-start.
 * These need no daemon and cover the Compose shared by standalone and injected hosts.
 */
@RunWith(AndroidJUnit4::class)
class RectListRegressionTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun nestedBaselineLayout() {
        rule.setContent {
            Row {
                Row(Modifier.alignByBaseline()) {
                    BasicText("text", Modifier.size(10.dp).alignByBaseline())
                }
            }
        }
        rule.waitForIdle()
    }

    @Test
    fun nestedBaselineLayoutWithPadding() {
        rule.setContent {
            Row {
                Row(Modifier.padding(10.dp).alignByBaseline()) {
                    BasicText("text", Modifier.size(10.dp).alignByBaseline())
                }
            }
        }
        rule.waitForIdle()
    }

    @Test
    fun baselineQueryDuringLazyItemReuse() {
        val itemPx = 20
        val itemDp = with(rule.density) { itemPx.toDp() }
        val state = LazyListState()
        rule.setContent {
            LazyColumn(state = state, modifier = Modifier.height(itemDp * 2)) {
                items(10) { index ->
                    BaselineQueryingLayout {
                        Box(Modifier.fillMaxWidth()) {
                            Spacer(
                                Modifier.layout { measurable, constraints ->
                                    val placeable = measurable.measure(constraints)
                                    layout(10 + index, itemPx, mapOf(FirstBaseline to index)) {
                                        placeable.place(0, 0)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
        // Move item 0 off-screen, retire its composition, then reuse its slot for item 4.
        repeat(3) {
            rule.runOnIdle { runBlocking { state.scrollBy(itemPx.toFloat()) } }
        }
        rule.runOnIdle { assertEquals(3, state.firstVisibleItemIndex) }
    }

    @Composable
    private fun BaselineQueryingLayout(content: @Composable () -> Unit) {
        Layout(content = { Box { content() } }) { measurables, constraints ->
            val placeable = measurables.single().measure(constraints)
            val firstBaseline = placeable[FirstBaseline]
            val lastBaseline = placeable[LastBaseline]
            layout(
                width = placeable.width,
                height = placeable.height,
                alignmentLines = mapOf(FirstBaseline to firstBaseline, LastBaseline to lastBaseline),
            ) {
                placeable.placeRelative(0, 0)
            }
        }
    }
}
