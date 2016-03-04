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

import android.app.Activity;
import android.app.ListFragment;
import android.content.Context;
import android.content.CursorLoader;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.Log;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ListView;
import android.widget.TextView;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;

import ua.kiev.bpo.expensesmanager.R;

/**
 * <p>
 * This displays a titled list of weeks with selectable days. It can be
 * configured to display the week number, start the week on a given day, show a
 * reduced number of days, or display an arbitrary number of weeks at a time. By
 * overriding methods and changing variables this fragment can be customized to
 * easily display a month selection component in a given style.
 * </p>
 */
public class SimpleDayPickerFragment extends ListFragment implements OnScrollListener {

    private static final String TAG = "MonthFragment";
    private static final String KEY_CURRENT_TIME = "current_time";

    // Affects when the month selection will change while scrolling up
    protected static final int SCROLL_HYST_WEEKS = 2;
    // How long the GoTo fling animation should last
    protected static final int GOTO_SCROLL_DURATION = 500;
    // How long to wait after receiving an onScrollStateChanged notification
    // before acting on it
    protected static final int SCROLL_CHANGE_DELAY = 40;
    // The number of days to display in each week

    public static final int DAYS_PER_WEEK = 7;

    public static int LIST_TOP_OFFSET = -1;  // so that the top line will be under the separator
    protected int WEEK_MIN_VISIBLE_HEIGHT = 12;
    protected int BOTTOM_BUFFER = 20;

    protected int mSaturdayColor = 0;
    protected int mSundayColor = 0;
    protected int mDayNameColor = 0;

    // You can override these numbers to get a different appearance
    protected int mNumWeeks = 6;
    protected boolean mShowWeekNumber = false;
    protected int mDaysPerWeek = 7;

    // These affect the scroll speed and feel
    protected float mFriction = 1.0f;

    protected Context mContext;
    protected Handler mHandler;

    private static float mScale = 0;
    // When the week starts; numbered like Time.<WEEKDAY> (e.g. SUNDAY=0).
    protected int mFirstDayOfWeek;
    // The first day of the focus month
    protected Time mFirstDayOfMonth = new Time();
    // The first day that is visible in the view
    protected Time mFirstVisibleDay = new Time();
    // The name of the month to display
    protected TextView mMonthName;
    // The last name announced by accessibility
    protected CharSequence mPrevMonthName;

    protected float mMinimumFlingVelocity;

    // disposable variable used for time calculations
    protected Time mTempTime = new Time();

    // highlighted time
    protected Time mSelectedDay = new Time();
    protected SimpleWeeksAdapter mAdapter;
    protected ListView mListView;
    protected ViewGroup mDayNamesHeader;
    protected String[] mDayLabels;

    private CursorLoader mLoader;

    // which month should be displayed/highlighted [0-11]
    protected int mCurrentMonthDisplayed;
    // used for tracking during a scroll
    protected long mPreviousScrollPosition;
    // used for tracking which direction the view is scrolling
    protected boolean mIsScrollingUp = false;
    // used for tracking what state listview is in
    protected int mPreviousScrollState = OnScrollListener.SCROLL_STATE_IDLE;
    protected int mCurrentScrollState = OnScrollListener.SCROLL_STATE_IDLE;

    protected boolean mHideDeclined;

    // This causes an update of the view at midnight
    protected Runnable mTodayUpdater = new Runnable() {
        @Override
        public void run() {
            Time midnight = new Time(mFirstVisibleDay.timezone);
            midnight.setToNow();
            long currentMillis = midnight.toMillis(true);

            midnight.hour = 0;
            midnight.minute = 0;
            midnight.second = 0;
            midnight.monthDay++;
            long millisToMidnight = midnight.normalize(true) - currentMillis;
            mHandler.postDelayed(this, millisToMidnight);

            if (mAdapter != null) {
                mAdapter.notifyDataSetChanged();
            }
        }
    };

    public SimpleDayPickerFragment(long initialTime) {
        goTo(initialTime, false, true, true);
        mHandler = new Handler();
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null && savedInstanceState.containsKey(KEY_CURRENT_TIME)) {
            goTo(savedInstanceState.getLong(KEY_CURRENT_TIME), false, true, true);
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setUpListView();
        setUpHeader();

        mMonthName = (TextView) getView().findViewById(R.id.month_name);
        SimpleWeekView child = (SimpleWeekView) mListView.getChildAt(0);
        if (child == null) {
            return;
        }
        int julianDay = child.getFirstJulianDay();
        mFirstVisibleDay.setJulianDay(julianDay);
        // set the title to the month of the second week
        mTempTime.setJulianDay(julianDay + DAYS_PER_WEEK);
        setMonthDisplayed(mTempTime, true);
    }

    /**
     * Sets up the strings to be used by the header. Override this method to use
     * different strings or modify the view params.
     */
    protected void setUpHeader() {
        mDayLabels = new String[7];
        for (int i = Calendar.SUNDAY; i <= Calendar.SATURDAY; i++) {
            mDayLabels[i - Calendar.SUNDAY] = DateUtils.getDayOfWeekString(i,
                    DateUtils.LENGTH_SHORTEST).toUpperCase();
        }
    }


    protected void setUpListView() {
        // Configure the listview
        mListView = getListView();
        // Transparent background on scroll
        mListView.setCacheColorHint(0);
        // No dividers
        mListView.setDivider(null);
        // Items are clickable
        mListView.setItemsCanFocus(true);
        // The thumb gets in the way, so disable it
        mListView.setFastScrollEnabled(false);
        mListView.setVerticalScrollBarEnabled(false);
        mListView.setOnScrollListener(this);
        mListView.setFadingEdgeLength(0);
        // Make the scrolling behavior nicer
        mListView.setFriction(ViewConfiguration.getScrollFriction() * mFriction);
    }

    @Override
    public void onResume() {
        super.onResume();
        setUpAdapter();
        doResumeUpdates();
    }
    protected void doResumeUpdates() {
        // Get default week start based on locale, subtracting one for use with android Time.
        Calendar cal = Calendar.getInstance(Locale.getDefault());
        mFirstDayOfWeek = cal.getFirstDayOfWeek() - 1;

        mShowWeekNumber = false;

        updateHeader();
        goTo(mSelectedDay.toMillis(true), false, false, false);
        mAdapter.setSelectedDay(mSelectedDay);
        mTodayUpdater.run();
    }

    @Override
    public void onPause() {
        super.onPause();
        mHandler.removeCallbacks(mTodayUpdater);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putLong(KEY_CURRENT_TIME, mSelectedDay.toMillis(true));
    }


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mContext = activity;
        String tz = Time.getCurrentTimezone();
        ViewConfiguration viewConfig = ViewConfiguration.get(activity);
        mMinimumFlingVelocity = viewConfig.getScaledMinimumFlingVelocity();

        // Ensure we're in the correct time zone
        mSelectedDay.switchTimezone(tz);
        mSelectedDay.normalize(true);
        mFirstDayOfMonth.timezone = tz;
        mFirstDayOfMonth.normalize(true);
        mFirstVisibleDay.timezone = tz;
        mFirstVisibleDay.normalize(true);
        mTempTime.timezone = tz;

        Resources res = activity.getResources();
        mSaturdayColor = res.getColor(R.color.month_saturday);
        mSundayColor = res.getColor(R.color.month_sunday);
        mDayNameColor = res.getColor(R.color.month_day_names_color);

        // Adjust sizes for screen density
        if (mScale == 0) {
            mScale = activity.getResources().getDisplayMetrics().density;
            if (mScale != 1) {
                WEEK_MIN_VISIBLE_HEIGHT *= mScale;
                BOTTOM_BUFFER *= mScale;
                LIST_TOP_OFFSET *= mScale;
            }
        }
        setUpAdapter();
        setListAdapter(mAdapter);
    }

    protected void setUpAdapter() {
        HashMap<String, Integer> weekParams = new HashMap<String, Integer>();
        weekParams.put(SimpleWeeksAdapter.WEEK_PARAMS_NUM_WEEKS, mNumWeeks);
        weekParams.put(SimpleWeeksAdapter.WEEK_PARAMS_SHOW_WEEK, mShowWeekNumber ? 1 : 0);
        weekParams.put(SimpleWeeksAdapter.WEEK_PARAMS_WEEK_START, mFirstDayOfWeek);
        weekParams.put(SimpleWeeksAdapter.WEEK_PARAMS_JULIAN_DAY,
                Time.getJulianDay(mSelectedDay.toMillis(false), mSelectedDay.gmtoff));
        if (mAdapter == null) {
            mAdapter = new SimpleWeeksAdapter(getActivity(), weekParams);
            //mAdapter.registerDataSetObserver(mObserver);
        } else {
            mAdapter.updateParams(weekParams);
        }
        // refresh the view with the new parameters
        mAdapter.notifyDataSetChanged();
    }

    public boolean goTo(long time, boolean animate, boolean setSelected, boolean forceScroll) {
        if (time == -1) {
            Log.e(TAG, "time is invalid");
            return false;
        }

        // Set the selected day
        if (setSelected) {
            mSelectedDay.set(time);
            mSelectedDay.normalize(true);
        }

        // If this view isn't returned yet we won't be able to load the lists
        // current position, so return after setting the selected day.
        if (!isResumed()) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "We're not visible yet");
            }
            return false;
        }

        mTempTime.set(time);
        long millis = mTempTime.normalize(true);
        // Get the week we're going to
        // TODO push Util function into Calendar public api.
        int position = Utils.getWeeksSinceEpochFromJulianDay(
                Time.getJulianDay(millis, mTempTime.gmtoff), mFirstDayOfWeek);

        View child;
        int i = 0;
        int top = 0;
        // Find a child that's completely in the view
        do {
            child = mListView.getChildAt(i++);
            if (child == null) {
                break;
            }
            top = child.getTop();
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "child at " + (i-1) + " has top " + top);
            }
        } while (top < 0);

        // Compute the first and last position visible
        int firstPosition;
        if (child != null) {
            firstPosition = mListView.getPositionForView(child);
        } else {
            firstPosition = 0;
        }
        int lastPosition = firstPosition + mNumWeeks - 1;
        if (top > BOTTOM_BUFFER) {
            lastPosition--;
        }

        if (setSelected) {
            mAdapter.setSelectedDay(mSelectedDay);
        }

        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "GoTo position " + position);
        }
        // Check if the selected day is now outside of our visible range
        // and if so scroll to the month that contains it
        if (position < firstPosition || position > lastPosition || forceScroll) {
            mFirstDayOfMonth.set(mTempTime);
            mFirstDayOfMonth.monthDay = 1;
            millis = mFirstDayOfMonth.normalize(true);
            setMonthDisplayed(mFirstDayOfMonth, true);
            position = Utils.getWeeksSinceEpochFromJulianDay(
                    Time.getJulianDay(millis, mFirstDayOfMonth.gmtoff), mFirstDayOfWeek);

            mPreviousScrollState = OnScrollListener.SCROLL_STATE_FLING;
            if (animate) {
                mListView.smoothScrollToPositionFromTop(
                        position, LIST_TOP_OFFSET, GOTO_SCROLL_DURATION);
                return true;
            } else {
                mListView.setSelectionFromTop(position, LIST_TOP_OFFSET);
                // Perform any after scroll operations that are needed
                onScrollStateChanged(mListView, OnScrollListener.SCROLL_STATE_IDLE);
            }
        } else if (setSelected) {
            // Otherwise just set the selection
            setMonthDisplayed(mSelectedDay, true);
        }
        return false;
    }

    protected void setMonthDisplayed(Time time, boolean updateHighlight) {
        CharSequence oldMonth = mMonthName.getText();
        mMonthName.setText(Utils.formatMonthYear(mContext, time));
        mMonthName.invalidate();
        if (!TextUtils.equals(oldMonth, mMonthName.getText())) {
            mMonthName.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED);
        }
        mCurrentMonthDisplayed = time.month;
        if (updateHighlight) {
            mAdapter.updateFocusMonth(mCurrentMonthDisplayed);
        }
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


    @Override
    public void onScroll(
            AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
    }


}
