/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.android.data

import android.content.Context
import com.alexvt.integrity.android.data.credentials.SimplePersistableCredentialsRepository
import com.alexvt.integrity.android.data.device.AndroidDeviceInfoRepository
import com.alexvt.integrity.android.data.log.RoomLogRepository
import com.alexvt.integrity.android.data.metadata.RoomMetadataRepository
import com.alexvt.integrity.android.data.search.RoomSearchIndexRepository
import com.alexvt.integrity.android.data.settings.SimplePersistableSettingsRepository
import com.alexvt.integrity.android.data.types.AndroidDataTypeRepository
import com.alexvt.integrity.core.data.credentials.CredentialsRepository
import com.alexvt.integrity.core.data.device.DeviceInfoRepository
import com.alexvt.integrity.core.data.log.LogRepository
import com.alexvt.integrity.core.data.metadata.MetadataRepository
import com.alexvt.integrity.core.data.search.SearchIndexRepository
import com.alexvt.integrity.core.data.settings.SettingsRepository
import com.alexvt.integrity.core.data.types.DataTypeRepository
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class DataDependenciesModule {
    @Provides
    @Singleton
    fun provideMetadataRepository(context: Context): MetadataRepository
            = RoomMetadataRepository(context)

    @Provides
    @Singleton
    fun provideCredentialsRepository(context: Context): CredentialsRepository
            = SimplePersistableCredentialsRepository(context)

    @Provides
    @Singleton
    fun provideSearchIndexRepository(context: Context): SearchIndexRepository
            = RoomSearchIndexRepository(context)

    @Provides
    @Singleton
    fun provideSettingsRepository(context: Context): SettingsRepository
            = SimplePersistableSettingsRepository(context)

    @Provides
    @Singleton
    fun provideLogRepository(context: Context): LogRepository
            = RoomLogRepository(context)

    @Provides
    @Singleton
    fun provideDataTypeRepository(context: Context): DataTypeRepository
            = AndroidDataTypeRepository(context)

    @Provides
    @Singleton
    fun provideDeviceInfoRepository(context: Context): DeviceInfoRepository
            = AndroidDeviceInfoRepository(context)
}