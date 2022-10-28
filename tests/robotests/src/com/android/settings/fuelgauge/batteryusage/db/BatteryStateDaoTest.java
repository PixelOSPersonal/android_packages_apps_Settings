/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.fuelgauge.batteryusage.db;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.database.Cursor;

import androidx.test.core.app.ApplicationProvider;

import com.android.settings.testutils.BatteryTestUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.List;

/** Tests for {@link BatteryStateDao}. */
@RunWith(RobolectricTestRunner.class)
public final class BatteryStateDaoTest {
    private static final int CURSOR_COLUMN_SIZE = 19;
    private static final long TIMESTAMP1 = System.currentTimeMillis();
    private static final long TIMESTAMP2 = System.currentTimeMillis() + 2;
    private static final long TIMESTAMP3 = System.currentTimeMillis() + 4;
    private static final String PACKAGE_NAME1 = "com.android.apps.settings";
    private static final String PACKAGE_NAME2 = "com.android.apps.calendar";
    private static final String PACKAGE_NAME3 = "com.android.apps.gmail";

    private Context mContext;
    private BatteryStateDatabase mDatabase;
    private BatteryStateDao mBatteryStateDao;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        mDatabase = BatteryTestUtils.setUpBatteryStateDatabase(mContext);
        mBatteryStateDao = mDatabase.batteryStateDao();
        BatteryTestUtils.insertDataToBatteryStateDatabase(mContext, TIMESTAMP3, PACKAGE_NAME3);
        BatteryTestUtils.insertDataToBatteryStateDatabase(mContext, TIMESTAMP2, PACKAGE_NAME2);
        BatteryTestUtils.insertDataToBatteryStateDatabase(
                mContext, TIMESTAMP1, PACKAGE_NAME1, /*multiple=*/ true);
    }

    @After
    public void closeDb() {
        mDatabase.close();
        BatteryStateDatabase.setBatteryStateDatabase(/*database=*/ null);
    }

    @Test
    public void batteryStateDao_insertAll() throws Exception {
        final List<BatteryState> states = mBatteryStateDao.getAllAfter(TIMESTAMP1);
        assertThat(states).hasSize(2);
        // Verifies the queried battery states.
        assertBatteryState(states.get(0), TIMESTAMP3, PACKAGE_NAME3);
        assertBatteryState(states.get(1), TIMESTAMP2, PACKAGE_NAME2);
    }

    @Test
    public void batteryStateDao_getCursorAfter() throws Exception {
        final Cursor cursor = mBatteryStateDao.getCursorAfter(TIMESTAMP2);
        assertThat(cursor.getCount()).isEqualTo(2);
        assertThat(cursor.getColumnCount()).isEqualTo(CURSOR_COLUMN_SIZE);
        // Verifies the queried first battery state.
        cursor.moveToFirst();
        assertThat(cursor.getString(4 /*packageName*/)).isEqualTo(PACKAGE_NAME3);
        // Verifies the queried second battery state.
        cursor.moveToNext();
        assertThat(cursor.getString(4 /*packageName*/)).isEqualTo(PACKAGE_NAME2);
    }

    @Test
    public void batteryStateDao_clearAllBefore() throws Exception {
        mBatteryStateDao.clearAllBefore(TIMESTAMP2);

        final List<BatteryState> states = mBatteryStateDao.getAllAfter(0);
        assertThat(states).hasSize(1);
        // Verifies the queried battery state.
        assertBatteryState(states.get(0), TIMESTAMP3, PACKAGE_NAME3);
    }

    @Test
    public void batteryStateDao_clearAll() throws Exception {
        assertThat(mBatteryStateDao.getAllAfter(0)).hasSize(3);
        mBatteryStateDao.clearAll();
        assertThat(mBatteryStateDao.getAllAfter(0)).isEmpty();
    }

    @Test
    public void getInstance_createNewInstance() throws Exception {
        BatteryStateDatabase.setBatteryStateDatabase(/*database=*/ null);
        assertThat(BatteryStateDatabase.getInstance(mContext)).isNotNull();
    }

    @Test
    public void getDistinctTimestampCount_returnsExpectedResult() {
        assertThat(mBatteryStateDao.getDistinctTimestampCount(/*timestamp=*/ 0))
                .isEqualTo(3);
        assertThat(mBatteryStateDao.getDistinctTimestampCount(TIMESTAMP1))
                .isEqualTo(2);
    }

    @Test
    public void getDistinctTimestamps_returnsExpectedResult() {
        final List<Long> timestamps =
                mBatteryStateDao.getDistinctTimestamps(/*timestamp=*/ 0);

        assertThat(timestamps).hasSize(3);
        assertThat(timestamps).containsExactly(TIMESTAMP1, TIMESTAMP2, TIMESTAMP3);
    }

    private static void assertBatteryState(
            BatteryState state, long timestamp, String packageName) {
        assertThat(state.timestamp).isEqualTo(timestamp);
        assertThat(state.packageName).isEqualTo(packageName);
    }
}