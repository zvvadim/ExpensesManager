/*
 * Copyright (C) 2010 The Android Open Source Project
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

package ua.kiev.bpo.expensesmanager.calendar;

import android.app.LoaderManager;
import android.content.ContentUris;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.drawable.StateListDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

import ua.kiev.bpo.expensesmanager.DayRecord;
import ua.kiev.bpo.expensesmanager.DayRecordManager;
import ua.kiev.bpo.expensesmanager.DayRecordsProvider;
import ua.kiev.bpo.expensesmanager.R;

public class MonthByWeekFragment extends SimpleDayPickerFragment implements LoaderManager
        .LoaderCallbacks<Cursor>, OnTouchListener {

    private static final String TAG = "MonthFragment";
    private static final String TAG_EVENT_DIALOG = "event_dialog";

    private static final String WHERE_CALENDARS_VISIBLE = CalendarContract.Calendars.VISIBLE + "=1";
    protected static boolean mShowDetailsInMonth = false;

    protected int mFirstLoadedJulianDay;
    protected int mLastLoadedJulianDay;

    protected boolean mIsMiniMonth;

    private static final int WEEKS_BUFFER = 1;
    // How long to wait after scroll stops before starting the loader
    // Using scroll duration because scroll state changes don't update
    // correctly when a scroll is triggered programmatically.
    private static final int LOADER_DELAY = 200;
    // The minimum time between requeries of the data if the db is
    // changing
    private static final int LOADER_THROTTLE_DELAY = 500;

    private final Time mDesiredDay = new Time();
    private Uri mDayRecordUri;

    private volatile boolean mShouldLoad = true;
    private boolean mIsDetached;

    private CursorLoader mLoader;

    private DayRecordManager mDayRecordManager;

    private final Runnable mTZUpdater = new Runnable() {
        @Override
        public void run() {
            String tz = Utils.getTimeZone(mContext, mTZUpdater);
            mSelectedDay.timezone = tz;
            mSelectedDay.normalize(true);
            mTempTime.timezone = tz;
            mFirstDayOfMonth.timezone = tz;
            mFirstDayOfMonth.normalize(true);
            mFirstVisibleDay.timezone = tz;
            mFirstVisibleDay.normalize(true);
            if (mAdapter != null) {
                mAdapter.refresh();
            }
        }
    };

    private final Runnable mUpdateLoader = new Runnable() {
        @Override
        public void run() {
            synchronized (this) {
                if (!mShouldLoad || mLoader == null) {
                    return;
                }
                // Stop any previous loads while we update the uri
                stopLoader();

                // Start the loader again
                mDayRecordUri = updateUri();

                mLoader.setUri(mDayRecordUri);
                mLoader.startLoading();
                mLoader.onContentChanged();
                if (Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG, "Started loader with uri: " + mDayRecordUri);
                }
            }
        }
    };
    // Used to load the events when a delay is needed
    Runnable mLoadingRunnable = new Runnable() {
        @Override
        public void run() {
            if (!mIsDetached) {
                mLoader = (CursorLoader) getLoaderManager().initLoader(0, null,
                        MonthByWeekFragment.this);
            }
        }
    };


    private void stopLoader() {
        synchronized (mUpdateLoader) {
            mHandler.removeCallbacks(mUpdateLoader);
            if (mLoader != null) {
                mLoader.stopLoading();
                if (Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG, "Stopped loader from loading");
                }
            }
        }
    }

    public MonthByWeekFragment() {
        this(System.currentTimeMillis(), true);
    }

    public MonthByWeekFragment(long initialTime, boolean isMiniMonth) {
        super(initialTime);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View v;
        v = inflater.inflate(R.layout.month_by_week, container, false);
        //v = inflater.inflate(R.layout.full_month_by_week, container, false);
        mDayNamesHeader = (ViewGroup) v.findViewById(R.id.day_names);

        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mIsMiniMonth = true;

        mListView.setSelector(new StateListDrawable());
        mListView.setOnTouchListener(this);

        mDayRecordManager = DayRecordManager.get(getActivity().getApplicationContext());

        mLoader = (CursorLoader) getLoaderManager().initLoader(0, null, this);

    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        mDesiredDay.setToNow();
        return false;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        CursorLoader loader;
        synchronized (mUpdateLoader){

            mFirstLoadedJulianDay =
                    Time.getJulianDay(mSelectedDay.toMillis(true), mSelectedDay.gmtoff)
                            - (mNumWeeks * 7 / 2);
            mDayRecordUri = updateUri();

            return mDayRecordManager.queryDayRecors(mDayRecordUri);
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        synchronized (mUpdateLoader) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Found " + data.getCount() + " cursor entries for uri " + mDayRecordUri);
            }
            CursorLoader cLoader = (CursorLoader) loader;
           /* if (mEventUri == null) {
                mEventUri = cLoader.getUri();
                updateLoadedDays();
            }
            if (cLoader.getUri().compareTo(mEventUri) != 0) {
                // We've started a new query since this loader ran so ignore the
                // result
                return;
            }*/

            ArrayList<DayRecord> dayRecords = new ArrayList<DayRecord>();
            DayRecord.buildDayRecordsFromCursor(
                    dayRecords, data, mContext, mFirstLoadedJulianDay, mLastLoadedJulianDay);
            ((MonthByWeekAdapter) mAdapter).setDayRecords(mFirstLoadedJulianDay,
                    mLastLoadedJulianDay - mFirstLoadedJulianDay + 1, dayRecords);



        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }
    protected String updateWhere() {
        // TODO fix selection/selection args after b/3206641 is fixed
        String where = WHERE_CALENDARS_VISIBLE;
        if (mHideDeclined || !mShowDetailsInMonth) {
            where += " AND " + CalendarContract.Instances.SELF_ATTENDEE_STATUS + "!="
                    + CalendarContract.Attendees.ATTENDEE_STATUS_DECLINED;
        }
        return where;
    }

    private void updateLoadedDays() {
        List<String> pathSegments = mDayRecordUri.getPathSegments();
        int size = pathSegments.size();
        if (size <= 2) {
            return;
        }
        long first = Long.parseLong(pathSegments.get(size - 2));
        long last = Long.parseLong(pathSegments.get(size - 1));
        mTempTime.set(first);
        mFirstLoadedJulianDay = Time.getJulianDay(first, mTempTime.gmtoff);
        mTempTime.set(last);
        mLastLoadedJulianDay = Time.getJulianDay(last, mTempTime.gmtoff);
    }


    protected void updateHeader() {
        TextView label = (TextView) mDayNamesHeader.findViewById(R.id.wk_label);
        if (mShowWeekNumber) {
            label.setVisibility(View.VISIBLE);
        } else {
            label.setVisibility(View.GONE);
        }
        int offset = mFirstDayOfWeek - 1;
        for (int i = 1; i < 8; i++) {
            label = (TextView) mDayNamesHeader.getChildAt(i);
            if (i < mDaysPerWeek + 1) {
                int position = (offset + i) % 7;
                label.setText(mDayLabels[position]);
                label.setVisibility(View.VISIBLE);
                if (position == Time.SATURDAY) {
                    label.setTextColor(mSaturdayColor);
                } else if (position == Time.SUNDAY) {
                    label.setTextColor(mSundayColor);
                } else {
                    label.setTextColor(mDayNameColor);
                }
            } else {
                label.setVisibility(View.GONE);
            }
        }
        mDayNamesHeader.invalidate();
    }

    private Uri updateUri() {
        SimpleWeekView child = (SimpleWeekView) mListView.getChildAt(0);
        if (child != null) {
            int julianDay = child.getFirstJulianDay();
            mFirstLoadedJulianDay = julianDay;
        }
        // -1 to ensure we get all day events from any time zone
        mTempTime.setJulianDay(mFirstLoadedJulianDay - 1);
        long start = mTempTime.toMillis(true);
        mLastLoadedJulianDay = mFirstLoadedJulianDay + (mNumWeeks + 2 * WEEKS_BUFFER) * 7;
        // +1 to ensure we get all day events from any time zone
        mTempTime.setJulianDay(mLastLoadedJulianDay + 1);
        long end = mTempTime.toMillis(true);

        Uri.Builder builder = DayRecordsProvider.CONTENT_URI.buildUpon();
        //Uri.Builder builder = CalendarContract.Instances.CONTENT_URI.buildUpon();
        ContentUris.appendId(builder, start);
        ContentUris.appendId(builder, end);
        return builder.build();
    }

    @Override
    protected void setUpAdapter() {
        mFirstDayOfWeek = Utils.getFirstDayOfWeek(mContext);
        mShowWeekNumber = Utils.getShowWeekNumber(mContext);

        HashMap<String, Integer> weekParams = new HashMap<String, Integer>();
        weekParams.put(SimpleWeeksAdapter.WEEK_PARAMS_NUM_WEEKS, mNumWeeks);
        weekParams.put(SimpleWeeksAdapter.WEEK_PARAMS_SHOW_WEEK, mShowWeekNumber ? 1 : 0);
        weekParams.put(SimpleWeeksAdapter.WEEK_PARAMS_WEEK_START, mFirstDayOfWeek);
        weekParams.put(MonthByWeekAdapter.WEEK_PARAMS_IS_MINI, mIsMiniMonth ? 1 : 0);
        weekParams.put(SimpleWeeksAdapter.WEEK_PARAMS_JULIAN_DAY,
                Time.getJulianDay(mSelectedDay.toMillis(true), mSelectedDay.gmtoff));
        weekParams.put(SimpleWeeksAdapter.WEEK_PARAMS_DAYS_PER_WEEK, mDaysPerWeek);
        if (mAdapter == null) {
            mAdapter = new MonthByWeekAdapter(getActivity(), weekParams);
        } else {
            mAdapter.updateParams(weekParams);
        }
        mAdapter.notifyDataSetChanged();
    }

    @Override
    protected void setUpHeader() {
        if (mIsMiniMonth) {
            super.setUpHeader();
            return;
        }

        mDayLabels = new String[7];
        for (int i = Calendar.SUNDAY; i <= Calendar.SATURDAY; i++) {
            mDayLabels[i - Calendar.SUNDAY] = DateUtils.getDayOfWeekString(i,
                    DateUtils.LENGTH_MEDIUM).toUpperCase();
        }
    }

    @Override
    public void onDetach() {
        mIsDetached = true;
        super.onDetach();
    }
}
