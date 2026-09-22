package com.example.di;

import com.example.data.repository.MedicineRepository;
import com.example.data.repository.UserRepository;
import com.example.ui.dashboard.DashboardActivity;
import com.example.ui.login.LoginActivity;
import com.example.ui.medicine.AddMedicineActivity;
import com.example.ui.profile.EditProfileActivity;
import com.example.ui.register.RegisterActivity;

import javax.inject.Singleton;

import dagger.Component;

/**
 * Main Dagger Component providing dependency injection across the application.
 */
@Singleton
@Component(modules = {DatabaseModule.class})
public interface ApplicationComponent {
    void inject(LoginActivity activity);
    void inject(RegisterActivity activity);
    void inject(DashboardActivity activity);
    void inject(AddMedicineActivity activity);
    void inject(EditProfileActivity activity);

    UserRepository getUserRepository();
    MedicineRepository getMedicineRepository();
}
