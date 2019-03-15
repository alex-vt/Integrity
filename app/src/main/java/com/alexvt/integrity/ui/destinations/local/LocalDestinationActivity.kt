/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.ui.destinations.local

import android.os.Bundle
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import com.alexvt.integrity.R
import com.alexvt.integrity.lib.util.ThemedActivity
import com.jakewharton.rxbinding3.widget.textChanges
import dagger.android.AndroidInjection
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.activity_local_location.*
import javax.inject.Inject


class LocalDestinationActivity : ThemedActivity() {

    @Inject
    lateinit var vmFactory: ViewModelProvider.Factory

    private val vm by lazy {
        ViewModelProviders.of(this, vmFactory)[LocalDestinationViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_local_location)

        bindToolbar()
        bindContent()
        bindNavigation()
    }

    private fun bindToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setDisplayShowHomeEnabled(true)
    }

    private fun bindContent() {
        etTitle.isEnabled = !vm.isEditMode()
        etTitle.append(vm.inputStateData.value!!.title)
        etTitle.textChanges()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { vm.onNewTitle(it.toString()) }

        etPath.append(vm.inputStateData.value!!.path)
        etPath.textChanges()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { vm.onNewPath(it.toString()) }

        bSave.setOnClickListener { vm.clickSave() }
    }

    private fun bindNavigation() {
        vm.navigationEventData.observe(this, androidx.lifecycle.Observer {
            if (it.goBack) {
                super.onBackPressed()
            } else {
                val errorText = when (it.inputError) {
                    InputError.EMPTY_NAME -> "Please enter title"
                    InputError.EMPTY_PATH -> "Please enter path"
                    InputError.ALREADY_EXISTS -> "Destination with this title already exists"
                    else -> "Error" // shouldn't happen
                }
                Toast.makeText(this, errorText, Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onSupportNavigateUp(): Boolean {
        vm.pressBackButton()
        return true
    }

    override fun onBackPressed() = vm.pressBackButton()
}
