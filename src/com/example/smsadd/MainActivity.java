package com.example.smsadd;

import java.util.ArrayList;

import com.example.smsadd.MainActivity.NewContact;
import com.example.smsadd.R;

import android.os.Bundle;
import android.os.IBinder;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
	public class NewContact {
		String first, last, phone;	
	}
	
	public class UpdateContact {
		String first, last, phone, id, previous;
		Context context;
	}
	
	TextView text;
	LocalBroadcastManager mLocalBroadcastManager;
	BroadcastReceiver broadcastReceiver;
	ListView lv;
	
	ArrayList<NewContact> newList = new ArrayList<NewContact>();
	ArrayList<String> insertList = new ArrayList<String>();
	ArrayList<UpdateContact> updateList = new ArrayList<UpdateContact>();
	private SimpleService mService = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);
		LinearLayout ll = (LinearLayout) findViewById(R.id.linearlayout);
		lv = (ListView) findViewById(R.id.listlist);
		final ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_checked, insertList);
		lv.setAdapter(adapter);
		//TODO: create a list for each checkbox to go into
		//create a function to traverse the list and update each checked selection
		//update specific checkbox's with the most recent change (so there is only 1 checkbox per number)
		//create a button to update checked boxs'
		//create the update function (move from service to activity)
		//receive updates from service
		broadcastReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				if(intent.getAction().equals("updateContactInfo")) {
					Bundle bundle = intent.getExtras();
					if(bundle != null) {
						String id = bundle.getString("id");
						String firstName = bundle.getString("firstName");
						String lastName = bundle.getString("lastName");
						String number = bundle.getString("number");
						String previousName = bundle.getString("previousName");	
						UpdateContact upCon = new UpdateContact();
						upCon.first = firstName;
						upCon.last = lastName;
						upCon.id = id;
						upCon.phone = number;
						upCon.previous = previousName;
						updateList.add(upCon);
						
						CheckBox cb = new CheckBox(context);
						LayoutParams lp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
						cb.setLayoutParams(lp);
						cb.setText(previousName + " -> " + firstName + " " + lastName + '\n' + number);
						lv.addView(cb);
					}
					Toast.makeText(getBaseContext(), "broadcast received in Activity", Toast.LENGTH_SHORT).show();
				} else if (intent.getAction().equals("newContactInfo")) {
					Bundle bundle = intent.getExtras();
					if(bundle != null) {
						String firstName = bundle.getString("firstName");
						String lastName = bundle.getString("lastName");
						String number = bundle.getString("number");
						NewContact newCon = new NewContact();
						newCon.first = firstName;
						newCon.last = lastName;
						newCon.phone = number;
						
						newList.add(newCon);
						
						insertList.add(number + " - " + firstName + " " + lastName);
						adapter.notifyDataSetChanged();
					}
				}
			}
		};
		
		
		Button start = (Button)findViewById(R.id.serviceButton);
		Button stop = (Button)findViewById(R.id.cancelButton);

		start.setOnClickListener(startListener);
		stop.setOnClickListener(stopListener);
		
		text = (TextView) findViewById(R.id.testText);
		Button test = (Button)findViewById(R.id.testButton);
		test.setOnClickListener(testListener);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		IntentFilter filter = new IntentFilter();
		filter.addAction("contactInfo");
		mLocalBroadcastManager.registerReceiver(broadcastReceiver, filter);
	}

	private OnClickListener testListener = new OnClickListener() {
		public void onClick(View v) {
			//text.setText(mService.getTime());
			CheckBox cb = new CheckBox(v.getContext());
			LayoutParams lp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			cb.setLayoutParams(lp);
			cb.setText("asdf asdf asdf asdf ");
			lv.addView(cb);
			
			//TODO loop through listview and grab all checked items.
			// parse string and insert contacts into contact list
			/*SparseBooleanArray ch = lv.getCheckedItemPositions();
			for(int i=0; i<lv.getCount(); i++) {
				if(ch.valueAt(i)) {
					String[] ins = insertList.get(i).split(" ");
					insertContact(v.getContext(), ins[2], ins[3], ins[0]);
				}
			}
			*/
		}
	};
	
	private OnClickListener startListener = new OnClickListener() {
		public void onClick(View v){
			Intent intent = new Intent(MainActivity.this, SimpleService.class);
			Bundle b = new Bundle();
			EditText key = (EditText) findViewById(R.id.set_keyword);
			String keyword = key.getText().toString();
			Spinner del = (Spinner) findViewById(R.id.spinner);
			final CheckBox checkbox = (CheckBox) findViewById(R.id.checkBox1);
			
			if(keyword.isEmpty()) {
				Toast.makeText(getBaseContext(), "keyword is set to \"default\" ", Toast.LENGTH_SHORT).show();
			} else {
				Toast.makeText(getBaseContext(), "keyword is set to " + keyword, Toast.LENGTH_SHORT).show();
			}
			if(!checkbox.isChecked()) {
				keyword = keyword.toLowerCase();
			}
			b.putString("keyword", keyword);
			b.putString("delimiter", del.getSelectedItem().toString());
			intent.putExtras(b);
			startService(intent);
			
			doBind(intent);
		}	        	
	};

	private OnClickListener stopListener = new OnClickListener() {
		public void onClick(View v){
			doUnbind();
			stopService(new Intent(MainActivity.this,SimpleService.class));
		}	        	
	};

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	private ServiceConnection mServiceConn = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			Log.v("myApp", "Service: " + name + " connected");
			mService = ((SimpleService.myBinder)service).getService();
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			Log.v("myApp", "Service: " + name + " disconnected");
		}
	};
	
	boolean isBound;
	
	private void doBind(Intent i) {
		if(bindService(i, mServiceConn, 0)) {
			Log.i("myApp", "Service bound");
			isBound = true;
		} else {
			Log.e("myApp", "Service not bound");
		}
	}
	
	private void doUnbind() {
		if(isBound) {
			unbindService(mServiceConn);
			isBound = false;
			Log.i("myApp", "unbinding service");
		}
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
