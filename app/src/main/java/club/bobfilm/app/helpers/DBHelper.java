package club.bobfilm.app.helpers;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import club.bobfilm.app.entity.Film;
import club.bobfilm.app.entity.FilmFile;
import club.bobfilm.app.util.Utils;

public class DBHelper extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 3;
    private static final String DATABASE_NAME = "toseex.db";
    public static final int FIELD_TYPE_NULL = 0;
    public static final int FIELD_TYPE_INTEGER = 1;
    public static final int FIELD_TYPE_FLOAT = 2;
    public static final int FIELD_TYPE_STRING = 3;
    public static final int FIELD_TYPE_BLOB = 4;
    public static final String TABLE_DOWNLOADS = "DOWNLOADS";
    public static final String TABLE_HISTORY = "HISTORY";
    public static final String TABLE_BOOKMARKS = "BOOKMARKS";
    public static final int ACTION_GET = 301;
    public static final int ACTION_ADD = 302;
    public static final int ACTION_DELETE = 303;
    public static final int ACTION_DELETE_ALL = 304;
    public static final int FN_DOWNLOADS = 311;
    public static final int FN_BOOKMARKS = 312;
    public static final int FN_HISTORY = 313;

    public static String FILE_ORDER_BY = "file_download_date DESC";
    private static ExecutorService mExecService = Executors.newCachedThreadPool();
    private final Context mContext;
    private AtomicInteger mOpenDBCounter = new AtomicInteger();
    private SQLiteDatabase mDatabase;
    private static Logger log = LoggerFactory.getLogger(DBHelper.class);

    private static volatile DBHelper instance;


    private final String CREATE_TABLE_DOWNLOADS = "CREATE TABLE IF NOT EXISTS "
            + TABLE_DOWNLOADS
            + " ("
            + "_id INTEGER, "
            + "file_name TEXT PRIMARY KEY UNIQUE ON CONFLICT REPLACE NOT NULL, "
            + "file_url TEXT, "
            + "file_logo_url TEXT, "
            + "file_size TEXT, "
            + "file_download_date TEXT, "
            + "is_download_complete INTEGER, "
            + "film_url TEXT, "
            + "film_title TEXT, "
            + "film_is_bookmarked INTEGER, "
            + "film_logo_url TEXT, "
            + "is_viewed INTEGER, "
            + "file_path TEXT, "
            + "download_status INTEGER, "
            + "download_progress INTEGER, "
            + "downloaded_size TEXT" + ");";

    private final String CREATE_TABLE_HISTORY = "CREATE TABLE IF NOT EXISTS "
            + TABLE_HISTORY
            + " ("
            + "_id INTEGER, " //1
            + "file_name TEXT PRIMARY KEY UNIQUE ON CONFLICT REPLACE NOT NULL, " //2
            + "file_url TEXT, " //3
            + "file_logo_url TEXT, " //4
            + "file_size TEXT, " //5
            + "file_download_date TEXT, " //6
            + "is_download_complete INTEGER, " //7
            + "film_url TEXT, " //8
            + "film_title TEXT, " //9
            + "film_is_bookmarked INTEGER, " //10
            + "film_logo_url TEXT, " //11
            + "is_viewed INTEGER, " //12
            + "file_path TEXT, " //13
            + "download_status INTEGER, " //14
            + "download_progress INTEGER, " //15
            + "downloaded_size TEXT" + ");"; //16

    private final String CREATE_TABLE_BOOKMARKS = "CREATE TABLE IF NOT EXISTS "
            + TABLE_BOOKMARKS
            + " ("
            + "_id INTEGER, "
            + "film_name TEXT NOT NULL, "
            + "film_url TEXT PRIMARY KEY UNIQUE ON CONFLICT REPLACE NOT NULL, "
            + "is_bookmarked INTEGER NOT NULL, "
            + "film_logo_url TEXT NOT NULL" + ");";


    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_DOWNLOADS);
        db.execSQL(CREATE_TABLE_HISTORY);
        db.execSQL(CREATE_TABLE_BOOKMARKS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_DOWNLOADS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_HISTORY);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_BOOKMARKS);
        onCreate(db);
    }

    public static DBHelper getInstance(Context c) {
        DBHelper localInstance = instance;
        if (localInstance == null) {
            synchronized (DBHelper.class) {
                localInstance = instance;
                if (localInstance == null) {
                    instance = localInstance = new DBHelper(c);
                }
            }
        }
        return localInstance;
    }

    private DBHelper(Context c) {
        super(c, DATABASE_NAME, null, DATABASE_VERSION);
        mContext = c;
    }

    private synchronized SQLiteDatabase getDatabase() {
        if (mOpenDBCounter.incrementAndGet() == 1) {
            // Opening new database
            mDatabase = instance.getWritableDatabase();
        }
        return mDatabase;
    }

    private synchronized void closeDatabase() {
        if (mOpenDBCounter.decrementAndGet() == 0) {
            // Closing database
            mDatabase.close();
        }
    }

    public void dbWorker(final int action, final int function, @Nullable final Object data, @Nullable final OnDBOperationListener dbListener) {
        mExecService.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    switchWork(action, function, data, dbListener);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    log.error(Utils.getErrorLogHeader() + new Object() {
                    }.getClass().getEnclosingMethod().getName(), ex);
                }
            }
        });
    }

    private synchronized void switchWork(int action, int function, @Nullable Object data, @Nullable OnDBOperationListener listener) {
        switch (action) {
            case ACTION_GET:
                switch (function) {
                    case FN_DOWNLOADS:
                        getDataFromDB(TABLE_DOWNLOADS, FILE_ORDER_BY, listener);
                        break;
                    case FN_BOOKMARKS:
                        getDataFromDB(TABLE_BOOKMARKS, null, listener);
                        break;
                    case FN_HISTORY:
                        getDataFromDB(TABLE_HISTORY, FILE_ORDER_BY, listener);
                        break;
                }
                break;
            case ACTION_ADD:
                switch (function) {
                    case FN_DOWNLOADS:
                        addItemToDB(TABLE_DOWNLOADS, data);
                        break;
                    case FN_BOOKMARKS:
                        addItemToDB(TABLE_BOOKMARKS, data);
                        break;
                    case FN_HISTORY:
                        addItemToDB(TABLE_HISTORY, data);
                        break;
                }
                break;
            case ACTION_DELETE:
                switch (function) {
                    case FN_DOWNLOADS:
                        deleteFromDB(TABLE_DOWNLOADS, "file_name=?", data != null ? ((FilmFile) data).getmFileName() : null);
                        break;
                    case FN_BOOKMARKS:
                        deleteFromDB(TABLE_BOOKMARKS, "film_url=?", data != null ? ((Film) data).getFilmUrl() : null);
                        break;
                    case FN_HISTORY:
                        deleteFromDB(TABLE_HISTORY, "file_name=?", data != null ? ((FilmFile) data).getmFileName() : null);
                        break;
                }
                break;
            case ACTION_DELETE_ALL:
                switch (function) {
                    case FN_DOWNLOADS:
                        deleteFromDB(TABLE_DOWNLOADS, "1", null);
                        break;
                    case FN_BOOKMARKS:
                        deleteFromDB(TABLE_BOOKMARKS, "1", null);
                        break;
                    case FN_HISTORY:
                        deleteFromDB(TABLE_HISTORY, "1", null);
                        break;
                }
        }
    }

    private void getDataFromDB(String tableName, @Nullable String orderBy, OnDBOperationListener listener) {
        SQLiteDatabase db = getDatabase();
        ArrayList<IDbData> items = new ArrayList<>();
        try {
            checkTableExists(tableName);
            Cursor cursor = db.query(tableName, null, null, null, null, null, orderBy);
            if (cursor.moveToFirst()) {
                String className = getClassName(tableName);
                Class<?> clazz = Class.forName(className);
                do {
                    IDbData item = (IDbData) clazz.newInstance();
                    item.fillItSelf(cursor);
                    items.add(item);
                } while (cursor.moveToNext());
                cursor.close();
            }
            listener.onSuccess(items);
        } catch (Exception ex) {
            ex.printStackTrace();
            log.error(Utils.getErrorLogHeader() + new Object() {
            }.getClass().getEnclosingMethod().getName(), ex);
            listener.onError(ex);
        } finally {
            closeDatabase();
        }
    }

    private void addItemToDB(String tableName, Object item) {
        SQLiteDatabase db = getDatabase();
        String className = getClassName(tableName);

        try {
            Class<?> clazz = Class.forName(className);
            checkTableExists(tableName);
            if (DatabaseUtils.queryNumEntries(db, tableName) >= 10000) {
                db.delete(tableName, null, null);
            }
            ContentValues values = ((IDbData) clazz.cast(item)).fillItemForDB();
            db.insertWithOnConflict(tableName, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        } catch (Exception ex) {
            ex.printStackTrace();
            log.error(Utils.getErrorLogHeader() + new Object() {
            }.getClass().getEnclosingMethod().getName(), ex);
        } finally {
            closeDatabase();
        }
    }

    private void deleteFromDB(String tableName, String whereClause, String whereArg) {
        SQLiteDatabase db = getDatabase();
        int deletedRowsCount = 0;
        String[] whereArgs = (whereArg != null) ? new String[]{whereArg} : null;
        try {
            deletedRowsCount = db.delete(tableName, whereClause, whereArgs);
            log.info("{} rows was deleted from {}", deletedRowsCount, tableName);
        } catch (Exception ex) {
            ex.printStackTrace();
            log.error(Utils.getErrorLogHeader() + new Object() {
            }.getClass().getEnclosingMethod().getName(), ex);
        } finally {
            closeDatabase();
        }
    }

    private String getClassName(String tableName) {
        String packageName = mContext.getPackageName() + ".entity.";
        switch (tableName) {
            case TABLE_DOWNLOADS:
                return packageName + "FilmFile";
            case TABLE_HISTORY:
                return packageName + "FilmFile";
            case TABLE_BOOKMARKS:
                return packageName + "Film";
        }
        return "";
    }

    private synchronized boolean checkTableExists(String table_name) {
        SQLiteDatabase db = getDatabase();
        Cursor cursorTableExists = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name=?", new String[]{table_name});
        if (cursorTableExists != null) {
            if (cursorTableExists.getCount() > 0) {
                cursorTableExists.close();
                closeDatabase();
                return true;
            }
            cursorTableExists.close();
        }
        createTable(db, table_name);
        closeDatabase();
        return false;
    }

    private void createTable(SQLiteDatabase db, String table_name) {
        switch (table_name) {
            case TABLE_DOWNLOADS:
                db.execSQL(CREATE_TABLE_DOWNLOADS);
                break;
            case TABLE_HISTORY:
                db.execSQL(CREATE_TABLE_HISTORY);
                break;
            case TABLE_BOOKMARKS:
                db.execSQL(CREATE_TABLE_BOOKMARKS);
                break;
        }
    }

    public interface OnDBOperationListener {
        void onSuccess(Object result);

        void onError(Exception ex);
    }

    public interface IDbData {
        void fillItSelf(Cursor cursor);

        ContentValues fillItemForDB();
    }

//    public synchronized void addHistoryFile(FilmFile historyFile) {
//        SQLiteDatabase db = getDatabase();
//        try {
//            checkTableExists(TABLE_HISTORY);
//            if (DatabaseUtils.queryNumEntries(db, TABLE_HISTORY) >= 10000) {
//                db.delete(TABLE_HISTORY, null, null);
//            }
//            ContentValues values = new ContentValues();
//            values.put("file_name", historyFile.getmFileName());
//            values.put("file_url", historyFile.getmFileUrl());
//            values.put("file_logo_url", historyFile.getmFileLogoUrl());
//            values.put("file_size", historyFile.getmFileSize());
//            values.put("file_download_date", historyFile.getmDownloadTimeDate());
//            values.put("is_viewed", historyFile.isViewed());
//            values.put("is_download_complete", historyFile.isDownloadComplete() ? 1 : 0);
//            values.put("film_url", historyFile.getFilmUrl());
//            values.put("film_title", historyFile.getFilmTitle());
//            values.put("film_is_bookmarked", historyFile.ismFilmBookmarked() ? 1 : 0);
//            values.put("film_logo_url", historyFile.getmFilmLogoUrl());
//            db.insertWithOnConflict(TABLE_HISTORY, null, values, SQLiteDatabase.CONFLICT_REPLACE);
//        } catch (Exception e) {
//            e.printStackTrace();
//        } finally {
//            closeDatabase();
//        }
//    }
//
//    public synchronized void deleteHistoryFile(FilmFile historyFile) {
//        SQLiteDatabase db = getDatabase();
//        String whereClause = "file_name=?";
//        String[] whereArgs = new String[]{historyFile.getmFileName()};
//        try {
//            db.delete(TABLE_HISTORY, whereClause, whereArgs);
//        } catch (Exception ex) {
//            ex.printStackTrace();
//        } finally {
//            closeDatabase();
//        }
//    }
//
//    public synchronized void getHistoryFiles(OnDBOperationListener listener) {
//        if (listener != null) {
//            SQLiteDatabase db = getDatabase();
//            try {
//                checkTableExists(TABLE_HISTORY);
//                Cursor cursor = db.query(TABLE_HISTORY, null, null, null, null, null, "file_download_date DESC");
//                ArrayList<FilmFile> historyFiles = new ArrayList<>();
//                if (cursor.moveToFirst()) {
//                    do {
//                        FilmFile file = new FilmFile();
//                        file.fillItSelf(cursor);
//                        file.setmFileName(cursor.getString(1));
//                        file.setmFileUrl(cursor.getString(2));
//                        file.setmFileLogoUrl(cursor.getString(3));
//                        file.setmFileSize(Long.parseLong(cursor.getString(4)));
//                        file.setmDownloadTimeDate(cursor.getString(5));
//                        file.setViewed(cursor.getInt(6) == 1);
//                        file.setDownloadComplete(cursor.getInt(7) == 1);
//                        file.setFilmUrl(cursor.getString(8));
//                        file.setFilmTitle(cursor.getString(9));
//                        file.setmFilmBookmarked(cursor.getInt(10) == 1);
//                        file.setmFilmLogoUrl(cursor.getString(11));
//                        historyFiles.add(file);
//                    } while (cursor.moveToNext());
//                    cursor.close();
//                }
//                listener.onSuccess(historyFiles);
//            } catch (Exception e) {
//                e.printStackTrace();
//                listener.onError(e);
//            } finally {
//                closeDatabase();
//            }
//        }
//    }
//
//    public synchronized void addBookmark(Film bookmark) {
//        SQLiteDatabase db = getDatabase();
//        try {
//            checkTableExists(TABLE_BOOKMARKS);
//            //checkDataDetailsByColumn(TABLE_BOOKMARKS, fileName, "file_name=?", "file_name");
//            if (DatabaseUtils.queryNumEntries(db, TABLE_BOOKMARKS) >= 10000) {
//                db.delete(TABLE_BOOKMARKS, null, null);
//            }
//            ContentValues values = new ContentValues();
//            values.put("film_name", bookmark.getFilmTitle());
//            values.put("film_url", bookmark.getFilmUrl());
//            values.put("is_bookmarked", bookmark.isBookmarked());
//            values.put("film_logo_url", bookmark.getPosterUrl());
//            db.insertWithOnConflict(TABLE_BOOKMARKS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
//        } catch (Exception e) {
//            e.printStackTrace();
//        } finally {
//            closeDatabase();
//        }
//    }
//
//    public synchronized void deleteBookmark(Film bookmark) {
//        SQLiteDatabase db = getDatabase();
//        String whereClause = "film_url=?";
//        String[] whereArgs = new String[]{bookmark.getFilmUrl()};
//        try {
//            db.delete(TABLE_BOOKMARKS, whereClause, whereArgs);
//        } catch (Exception ex) {
//            ex.printStackTrace();
//        } finally {
//            closeDatabase();
//        }
//    }
//
//    public synchronized void getBookmarkFiles(OnDBOperationListener listener) {
//        if (listener != null) {
//            SQLiteDatabase db = getDatabase();
//            try {
//                boolean tableExist = checkTableExists(TABLE_BOOKMARKS);
//                Cursor cursor = db.query(TABLE_BOOKMARKS, null, null, null, null, null, null);
//                List<Film> bookmarks = new ArrayList<>();
//
//                try {
//                    if (cursor.moveToFirst()) {
//                        do {
//                            Film bookmark = new Film();
//                            bookmark.setFilmTitle(cursor.getString(1));
//                            bookmark.setFilmUrl(cursor.getString(2));
//                            bookmark.setBookmarked(cursor.getInt(3) == 1);
//                            bookmark.setPosterUrl(cursor.getString(4));
//                            bookmarks.add(bookmark);
//                        } while (cursor.moveToNext());
//                        cursor.close();
//                    }
//                } catch (Exception ex) {
//                    ex.printStackTrace();
//                }
//                listener.onSuccess(bookmarks);
//            } catch (Exception e) {
//                e.printStackTrace();
//                listener.onError(e);
//            } finally {
//                closeDatabase();
//            }
//        }
//    }
//
//    public synchronized void addDownloadFile(FilmFile downloadFile) {
//        SQLiteDatabase db = getDatabase();
//        try {
//            checkTableExists(TABLE_DOWNLOADS);
//            if (DatabaseUtils.queryNumEntries(db, TABLE_DOWNLOADS) >= 10000) {
//                db.delete(TABLE_DOWNLOADS, null, null);
//            }
//            ContentValues values = new ContentValues();
//            values.put("file_name", downloadFile.getmFileName());
//            values.put("file_url", downloadFile.getmFileUrl());
//            values.put("file_logo_url", downloadFile.getmFileLogoUrl());
//            values.put("file_size", downloadFile.getmFileSize());
//            values.put("file_download_date", downloadFile.getmDownloadTimeDate());
//            values.put("is_download_complete", downloadFile.isDownloadComplete() ? 1 : 0);
//            values.put("file_path", downloadFile.getmFilePath());
//            db.insertWithOnConflict(TABLE_DOWNLOADS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
//        } catch (Exception e) {
//            e.printStackTrace();
//        } finally {
//            closeDatabase();
//        }
//    }
//
//    public synchronized void deleteDownloadFile(FilmFile downloadFile) {
//        SQLiteDatabase db = getDatabase();
//        String whereClause = "file_name=?";
//        String[] whereArgs = new String[]{downloadFile.getmFileName()};
//        try {
//            db.delete(TABLE_DOWNLOADS, whereClause, whereArgs);
//        } catch (Exception ex) {
//            ex.printStackTrace();
//        } finally {
//            closeDatabase();
//        }
//    }
//
//    public synchronized void getDownloadFiles(OnDBOperationListener listener) {
//        if (listener != null) {
//            SQLiteDatabase db = getDatabase();
//            try {
//                checkTableExists(TABLE_DOWNLOADS);
//                Cursor cursor = db.query(TABLE_DOWNLOADS, null, null, null, null, null, null);
//                ArrayList<FilmFile> downloadFiles = new ArrayList<>();
//                if (cursor.moveToFirst()) {
//                    do {
//                        FilmFile file = new FilmFile();
//                        file.setmFileName(cursor.getString(1));
//                        file.setmFileUrl(cursor.getString(2));
//                        file.setmFileLogoUrl(cursor.getString(3));
//                        file.setmFileSize(Long.parseLong(cursor.getString(4)));
//                        file.setmDownloadTimeDate(cursor.getString(5));
//
//                        file.setDownloadComplete(cursor.getInt(6) == 1);
//                        file.setmFilePath(cursor.getString(7));
//                        downloadFiles.add(file);
//                    } while (cursor.moveToNext());
//                    cursor.close();
//                }
//                listener.onSuccess(downloadFiles);
//            } catch (Exception e) {
//                e.printStackTrace();
//                listener.onError(e);
//            } finally {
//                closeDatabase();
//            }
//        }
//    }
//
//    public synchronized void addDownloadFiles(List<FilmFile> downloadFiles) {
//        SQLiteDatabase db = getDatabase();
//        try {
//            checkTableExists(TABLE_DOWNLOADS);
//            if (DatabaseUtils.queryNumEntries(db, TABLE_DOWNLOADS) >= 10000) {
//                db.delete(TABLE_DOWNLOADS, null, null);
//            }
//            for (FilmFile file : downloadFiles) {
//                ContentValues values = new ContentValues();
//                values.put("file_name", file.getmFileName());
//                values.put("file_url", file.getmFileUrl());
//                values.put("file_logo_url", file.getmFileLogoUrl());
//                values.put("file_size", file.getmFileSize());
//                values.put("file_download_date", file.getmDownloadTimeDate());
//                values.put("is_download_complete", file.isDownloadComplete() ? 1 : 0);
//
//                db.insertWithOnConflict(TABLE_DOWNLOADS, null, values, SQLiteDatabase.CONFLICT_IGNORE);
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        } finally {
//            closeDatabase();
//        }
//    }

    private void reCreateTable(SQLiteDatabase db, String table_name) {
        db.execSQL("DROP TABLE IF EXISTS " + table_name);
        createTable(db, table_name);
    }

    public synchronized boolean checkDataDetailsByColumn(String table_name, String columnData, String selection, String selectColumn) {
        SQLiteDatabase db = getDatabase();

        /*
         * columns – список полей, которые мы хотим получить
         * selection – строка условия WHERE
         * selectionArgs – массив аргументов для selection.
         * В selection можно использовать знаки ? , а которые будут заменены этими значениями.
         * groupBy - группировка
         * having – использование условий для агрегатных функций
         * orderBy - сортировка
         */

        String[] columns = new String[]{selectColumn};
        String[] selectionArgs = new String[]{columnData};
        String groupBy = null;
        String having = null;
        String orderBy = null;
        try {
            Cursor cursorData;
            if (!selectColumn.equalsIgnoreCase("count")) {
                cursorData = db.query(table_name, columns, selection, selectionArgs, groupBy, having, orderBy);
            } else {
                cursorData = db.rawQuery("SELECT " + selectColumn + " FROM " + table_name, null);
            }

            if (cursorData != null) {
                if (cursorData.moveToFirst()) {
                    if (cursorData.getType(0) != FIELD_TYPE_NULL) {
                        cursorData.close();
                        return true;
                    }
                }
                cursorData.close();
            }
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            closeDatabase();
        }
    }

    public synchronized boolean checkDataDetailsById(String table_name, int id, String selectColumn) {
        SQLiteDatabase db = getDatabase();
        String[] select = new String[]{selectColumn};
        String where = "_id=?";
        String[] whereArgs = new String[]{String.valueOf(id)};
        String groupBy = null;
        String having = null;
        String orderBy = null;
        try {
            Cursor cursorProjectDetails = db.query(table_name, select, where, whereArgs, groupBy, having, orderBy);
            if (cursorProjectDetails != null) {
                if (cursorProjectDetails.moveToFirst()) {
                    if (cursorProjectDetails.getString(0) != null) {
                        cursorProjectDetails.close();
                        return true;
                    }
                }
                cursorProjectDetails.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeDatabase();
        }
        return false;
    }

    public synchronized void dropTable(String table_name) {
        SQLiteDatabase db = getDatabase();
        db.delete(table_name, "1", null);
    }

    public synchronized void resetDB() {
        SQLiteDatabase db = getDatabase();
        onUpgrade(db, 1, 1);
        closeDatabase();
    }

}

