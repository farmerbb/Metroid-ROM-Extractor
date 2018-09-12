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

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.io.RandomAccessFile
import java.math.BigInteger
import java.security.DigestInputStream
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

class Extractor(private val inFile: File, private val outFile: File) {

    private val inBytes = ByteArray(0x20000)
    private val outBytes = ByteArray(0x20010)

    private external fun decryptMetroidNative(inArray: ByteArray): ByteArray

    init {
        System.loadLibrary("decryptmetroid")
    }

    internal fun importFile(input: InputStream): Boolean {
        try {
            val output = FileOutputStream(inFile)

            if(input.available() != 181020)
                return false

            val data = ByteArray(input.available())
            input.read(data)
            output.write(data)
            input.close()
            output.close()
        } catch (e: IOException) {
            return false
        }

        return validateInputFile() && createRom()
    }

    internal fun exportFile(output: OutputStream): Boolean {
        return try {
            output.write(outBytes)
            output.close()

            true
        } catch (e: IOException) {
            false
        }
    }

    private fun validateInputFile(): Boolean {
        val md5s = arrayOf(
                "0c669a58dc2bd79f5c62acfcfd3ccce8",
                "2a7cc6ff20ff47d3d71721c8f7704593"
        )

        val fileMd5 = generateMd5(inFile)
        for(md5 in md5s) {
            if(md5 == fileMd5)
                return true
        }

        return false
    }

    private fun generateMd5(file: File): String {
        val digest: MessageDigest
        try {
            digest = MessageDigest.getInstance("MD5")
        } catch (e: NoSuchAlgorithmException) {
            return ""
        }

        val buffer = ByteArray(8192)
        try {
            val dis = DigestInputStream(FileInputStream(file), digest)

            while(true) {
                if(dis.read(buffer) == -1) {
                    dis.close()
                    break
                }
            }
        } catch (e: IOException) {
            return ""
        }

        var md5 = BigInteger(1, digest.digest()).toString(16)
        while(md5.length < 32) {
            md5 = "0$md5"
        }

        return md5
    }

    private fun createRom(): Boolean {
        try {
            val file = RandomAccessFile(inFile, "r")
            file.seek(0xa3f8)
            file.readFully(inBytes)
        } catch (e: IOException) {
            return false
        }

        val decryptedBytes = decryptMetroidNative(inBytes)
        val iNesHeader = byteArrayOf(
                0x4e, 0x45, 0x53, 0x1a, 0x08, 0x00, 0x11, 0x00,
                0x00, 0x00, 0x4e, 0x49, 0x20, 0x31, 0x2e, 0x33
        )

        System.arraycopy(iNesHeader, 0, outBytes, 0, 0x10)
        System.arraycopy(decryptedBytes, 0, outBytes, 0x10, 0x20000)

        try {
            val output = FileOutputStream(outFile)
            output.write(outBytes)
            output.close()
        } catch (e: IOException) {
            return false
        }

        return generateMd5(outFile) == "d7da4a907be0012abca6625471ef2c9c"
    }
}