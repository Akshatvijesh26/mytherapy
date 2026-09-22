package com.example;

import com.example.data.model.Medicine;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class MedicineTest {

    @Test
    public void testMedicineCreationAndProperties() {
        Medicine medicine = new Medicine(
                101L,
                "Amoxicillin 500mg",
                "file:///storage/med.jpg",
                2,
                "Morning, Night",
                "After Eat",
                "08:30 AM",
                false
        );
        medicine.setId(1L);

        assertEquals(1L, medicine.getId());
        assertEquals(101L, medicine.getUserId());
        assertEquals("Amoxicillin 500mg", medicine.getMedicineName());
        assertEquals("file:///storage/med.jpg", medicine.getPhotoUri());
        assertEquals(2, medicine.getFrequencyPerDay());
        assertEquals("Morning, Night", medicine.getTimeSlots());
        assertEquals("After Eat", medicine.getTimingRelation());
        assertEquals("08:30 AM", medicine.getReminderTime());
        assertFalse(medicine.isPassed());

        // Toggle status
        medicine.setPassed(true);
        assertTrue(medicine.isPassed());
    }

    @Test
    public void testMedicineDefaultConstructor() {
        Medicine medicine = new Medicine();
        assertNotNull(medicine.getMedicineName());
        assertNotNull(medicine.getTimeSlots());
        assertNotNull(medicine.getTimingRelation());
        assertNotNull(medicine.getReminderTime());
        assertFalse(medicine.isPassed());
    }

    @Test
    public void testReceiverConstants() {
        assertEquals("medicine_reminders_channel", com.example.receiver.MedicineReminderReceiver.CHANNEL_ID);
        assertEquals("Medicine Reminders", com.example.receiver.MedicineReminderReceiver.CHANNEL_NAME);
        assertEquals("extra_medicine_id", com.example.receiver.MedicineReminderReceiver.EXTRA_MEDICINE_ID);
        assertEquals("extra_medicine_name", com.example.receiver.MedicineReminderReceiver.EXTRA_MEDICINE_NAME);
        assertEquals("extra_photo_uri", com.example.receiver.MedicineReminderReceiver.EXTRA_PHOTO_URI);
    }
}
