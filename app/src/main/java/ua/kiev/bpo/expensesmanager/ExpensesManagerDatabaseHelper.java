package ua.kiev.bpo.expensesmanager;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.CursorWrapper;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.Date;

public class ExpensesManagerDatabaseHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "em.sqlite";
    private static final int VERSION = 1;

    public static final String TABLE_EXPENSES = "expenses";
    public static final String COLUMN_EXPENSES_ID = "_id";
    public static final String COLUMN_EXPENSES_DATE = "exp_date";
    public static final String COLUMN_EXPENSES_AMOUNT = "exp_amount";

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

    public long insertDayRecord(DayRecord dayRecord){
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_EXPENSES_DATE, dayRecord.getDateOfRecord().getTime());
        cv.put(COLUMN_EXPENSES_AMOUNT, dayRecord.getAmount());

        return getWritableDatabase().insert(TABLE_EXPENSES,null,cv);
    }

    public DayRecordCursor queryDayRecordByDay(long date){
        Cursor wraped = getReadableDatabase().query(TABLE_EXPENSES,
                null, COLUMN_EXPENSES_DATE + " = ?", new String[]{String.valueOf(date)}, null,
                null,null,"1");

        return  new DayRecordCursor(wraped);
    }

    public DayRecordCursor queryDayRecord(long id){
        Cursor wraped = getReadableDatabase().query(TABLE_EXPENSES,
                null, COLUMN_EXPENSES_ID + " = ?", new String[]{String.valueOf(id)}, null,
                null,null,"1");

        return  new DayRecordCursor(wraped);
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
            dayRecord.setStartDate((int) dayRecordDate);

            int dayRecordAmount = getInt(getColumnIndex(COLUMN_EXPENSES_AMOUNT));
            dayRecord.setAmount(dayRecordAmount);

            return dayRecord;
        }
    }

    public void updateDayRecord(DayRecord dayRecord){
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_EXPENSES_ID, dayRecord.getId());
        cv.put(COLUMN_EXPENSES_DATE, dayRecord.getDateOfRecord().getTime());
        cv.put(COLUMN_EXPENSES_AMOUNT, dayRecord.getAmount());

        getWritableDatabase().insertWithOnConflict(TABLE_EXPENSES, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
    }
}
