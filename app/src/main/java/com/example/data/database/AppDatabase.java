package com.example.data.database;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.data.dao.MedicineDao;
import com.example.data.dao.UserDao;
import com.example.data.model.Medicine;
import com.example.data.model.User;

/**
 * Main Room Database for the My Therapy app.
 */
@Database(entities = {User.class, Medicine.class}, version = 2, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static final String DATABASE_NAME = "my_therapy_db";
    private static volatile AppDatabase INSTANCE;

    @NonNull
    public abstract UserDao userDao();

    @NonNull
    public abstract MedicineDao medicineDao();

    /**
     * Singleton accessor for the database instance.
     * @param context Application context.
     * @return AppDatabase instance.
     */
    @NonNull
    public static AppDatabase getInstance(@NonNull Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            DATABASE_NAME
                    )
                    .fallbackToDestructiveMigration()
                    .build();
                }
            }
        }
        return INSTANCE;
    }
}
