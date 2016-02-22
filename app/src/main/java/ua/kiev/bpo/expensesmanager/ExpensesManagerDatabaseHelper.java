package ua.kiev.bpo.expensesmanager;

import android.content.Context;
import android.database.Cursor;
import android.database.CursorWrapper;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.Date;

public class ExpensesManagerDatabaseHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "em.sqlite";
    private static final int VERSION = 1;

    private static final String TABLE_EXPENSES = "expenses";
    private static final String COLUMN_EXPENSES_ID = "_id";
    private static final String COLUMN_EXPENSES_DATE = "exp_date";
    private static final String COLUMN_EXPENSES_AMOUNT = "exp_amount";

    public ExpensesManagerDatabaseHelper(Context context) {
        super(context, DB_NAME, null, VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        db.execSQL("create table expenses (" +
                "_id integer primary key autoincrement, " +
                "exp_date integer, " +
                "exp_amount integer)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    public DayRecordCursor queryCreateDayRecord(Long id){
        return null;
    }

    public class DayRecordCursor extends CursorWrapper {
        public DayRecordCursor(Cursor cursor) {
            super(cursor);
        }

        public DayRecord getDayRecord(){
            if (isBeforeFirst() || isAfterLast()){
                return null;
            }
            DayRecord dayRecord = new DayRecord();
            long dayRecordId = getLong(getColumnIndex(COLUMN_EXPENSES_ID));
            dayRecord.setId(dayRecordId);

            long dayRecordDate = getLong(getColumnIndex(COLUMN_EXPENSES_DATE));
            dayRecord.setDateOfRecord(new Date(dayRecordDate));

            int dayRecordAmount = getInt(getColumnIndex(COLUMN_EXPENSES_AMOUNT));
            dayRecord.setAmount(dayRecordAmount);

            return dayRecord;
        }
    }


}
