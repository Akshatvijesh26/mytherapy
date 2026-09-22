package com.example.di;

import android.content.Context;

import androidx.annotation.NonNull;

import com.example.MyTherapyApp;
import com.example.data.dao.MedicineDao;
import com.example.data.dao.UserDao;
import com.example.data.database.AppDatabase;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;

/**
 * Hilt / Dagger module providing Room Database and DAO dependencies.
 */
@Module
@InstallIn(SingletonComponent.class)
public class DatabaseModule {

    @Provides
    @Singleton
    @NonNull
    public static Context provideContext() {
        return MyTherapyApp.getInstance().getApplicationContext();
    }

    @Provides
    @Singleton
    @NonNull
    public static AppDatabase provideAppDatabase(@NonNull Context context) {
        return AppDatabase.getInstance(context);
    }

    @Provides
    @Singleton
    @NonNull
    public static UserDao provideUserDao(@NonNull AppDatabase database) {
        return database.userDao();
    }

    @Provides
    @Singleton
    @NonNull
    public static MedicineDao provideMedicineDao(@NonNull AppDatabase database) {
        return database.medicineDao();
    }
}
