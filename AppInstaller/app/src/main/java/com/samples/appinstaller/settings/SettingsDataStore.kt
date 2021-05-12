package com.samples.appinstaller.settings

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.google.protobuf.InvalidProtocolBufferException
import com.samples.appinstaller.AppSettings
import java.io.InputStream
import java.io.OutputStream

const val PROTO_STORE_FILE_NAME = "app_settings.pb"

object AppSettingsSerializer : Serializer<AppSettings> {

    override val defaultValue: AppSettings = AppSettings.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): AppSettings {
        try {
            return AppSettings.parseFrom(input)
        } catch (ipbe: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read proto.", ipbe)
        }
    }

    override suspend fun writeTo(t: AppSettings, output: OutputStream) = t.writeTo(output)
}