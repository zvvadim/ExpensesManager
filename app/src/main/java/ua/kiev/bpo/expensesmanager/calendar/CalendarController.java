package ua.kiev.bpo.expensesmanager.calendar;

import android.content.Context;
import android.text.format.Time;

import java.lang.ref.WeakReference;
import java.util.WeakHashMap;

public class CalendarController {
    private static final boolean DEBUG = false;
    private static final String TAG = "CalendarController";

    public static final int MIN_CALENDAR_YEAR = 1970;
    public static final int MAX_CALENDAR_YEAR = 2036;
    public static final int MIN_CALENDAR_WEEK = 0;
    public static final int MAX_CALENDAR_WEEK = 3497; // weeks between 1/1/1970 and 1/1/2037

    private static WeakHashMap<Context, WeakReference<CalendarController>> instances =
            new WeakHashMap<Context, WeakReference<CalendarController>>();

    private final Context mContext;

    private final Time mTime = new Time();

    private final Runnable mUpdateTimezone = new Runnable() {
        @Override
        public void run() {
            mTime.switchTimezone(Utils.getTimeZone(mContext, this));
        }
    };

    public static CalendarController getInstance(Context context) {
        synchronized (instances) {
            CalendarController controller = null;
            WeakReference<CalendarController> weakController = instances.get(context);
            if (weakController != null) {
                controller = weakController.get();
            }

            if (controller == null) {
                controller = new CalendarController(context);
                instances.put(context, new WeakReference(controller));
            }
            return controller;
        }
    }

    public static void removeInstance(Context context) {
        instances.remove(context);
    }

    private CalendarController(Context context) {
        mContext = context;
        mUpdateTimezone.run();
        mTime.setToNow();
    }

    public long getTime() {
        return mTime.toMillis(false);
    }


}
