package cc.thisis98k.lazymouse.data

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

internal val Context.prefsStore by preferencesDataStore("lazymouse")
