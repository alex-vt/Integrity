/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.android.ui.destinations.samba

import android.os.Bundle
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import com.alexvt.integrity.R
import com.alexvt.integrity.lib.android.util.ThemedActivity
import com.jakewharton.rxbinding3.widget.textChanges
import dagger.android.AndroidInjection
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.activity_samba_location.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject


class SambaDestinationActivity : ThemedActivity() {

    @Inject
    lateinit var vmFactory: ViewModelProvider.Factory

    private val vm by lazy {
        ViewModelProviders.of(this, vmFactory)[SambaDestinationViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_samba_location)

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
                .debounce(20, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { vm.onNewTitle(it.toString()) }

        etPath.append(vm.inputStateData.value!!.path)
        etPath.textChanges()
                .debounce(20, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { vm.onNewPath(it.toString()) }

        vm.userNameLoadedEventData.observe(this, Observer {
            etUser.setText("")
            etUser.append(vm.inputStateData.value!!.user)
        })
        etUser.textChanges()
                .debounce(20, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { vm.onNewUser(it.toString()) }

        etPassword.append(vm.inputStateData.value!!.password)
        etPassword.textChanges()
                .debounce(20, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { vm.onNewPassword(it.toString()) }

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
                    InputError.EMPTY_USER -> "Please enter user name"
                    InputError.EMPTY_PASSWORD -> "Please enter password"
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
