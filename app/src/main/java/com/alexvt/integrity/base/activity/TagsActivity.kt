/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.base.activity

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
import com.alexvt.integrity.base.adapter.TagRecyclerAdapter
import com.alexvt.integrity.core.IntegrityCore
import com.alexvt.integrity.databinding.ViewTagEditBinding
import com.alexvt.integrity.lib.Tag
import com.alexvt.integrity.lib.util.IntentUtil
import kotlinx.android.synthetic.main.activity_tags.*
import android.widget.RadioGroup
import com.alexvt.integrity.core.util.ThemedActivity


class TagsActivity : ThemedActivity() {

    var selectedTags: List<Tag> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tags)
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setDisplayShowHomeEnabled(true)

        if (IntentUtil.getSnapshot(intent) != null) {
            selectedTags = IntentUtil.getSnapshot(intent)!!.tags
        }

        fab.setOnClickListener { editTag(Tag(text = "", color = "#FFFFFF")) }

        rvTagList.adapter = TagRecyclerAdapter(ArrayList(), this)

        bDone.visibility = if (isSelectMode()) View.VISIBLE else View.GONE
        bDone.setOnClickListener { returnSelection() }
    }

    override fun onStart() {
        super.onStart()
        refreshTagList() // no other sources of data, no need to listen to changes
    }


    private fun refreshTagList() {
        (rvTagList.adapter as TagRecyclerAdapter).setItems(getItemSelection())
    }

    private fun getItemSelection()
            = IntegrityCore.settingsRepository.getAllTags().map {
                Pair(it, selectedTags.contains(it))
            }

    fun editTag(tagToEdit: Tag, oldTagText: String? = null) {
        val tagEditViews: ViewTagEditBinding = DataBindingUtil.inflate(LayoutInflater.from(this),
                R.layout.view_tag_edit, null, false)
        tagEditViews.etText.setText(tagToEdit.text)
        // todo color picker
        tagEditViews.tbColor1.setOnClickListener { (it.parent as RadioGroup).check(it.id) }
        tagEditViews.tbColor2.setOnClickListener { (it.parent as RadioGroup).check(it.id) }
        tagEditViews.tbColor3.setOnClickListener { (it.parent as RadioGroup).check(it.id) }
        tagEditViews.tbColor4.setOnClickListener { (it.parent as RadioGroup).check(it.id) }
        tagEditViews.toggleGroup.setOnCheckedChangeListener { group, checkedId ->
            for (j in 0 until group.childCount) {
                val view = group.getChildAt(j) as ToggleButton
                view.isChecked = (view.id == checkedId)
            }
        }
        when {
            tagToEdit.color == "#FFFFFF" -> tagEditViews.tbColor1.isChecked = true
            tagToEdit.color == "#EE8888" -> tagEditViews.tbColor2.isChecked = true
            tagToEdit.color == "#88EE88" -> tagEditViews.tbColor3.isChecked = true
            tagToEdit.color == "#8888EE" -> tagEditViews.tbColor4.isChecked = true
        }
        MaterialDialog(this)
                .customView(view = tagEditViews.llTagEdit)
                .positiveButton(text = "Save") {
                    val color = when {
                        tagEditViews.tbColor2.isChecked -> "#EE8888"
                        tagEditViews.tbColor3.isChecked -> "#88EE88"
                        tagEditViews.tbColor4.isChecked -> "#8888EE"
                        else -> "#FFFFFF" // color 1
                    }
                    val tagFromEditor = Tag(tagEditViews.etText.text.toString().trim(), color)
                    checkAndAddTag(tagFromEditor, oldTagText)
                }
                .negativeButton(text = "Cancel")
                .show()
    }

    private fun checkAndAddTag(tag: Tag, oldText: String?) {
        if (tag.text.isBlank()) {
            Toast.makeText(this, "Please enter tag name", Toast.LENGTH_SHORT).show()
            return
        }
        // tag must not have name like any existing tag except itself before the change
        if (IntegrityCore.settingsRepository.getAllTags()
                        .map { it.text }
                        .minus(oldText)
                        .contains(tag.text)) {
            Toast.makeText(this, "This tag already exists", Toast.LENGTH_SHORT).show()
            return
        }
        if (oldText != null) {
            // old tag needs to be replaced
            IntegrityCore.settingsRepository.removeTag(this, oldText)
        }
        IntegrityCore.settingsRepository.addTag(this, tag)
        refreshTagList()
    }

    fun toggleSelection(tag: Tag) {
        if (selectedTags.contains(tag)) {
            selectedTags = selectedTags.minus(tag)
        } else {
            selectedTags = selectedTags.plus(tag)
        }
        refreshTagList()
    }

    fun askRemoveTag(tag: Tag) {
        MaterialDialog(this)
                .title(text = "Delete tag?")
                .positiveButton(text = "Delete") {
                    dialog ->
                    IntegrityCore.settingsRepository.removeTag(this, tag.text)
                    refreshTagList()
                }
                .negativeButton(text = "Cancel")
                .show()
    }

    fun isSelectMode() = IntentUtil.isSelectMode(intent)

    private fun returnSelection() {
        val returnIntent = Intent()
        if (IntentUtil.getSnapshot(intent) != null) {
            val snapshot = IntentUtil.getSnapshot(intent)!!.copy(
                    tags = ArrayList(selectedTags)
            )
            IntentUtil.putSnapshot(returnIntent, snapshot)
        }
        setResult(Activity.RESULT_OK, returnIntent)
        finish()
    }

    override fun onSupportNavigateUp(): Boolean {
        super.onBackPressed()
        return true
    }
}
