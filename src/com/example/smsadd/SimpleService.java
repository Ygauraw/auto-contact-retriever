package com.example.smsadd;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.PhoneLookup;
import android.support.v4.content.LocalBroadcastManager;
import android.telephony.SmsMessage;
import android.widget.Toast;

public class SimpleService extends Service {

	String keyword = "default";
	String delimiter = ",";
	LocalBroadcastManager mLocalBroadcastManager;

	@Override
    public void onCreate() {
        super.onCreate();
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);
        
		IntentFilter filter = new IntentFilter();
		filter.addAction("android.provider.Telephony.SMS_RECEIVED");
		filter.addAction(android.telephony.TelephonyManager.ACTION_PHONE_STATE_CHANGED);
		registerReceiver(receiver, filter);
        Toast.makeText(this,"Service created ...", Toast.LENGTH_SHORT).show();
    }
    
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		Bundle b = intent.getExtras();
		if(b != null) {
			keyword = b.getString("keyword");
			delimiter = b.getString("delimiter");
		}
		return START_STICKY;
	}
	
    @Override
    public void onDestroy() {
    	unregisterReceiver(receiver);
          super.onDestroy();
          Toast.makeText(this, "Service destroyed ...", Toast.LENGTH_SHORT).show();
    }
    
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			 // Parse the SMS.
	        Bundle bundle = intent.getExtras();
	        SmsMessage[] msgs = null;
	        if (bundle != null)
	        {
	            // Retrieve the SMS.
	            Object[] pdus = (Object[]) bundle.get("pdus");
	            msgs = new SmsMessage[pdus.length];
	            for (int i=0; i<msgs.length; i++)
	            {
	                msgs[i] = SmsMessage.createFromPdu((byte[])pdus[i]);
	                String phone = msgs[i].getOriginatingAddress();
	                String tmp = msgs[i].getMessageBody().toString();
	                
	                tmp = tmp.trim();
	                String[] values = tmp.split(delimiter);
	                if(values[0].equals(keyword) && values[1]!=null && values[2]!=null) {
	                	
	                	Uri uri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phone));
	                	String[] projection = new String[]{ PhoneLookup.DISPLAY_NAME, PhoneLookup._ID };
						Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null);
						
						if(cursor != null && cursor.getCount() > 0) { //already exists within contacts list
							
							String str1 = "Updating Contact: ";
							str1 += '\n';
							String id = "";
							String previousName = "";
							while(cursor.moveToNext()) {
								previousName = cursor.getString(cursor.getColumnIndex(PhoneLookup.DISPLAY_NAME));
								str1 += previousName;
								str1 += '\n';
								id = cursor.getString(cursor.getColumnIndex(PhoneLookup._ID));
							}

							Toast.makeText(context, str1, Toast.LENGTH_SHORT).show();

							//updateContact(context, values[1], values[2], phone, id);
							
							broadcastUpdateContact(id, values[1], values[2], phone, previousName);
							
						} else { // new contact, create contact
				            //insertContact(context, values[1], values[2], phone);
				            broadcastNewContact(values[1], values[2], phone);
						}
						
						//TODO mark the message as read
						/*
						 * ContentValues values = new ContentValues();
							values.put("read",true);
							getContentResolver().update(Uri.parse("content://sms/inbox"),values, "_id="+SmsMessageId, null);
						 */
	                }
	            }
	        }
		}
    };

    @Override
    public IBinder onBind(Intent intent) {
    	return mBinder;
    }
    
    private final IBinder mBinder = new myBinder();
    public class myBinder extends Binder {
    	SimpleService getService() {
    		return SimpleService.this;
    	}
    }
    
    public boolean broadcastNewContact(String first, String last, String phone) {
    	Intent broadcastIntent = new Intent("newContactInfo");
		broadcastIntent.putExtra("firstName", first);
		broadcastIntent.putExtra("lastName", last);
		broadcastIntent.putExtra("number", phone);
		mLocalBroadcastManager.sendBroadcast(broadcastIntent);
    	return true;
    }
    
    public boolean broadcastUpdateContact(String id, String first, String last, String phone, String previous) {
    	Intent broadcastIntent = new Intent("updateContactInfo");
		broadcastIntent.putExtra("id", id);
		broadcastIntent.putExtra("firstName", first);
		broadcastIntent.putExtra("lastName", last);
		broadcastIntent.putExtra("number", phone);
		broadcastIntent.putExtra("previousName", previous);
		mLocalBroadcastManager.sendBroadcast(broadcastIntent);
    	return true;
    }

    public String getTime() {
    	SimpleDateFormat mDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    	return mDateFormat.format(new Date());
    }
    
    public boolean insertContact(Context context, String first, String last, String phone){
    	Toast.makeText(context, "inserting new contact into contact list!", Toast.LENGTH_SHORT).show();
		ArrayList<ContentProviderOperation> op_list = new ArrayList<ContentProviderOperation>(); 
    	
		op_list.add(ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI) 
				.withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null) 
				.withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null) 
				//.withValue(RawContacts.AGGREGATION_MODE, RawContacts.AGGREGATION_MODE_DEFAULT) 
				.build()); 

		// first and last names 
		op_list.add(ContentProviderOperation.newInsert(Data.CONTENT_URI) 
				.withValueBackReference(Data.RAW_CONTACT_ID, 0) 
				.withValue(Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE) 
				.withValue(StructuredName.GIVEN_NAME, first) 
				.withValue(StructuredName.FAMILY_NAME, last) 
				.build()); 

		op_list.add(ContentProviderOperation.newInsert(Data.CONTENT_URI) 
				.withValueBackReference(Data.RAW_CONTACT_ID, 0) 
				.withValue(ContactsContract.Data.MIMETYPE,ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
				.withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phone)
				.build());

		try{ 
			ContentProviderResult[] results = context.getContentResolver().applyBatch(ContactsContract.AUTHORITY, op_list); 
			return true;
		}catch(Exception e){ 
			e.printStackTrace(); 
	    	return false;
		}
    }
    
    public boolean updateContact(Context context, String first, String last, String phone, String id) {
		ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
		ops.add(ContentProviderOperation
				.newUpdate(ContactsContract.Data.CONTENT_URI)
				.withSelection(
						ContactsContract.Data.CONTACT_ID
							+ "=? AND "
							+ ContactsContract.Data.MIMETYPE
							+ "=?",
						new String[] {
								id,
								ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE
						})
				.withValue(
						ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME,
						first + " " + last).build());							
		try {
			ContentProviderResult[] result = context.getContentResolver().applyBatch(
					ContactsContract.AUTHORITY, ops);
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		} 
    }
}
