/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.ui.tags

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import android.widget.ToggleButton
import androidx.databinding.DataBindingUtil
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import com.alexvt.integrity.R
import com.alexvt.integrity.databinding.ViewTagEditBinding
import com.alexvt.integrity.lib.metadata.Tag
import com.alexvt.integrity.lib.util.IntentUtil
import android.widget.RadioGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import com.afollestad.materialdialogs.callbacks.onCancel
import com.afollestad.materialdialogs.customview.getCustomView
import com.alexvt.integrity.lib.util.ThemedActivity
import com.jakewharton.rxbinding3.widget.textChanges
import dagger.android.AndroidInjection
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.activity_tags.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject


class TagsActivity : ThemedActivity() {

    @Inject
    lateinit var vmFactory: ViewModelProvider.Factory

    private val vm by lazy {
        ViewModelProviders.of(this, vmFactory)[TagsViewModel::class.java]
    }

    private val tagEditDialog: MaterialDialog by lazy { bindTagEditDialog() }

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tags)
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setDisplayShowHomeEnabled(true)

        bindToolbar()
        bindTagList()
        bindFloatingButton()
        bindDoneButton()
        bindNavigation()
    }

    private fun bindToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setDisplayShowHomeEnabled(true)
    }

    private fun bindTagList() {
        rvTagList.adapter = TagRecyclerAdapter(ArrayList(), vm.selectMode, this,
                { vm.clickTag(it) }, { askRemoveTag(it) })
        vm.tagListData.observe(this, Observer {
            (rvTagList.adapter as TagRecyclerAdapter).setItems(it)
        })
        vm.inputStateData.observe(this, Observer {
            updateTagEditDialog(it.editingTag, it.editTagName, it.editedTagColor)
        })
    }

    private fun bindFloatingButton() {
        fab.setOnClickListener { vm.clickCreateTag() }
    }

    private fun bindDoneButton() {
        bDone.visibility = if (vm.isSelectMode()) View.VISIBLE else View.GONE
        bDone.setOnClickListener { vm.clickDone() }
    }

    private fun bindNavigation() {
        vm.navigationEventData.observe(this, androidx.lifecycle.Observer {
            if (it.goBack) {
                super.onBackPressed()
            } else if (it.returnData) {
                val returnIntent = Intent()
                IntentUtil.putSnapshot(returnIntent, it.bundledSnapshot)
                setResult(Activity.RESULT_OK, returnIntent)
                finish()
            } else {
                val errorText = when (it.inputError) {
                    InputError.EMPTY_NAME -> "Please enter tag name"
                    InputError.ALREADY_EXISTS -> "This tag already exists"
                    else -> "Error" // shouldn't happen
                }
                Toast.makeText(this, errorText, Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun bindColorButton(buttonGroup: RadioGroup, colorButton: View) {
        colorButton.setOnClickListener { buttonGroup.check(it.id) }
    }

    private fun getColorIdMap(tagEditViews: ViewTagEditBinding) = with (tagEditViews) { linkedMapOf(
            tbColor1.id to "#FFFFFF",
            tbColor2.id to "#EE8888",
            tbColor3.id to "#88EE88",
            tbColor4.id to "#8888EE"
    )}

    private fun checkColor(tagEditViews: ViewTagEditBinding, color: String) {
        // unchecking colors first
        getColorIdMap(tagEditViews).forEach {
            tagEditViews.toggleGroup.findViewById<ToggleButton>(it.key).isChecked = false
        }

        val buttonId = getColorIdMap(tagEditViews).toList()
                .firstOrNull {it.second == color}?.first
                ?: getColorIdMap(tagEditViews).toList().first().first
        tagEditViews.toggleGroup.findViewById<ToggleButton>(buttonId).isChecked = true
    }

    private fun getCheckedColor(tagEditViews: ViewTagEditBinding) = getColorIdMap(tagEditViews)
            .toList()
            .firstOrNull { tagEditViews.toggleGroup.findViewById<ToggleButton>(it.first).isChecked }?.second
            ?: getColorIdMap(tagEditViews).toList().first().second // first color

    private fun bindTagEditDialog(): MaterialDialog {
        val tagEditViews: ViewTagEditBinding = DataBindingUtil.inflate(LayoutInflater.from(this),
                R.layout.view_tag_edit, null, false)

        tagEditViews.etText.textChanges()
                .debounce(20, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { vm.updateEditTagName(it.toString()) }

        // todo color picker
        val toggles = tagEditViews.toggleGroup
        with (tagEditViews) {
            bindColorButton(toggles, tbColor1)
            bindColorButton(toggles, tbColor2)
            bindColorButton(toggles, tbColor3)
            bindColorButton(toggles, tbColor4)
            toggles.setOnCheckedChangeListener { group, checkedId ->
                for (j in 0 until group.childCount) {
                    val view = group.getChildAt(j) as ToggleButton
                    view.isChecked = (view.id == checkedId)
                    vm.updateEditColor(getCheckedColor(tagEditViews))
                }
            }
        }

        return MaterialDialog(this)
                .customView(view = tagEditViews.llTagEdit)
                .noAutoDismiss()
                .positiveButton(text = "Save") {
                    vm.clickSave()
                }
                .negativeButton(text = "Cancel") {
                    vm.closeViewTag()
                }
                .onCancel {
                    vm.closeViewTag()
                }
    }

    private fun updateTagEditDialog(show: Boolean, text: String?, color: String) {
        if (show) {
            tagEditDialog.show()
            val tagEditViews: ViewTagEditBinding = DataBindingUtil.findBinding(tagEditDialog.getCustomView()!!)!!
            if (tagEditViews.etText.text.toString() != text) {
                tagEditViews.etText.setText("")
                tagEditViews.etText.append(text)
            }
            checkColor(tagEditViews, color)
        } else {
            tagEditDialog.cancel()
        }
    }

    override fun onDestroy() {
        tagEditDialog.dismiss()
        super.onDestroy()
    }

    private fun askRemoveTag(tag: Tag) {
        MaterialDialog(this)
                .title(text = "Delete tag?")
                .positiveButton(text = "Delete") {
                    vm.removeTag(tag.text)
                }
                .negativeButton(text = "Cancel")
                .show()
    }

    override fun onSupportNavigateUp(): Boolean {
        super.onBackPressed()
        return true
    }

    override fun onBackPressed() {
        vm.pressBackButton()
    }
}
