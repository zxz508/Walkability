package com.example.myapp.data.database;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import android.content.Context;

import com.example.myapp.data.dao.LocationDao;
import com.example.myapp.data.dao.PathDao;
import com.example.myapp.data.dao.PathPointDao;
import com.example.myapp.data.dao.RouteDao;
import com.example.myapp.data.dao.StepDao;
import com.example.myapp.data.dao.UserDao;
import com.example.myapp.data.model.Location;
import com.example.myapp.data.model.Path;
import com.example.myapp.data.model.PathPoint;
import com.example.myapp.data.model.Step;
import com.example.myapp.data.model.User;
import com.example.myapp.data.model.Route;


@Database(entities = {User.class, Step.class, Path.class, PathPoint.class, Location.class, Route.class}, version = 12, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    public abstract UserDao userDao();
    public abstract StepDao stepDao();
    public abstract PathDao pathDao();
    public abstract PathPointDao pathPointDao();
    public abstract RouteDao routeDao();
    public abstract LocationDao locationDao();
    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "running_database")
                            .addMigrations(MIGRATION_11_12)
                            .build();
                }
            }
        }
        return INSTANCE;
    }
    // Migration from version 11 to version 12
    static final Migration MIGRATION_11_12 = new Migration(11, 12) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // 清空 path 表的数据
            database.execSQL("DELETE FROM paths");

            // 如果有新列要添加
            database.execSQL("ALTER TABLE paths ADD COLUMN distance REAL DEFAULT 0 NOT NULL");
            database.execSQL("ALTER TABLE paths ADD COLUMN calories REAL DEFAULT 0 NOT NULL");
            database.execSQL("ALTER TABLE paths ADD COLUMN averageSpeed REAL DEFAULT 0 NOT NULL");
        }
    };
}
