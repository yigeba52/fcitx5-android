package org.fcitx.fcitx5.android.input.keyboard

import org.fcitx.fcitx5.android.core.Fcitx

sealed class KeyAction<T> {

    abstract val act: T

    data class FcitxKeyAction(override var act: String) : KeyAction<String>() {
        fun upper() {
            act = act.uppercase()
        }

        fun lower() {
            act = act.lowercase()
        }
    }

    data class CommitAction(override val act: String) : KeyAction<String>()

    data class RepeatStartAction(override val act: String) : KeyAction<String>()

    data class RepeatEndAction(override val act: String) : KeyAction<String>()

    data class CapsAction(val lock: Boolean) : KeyAction<Unit>() {
        override val act: Unit get() = Unit
    }

    object QuickPhraseAction : KeyAction<Unit>() {
        override val act: Unit get() = Unit
        override fun toString(): String = javaClass.simpleName
    }

    object UnicodeAction : KeyAction<Unit>() {
        override val act: Unit get() = Unit
        override fun toString(): String = javaClass.simpleName
    }

    object LangSwitchAction : KeyAction<Unit>() {
        override val act: Unit get() = Unit
        override fun toString(): String = javaClass.simpleName
    }

    object InputMethodSwitchAction : KeyAction<Unit>() {
        override val act: Unit get() = Unit
        override fun toString(): String = javaClass.simpleName
    }

    data class LayoutSwitchAction(override val act: String = "") : KeyAction<String>()

    data class CustomAction(override val act: (Fcitx) -> Unit) : KeyAction<(Fcitx) -> Unit>()

}