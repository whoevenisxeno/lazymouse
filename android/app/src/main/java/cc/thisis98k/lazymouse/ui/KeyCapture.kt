package cc.thisis98k.lazymouse.ui

import android.content.Context
import android.text.InputType
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.BaseInputConnection
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection

interface KeySink {
    fun text(s: String)
    fun special(name: String)
}

class KeyCaptureView(context: Context) : View(context) {
    var sink: KeySink? = null

    init {
        isFocusable = true
        isFocusableInTouchMode = true
    }

    override fun onCheckIsTextEditor() = true

    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection {
        outAttrs.inputType = InputType.TYPE_CLASS_TEXT or
            InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS or
            InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        outAttrs.imeOptions = EditorInfo.IME_ACTION_NONE or EditorInfo.IME_FLAG_NO_FULLSCREEN

        return object : BaseInputConnection(this, false) {
            override fun commitText(text: CharSequence?, newCursorPosition: Int): Boolean {
                text?.toString()?.takeIf { it.isNotEmpty() }?.let { sink?.text(it) }
                return true
            }

            override fun setComposingText(text: CharSequence?, newCursorPosition: Int): Boolean {
                return true
            }

            override fun deleteSurroundingText(beforeLength: Int, afterLength: Int): Boolean {
                repeat(beforeLength.coerceAtLeast(0)) { sink?.special("backspace") }
                return true
            }

            override fun sendKeyEvent(event: KeyEvent): Boolean {
                if (event.action == KeyEvent.ACTION_DOWN) when (event.keyCode) {
                    KeyEvent.KEYCODE_DEL -> sink?.special("backspace")
                    KeyEvent.KEYCODE_ENTER -> sink?.special("enter")
                    KeyEvent.KEYCODE_TAB -> sink?.special("tab")
                    KeyEvent.KEYCODE_DPAD_LEFT -> sink?.special("left")
                    KeyEvent.KEYCODE_DPAD_RIGHT -> sink?.special("right")
                    KeyEvent.KEYCODE_DPAD_UP -> sink?.special("up")
                    KeyEvent.KEYCODE_DPAD_DOWN -> sink?.special("down")
                    else -> {
                        val ch = event.unicodeChar
                        if (ch != 0) sink?.text(ch.toChar().toString())
                    }
                }
                return true
            }
        }
    }
}
