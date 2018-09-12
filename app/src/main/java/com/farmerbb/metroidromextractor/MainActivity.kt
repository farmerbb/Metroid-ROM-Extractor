/*
 * Copyright (C) 2018 Braden Farmer
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

package com.farmerbb.metroidromextractor

import android.content.ActivityNotFoundException
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.text.method.LinkMovementMethod
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_main.*

import java.io.File

class MainActivity: AppCompatActivity() {

    companion object {
        const val IMPORT = 123
        const val EXPORT = 456
    }

    private var importSuccessful: Boolean? = null
    private var exportSuccessful: Boolean? = null

    private val extractor: Extractor by lazy {
        Extractor(
                File(filesDir, "NESemu.rel"),
                File(filesDir, "Metroid.nes")
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        select.setOnClickListener { openInFilePicker() }
        save.setOnClickListener { openOutFilePicker() }
        save.isEnabled = false

        savedInstanceState?.let {
            if(it.containsKey("import_successful"))
                importCompleted(it.getBoolean("import_successful"))

            if(it.containsKey("export_successful"))
                exportCompleted(it.getBoolean("export_successful"))
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        if(resultCode != RESULT_OK)
            return

        resultData?.data?.let {
            when(requestCode) {
                IMPORT -> {
                    importCompleted(extractor.importFile(contentResolver.openInputStream(it)!!))
                }

                EXPORT -> {
                    exportCompleted(extractor.exportFile(contentResolver.openOutputStream(it)!!))
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        importSuccessful?.let { outState?.putBoolean("import_successful", it) }
        exportSuccessful?.let { outState?.putBoolean("export_successful", it) }

        super.onSaveInstanceState(outState)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.add(Menu.NONE, 123, Menu.NONE, getString(R.string.about))
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        val textView = TextView(this).apply {
            setText(R.string.about_dialog_text)
            setPadding(R.dimen.about_dialog_padding)
            movementMethod = LinkMovementMethod.getInstance()
        }

        AlertDialog.Builder(this)
                .setTitle(R.string.about)
                .setView(textView)
                .setPositiveButton(R.string.close, null)
                .create()
                .show()

        return true
    }

    private fun openInFilePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false)
            type = "*/*"
        }

        try {
            startActivityForResult(intent, IMPORT)
        } catch (e: ActivityNotFoundException) { /* Gracefully fail */ }
    }

    private fun openOutFilePicker() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            putExtra(Intent.EXTRA_TITLE, "Metroid.nes")
            type = "*/*"
        }

        try {
            startActivityForResult(intent, EXPORT)
        } catch (e: ActivityNotFoundException) { /* Gracefully fail */ }
    }

    private fun importCompleted(success: Boolean) {
        if(success)
            select.drawableEnd(R.drawable.success)
        else
            select.drawableEnd(R.drawable.failure)

        save.isEnabled = success
        importSuccessful = success
    }

    private fun exportCompleted(success: Boolean) {
        if(success)
            save.drawableEnd(R.drawable.success)
        else
            save.drawableEnd(R.drawable.failure)

        exportSuccessful = success
    }

    fun TextView.drawableEnd(id: Int)
            = setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, id, 0)

    fun TextView.setPadding(id: Int) {
        val size = resources.getDimensionPixelSize(id)
        setPadding(size, size, size, size)
    }
}
