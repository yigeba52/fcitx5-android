package org.fcitx.fcitx5.android.input.picker

import android.graphics.Rect
import android.view.Gravity
import androidx.core.content.ContextCompat
import androidx.transition.Slide
import androidx.viewpager2.widget.ViewPager2
import org.fcitx.fcitx5.android.input.broadcast.InputBroadcastReceiver
import org.fcitx.fcitx5.android.input.dependency.theme
import org.fcitx.fcitx5.android.input.keyboard.*
import org.fcitx.fcitx5.android.input.popup.PopupComponent
import org.fcitx.fcitx5.android.input.popup.PopupListener
import org.fcitx.fcitx5.android.input.wm.EssentialWindow
import org.fcitx.fcitx5.android.input.wm.InputWindow
import org.fcitx.fcitx5.android.input.wm.InputWindowManager
import org.mechdancer.dependency.manager.must

class PickerWindow(val data: List<Pair<String, Array<String>>>) :
    InputWindow.ExtendedInputWindow<PickerWindow>(), EssentialWindow, InputBroadcastReceiver {

    private val theme by manager.theme()
    private val windowManager: InputWindowManager by manager.must()
    private val commonKeyActionListener: CommonKeyActionListener by manager.must()
    private val popup: PopupComponent by manager.must()

    companion object : EssentialWindow.Key

    override val key: EssentialWindow.Key
        get() = PickerWindow

    private lateinit var pickerLayout: PickerLayout
    private lateinit var pickerPagesAdapter: PickerPagesAdapter

    override fun enterAnimation(lastWindow: InputWindow) = Slide().apply {
        slideEdge = Gravity.BOTTOM
    }.takeIf {
        // disable animation switching between keyboard
        lastWindow !is KeyboardWindow
    }

    override fun exitAnimation(nextWindow: InputWindow) = super.exitAnimation(nextWindow).takeIf {
        // disable animation switching between keyboard
        nextWindow !is KeyboardWindow
    }

    private val keyActionListener = KeyActionListener { it, source ->
        when (it) {
            is KeyAction.LayoutSwitchAction -> {
                // Switch to NumberKeyboard before attaching KeyboardWindow
                (windowManager.getEssentialWindow(KeyboardWindow) as KeyboardWindow)
                    .switchLayout(it.act)
                // The real switchLayout (detachCurrentLayout and attachLayout) in KeyboardWindow is postponed,
                // so we have to postpone attachWindow as well
                ContextCompat.getMainExecutor(context).execute {
                    windowManager.attachWindow(KeyboardWindow)
                }
            }
            else -> {
                if (it is KeyAction.CommitAction) {
                    pickerPagesAdapter.insertRecent(it.text)
                }
                commonKeyActionListener.listener.onKeyAction(it, source)
            }
        }
    }

    private val popupListener: PopupListener by lazy {
        object : PopupListener by popup.listener {
            override fun onShowKeyboard(viewId: Int, keyboard: KeyDef.Popup.Keyboard, bounds: Rect) {
                // prevent ViewPager from consuming swipe gesture when popup keyboard shown
                pickerLayout.pager.isUserInputEnabled = false
                popup.listener.onShowKeyboard(viewId, keyboard, bounds)
            }

            override fun onDismiss(viewId: Int) {
                popup.listener.onDismiss(viewId)
                // restore ViewPager scrolling
                pickerLayout.pager.isUserInputEnabled = true
            }
        }
    }

    override fun onCreateView() = PickerLayout(context, theme).apply {
        pickerLayout = this
        pickerPagesAdapter = PickerPagesAdapter(theme, keyActionListener, popupListener, data)
        tabsUi.apply {
            setTabs(pickerPagesAdapter.categories)
            setOnTabClickListener { i ->
                pager.setCurrentItem(pickerPagesAdapter.getStartPageOfCategory(i), false)
            }
        }
        pager.apply {
            adapter = pickerPagesAdapter
            registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageScrolled(
                    position: Int,
                    positionOffset: Float,
                    positionOffsetPixels: Int
                ) {
                    val range = pickerPagesAdapter.getCategoryRangeOfPage(position)
                    val start = range.first
                    val total = range.last - start + 1
                    val current = position - start
                    paginationUi.updatePageCount(total)
                    paginationUi.updateScrollProgress(current, positionOffset)
                }

                override fun onPageSelected(position: Int) {
                    tabsUi.activateTab(pickerPagesAdapter.getCategoryOfPage(position))
                    popup.dismissAll()
                }
            })
            // show first symbol category by default, rather than recently used
            val initialPage = pickerPagesAdapter.getStartPageOfCategory(1)
            setCurrentItem(initialPage, false)
            // ViewPager2#setCurrentItem(Int, smoothScroll = false) won't trigger onPageScrolled
            // need to call updatePageCount manually
            paginationUi.updatePageCount(
                pickerPagesAdapter.getCategoryRangeOfPage(initialPage).run { last - first + 1 }
            )
        }
    }

    override fun onCreateBarExtension() = pickerLayout.tabsUi.root

    override fun onAttached() {
        pickerLayout.embeddedKeyboard.keyActionListener = keyActionListener
    }

    override fun onDetached() {
        popup.dismissAll()
        pickerLayout.embeddedKeyboard.keyActionListener = null
        pickerPagesAdapter.saveRecent()
    }

    override val showTitle = false
}