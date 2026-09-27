/*
 * Copyright (C) 2022 The Android Open Source Project
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
package com.android.quicksearchbox

import android.app.SearchManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ComponentInfo
import android.content.pm.PackageManager
import android.content.pm.PackageManager.NameNotFoundException
import android.content.pm.ResolveInfo
import android.os.Bundle
import android.speech.RecognizerIntent
import android.util.Log

/** Voice Search integration. */
class VoiceSearch(context: Context?) {

  private val mContext: Context?

  protected val context: Context?
    get() = mContext

  fun shouldShowVoiceSearch(): Boolean {
    // IchthysOS: the user does not want voice search anywhere in the OS. This is the single
    // gate SearchActivityView uses to show/hide the microphone button in the search box (and
    // SearchWidgetProvider for the widget's mic), so returning false removes the affordance
    // outright. Typed search in QSB is untouched.
    //
    // Today this would also be false because nothing on the device handles
    // RecognizerIntent.ACTION_WEB_SEARCH, but that is accidental - installing any app that
    // does would put the mic back. This makes it deterministic.
    if (VOICE_SEARCH_DISABLED) return false
    return isVoiceSearchAvailable
  }

  protected fun createVoiceSearchIntent(): Intent {
    return Intent(RecognizerIntent.ACTION_WEB_SEARCH)
  }

  private val resolveInfo: ResolveInfo?
    @Suppress("DEPRECATION")
    get() {
      val intent: Intent = createVoiceSearchIntent()
      return mContext
        ?.getPackageManager()
        ?.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
    }
  val isVoiceSearchAvailable: Boolean
    get() = resolveInfo != null

  fun createVoiceWebSearchIntent(appData: Bundle?): Intent? {
    // IchthysOS: no voice search. SearchWidgetProvider and AbstractSource call this directly
    // rather than going through shouldShowVoiceSearch(), and a null intent is already the
    // "no voice search available" contract both of them handle (the widget hides its mic).
    if (VOICE_SEARCH_DISABLED) return null
    if (!isVoiceSearchAvailable) return null
    val intent: Intent = createVoiceSearchIntent()
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    intent.putExtra(
      RecognizerIntent.EXTRA_LANGUAGE_MODEL,
      RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH
    )
    if (appData != null) {
      intent.putExtra(SearchManager.APP_DATA, appData)
    }
    return intent
  }

  /**
   * Create an intent to launch the voice search help screen, if any exists.
   * @return The intent, or null.
   */
  fun createVoiceSearchHelpIntent(): Intent? {
    return null
  }

  /**
   * Gets the `versionCode` of the currently installed voice search package.
   *
   * @return The `versionCode` of voiceSearch, or 0 if none is installed.
   */
  val version: Long
    @Suppress("DEPRECATION")
    get() {
      val ri: ResolveInfo = resolveInfo ?: return 0
      val ci: ComponentInfo = if (ri.activityInfo != null) ri.activityInfo else ri.serviceInfo
      return try {
        context!!.getPackageManager().getPackageInfo(ci.packageName, 0).getLongVersionCode()
      } catch (e: NameNotFoundException) {
        Log.e(TAG, "Cannot find voice search package " + ci.packageName, e)
        0
      }
    }
  val component: ComponentName
    get() = createVoiceSearchIntent().resolveActivity(context!!.getPackageManager())

  companion object {
    private const val TAG = "QSB.VoiceSearch"

    /**
     * IchthysOS: the user considers voice search spyware and wants no microphone affordance in
     * the OS. Flip to false to restore the mic button in the QSB search box and widget. Plain
     * `val`, not `const val`, so the early returns don't make the rest statically unreachable.
     */
    private val VOICE_SEARCH_DISABLED = true
  }

  init {
    mContext = context
  }
}
