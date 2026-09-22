package com.example;

import android.app.Application;
import android.content.Context;

import androidx.annotation.NonNull;

import com.example.data.database.AppDatabase;
import com.example.data.repository.UserRepository;
import com.example.di.ApplicationComponent;
import com.example.di.DaggerApplicationComponent;

/**
 * Application class initializing Room Database, Dagger Hilt DI, and application components.
 */
public class MyTherapyApp extends Application {

    private static MyTherapyApp instance;
    private ApplicationComponent applicationComponent;
    private UserRepository userRepository;
    private com.example.data.repository.MedicineRepository medicineRepository;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        // Initialize Room DB and Repository
        AppDatabase db = AppDatabase.getInstance(this);
        userRepository = new UserRepository(db.userDao());
        medicineRepository = new com.example.data.repository.MedicineRepository(db.medicineDao());

        // Initialize Medicine Reminders Notification Channel
        com.example.util.MedicineReminderScheduler.createNotificationChannel(this);

        // Initialize Dagger ApplicationComponent
        try {
            applicationComponent = DaggerApplicationComponent.builder().build();
        } catch (Throwable t) {
            // Fallback initialization if component builder requires manual context binding
            applicationComponent = new ApplicationComponent() {
                @Override
                public void inject(com.example.ui.login.LoginActivity activity) {
                }

                @Override
                public void inject(com.example.ui.register.RegisterActivity activity) {
                }

                @Override
                public void inject(com.example.ui.dashboard.DashboardActivity activity) {
                }

                @Override
                public void inject(com.example.ui.medicine.AddMedicineActivity activity) {
                }

                @Override
                public void inject(com.example.ui.profile.EditProfileActivity activity) {
                }

                @Override
                public UserRepository getUserRepository() {
                    return userRepository;
                }

                @Override
                public com.example.data.repository.MedicineRepository getMedicineRepository() {
                    return medicineRepository;
                }
            };
        }
    }

    @NonNull
    public static MyTherapyApp getInstance() {
        return instance;
    }

    @NonNull
    public ApplicationComponent getComponent() {
        return applicationComponent;
    }

    @NonNull
    public UserRepository getUserRepository() {
        return userRepository;
    }

    @NonNull
    public com.example.data.repository.MedicineRepository getMedicineRepository() {
        return medicineRepository;
    }
}
