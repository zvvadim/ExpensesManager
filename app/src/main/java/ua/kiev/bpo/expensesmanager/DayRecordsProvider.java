package ua.kiev.bpo.expensesmanager;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.support.annotation.Nullable;

import java.util.HashMap;

public class DayRecordsProvider extends ContentProvider{

    static final String PROVIDER_NAME = "ua.kiev.bpo.expensesmanager.DayRecordsProvider";
    static final String URL = "content://" + PROVIDER_NAME + "/instances/when/";
    public static final Uri CONTENT_URI = Uri.parse(URL);

    static final int DAYRECORD = 1;
    static final int DAYRECORD_ID = 2;

    private static HashMap<String, String> DAYRECORD_PROJECTION_MAP;

    static final UriMatcher uriMatcher;
    static{
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(PROVIDER_NAME, "dayrecords", DAYRECORD);
        uriMatcher.addURI(PROVIDER_NAME, "dayrecords/#", DAYRECORD_ID);
    }

    private Context mAppContext;
    private ExpensesManagerDatabaseHelper mHelper;
    private SQLiteDatabase mDb;

    @Override
    public boolean onCreate() {
        mAppContext = getContext();
        mHelper = new ExpensesManagerDatabaseHelper(mAppContext);
        mDb = mHelper.getWritableDatabase();
        return (mDb == null)? false:true;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Nullable
    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }

    @Nullable
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Nullable
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {

        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(ExpensesManagerDatabaseHelper.TABLE_EXPENSES);

        // otherwise process arguments and perform a standard query
        int length = uri.getPathSegments().size();
        if (length != 4) {
            throw new IllegalArgumentException("Unknown Uri");
        }

        String[] useProjection = null;
        if (projection != null && projection.length > 0) {
            useProjection = new String[projection.length + 1];
            System.arraycopy(projection, 0, useProjection, 0, projection.length);
            useProjection[projection.length] = "_id AS _id";
        }

        qb.appendWhere(ExpensesManagerDatabaseHelper.COLUMN_EXPENSES_DATE + " BETWEEN " + uri
                .getPathSegments()
                .get(2) + " AND " + uri
                .getPathSegments()
                .get(3));

        if (sortOrder == null || sortOrder == ""){
            sortOrder = ExpensesManagerDatabaseHelper.COLUMN_EXPENSES_DATE;
        }
        Cursor c = qb.query(mDb,projection,	selection, selectionArgs,null, null, sortOrder);

        /**
         * register to watch a content URI for changes
         */
        c.setNotificationUri(mAppContext.getContentResolver(), uri);
        return c;

    }
}
