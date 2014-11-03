package gcm;

import java.util.GregorianCalendar;

import net.diogomarques.wifioppish.MessagesProvider;
import net.diogomarques.wifioppish.R;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

public class PopUpActivity extends Activity {
	private Context c;
	private final int threshold = (60 * 2 * 1000);

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		c = this;
		Intent intent = getIntent();

		final String name = intent.getExtras().getString("name");
		String location = intent.getExtras().getString("location");
		final String date = intent.getExtras().getString("date");
		String duration = intent.getExtras().getString("duration");

		final double latS = intent.getExtras().getDouble("latS");
		final double lonS = intent.getExtras().getDouble("lonS");
		final double latE = intent.getExtras().getDouble("latE");
		final double lonE = intent.getExtras().getDouble("lonE");

		// set timer for retriving location
		long timeleft = GcmBroadcastReceiver.timeToDate(date);
		long handlerTimer = timeleft - threshold;
		if (handlerTimer < 0)
			handlerTimer = 0;
		Log.d("gcm", "pop up timer:" + handlerTimer);

		final AlertDialog alert = new AlertDialog.Builder(this)
				.setIcon(R.drawable.logo)
				.setTitle("Associate to simulation")
				.setMessage(
						"Do you want to join " + name + " simulation in "
								+ location + " at " + date + " for " + duration
								+ " minutes?")
				.setPositiveButton("Yes",
						new DialogInterface.OnClickListener() { 
							@Override
							public void onClick(DialogInterface dialog, 
									int which) {
								regSimulationContentProvider(name);
								Intent intent = new Intent().setClass(c,
										DemoActivity.class);
								intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
								intent.setAction("registerParticipant");
								intent.putExtra("name", name);
								startActivity(intent);
								Simulation.preDownloadTiles(latS, lonS, latE, 
										lonE, c);
								setAlarm(date);

								generateNotification(c,
										"You have been associate to " + name
												+ ". Details in FIND Service.");
							}

						}).setNegativeButton("No", null).show();

		// Hide after some seconds
		final Handler handler = new Handler();
		final Runnable runnable = new Runnable() {
			@Override
			public void run() {
				if (alert.isShowing()) {
					regSimulationContentProvider(name);
					Intent intent = new Intent()
							.setClass(c, DemoActivity.class);
					intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					intent.setAction("registerParticipant");
					intent.putExtra("name", name);
					startActivity(intent);
					Simulation.preDownloadTiles(latS, lonS, latE, lonE, c);
					generateNotification(c, "You have been associate to "
							+ name + ". Details in FIND Service.");
					setAlarm(date);

					alert.dismiss();
				}
			}
		};

		alert.setOnDismissListener(new DialogInterface.OnDismissListener() {
			@Override
			public void onDismiss(DialogInterface dialog) {
				handler.removeCallbacks(runnable);
			}
		});

		handler.postDelayed(runnable, handlerTimer);
	}

	private void setAlarm(String date) {
		long timeleft = GcmBroadcastReceiver.timeToDate(date);
		Long time = new GregorianCalendar().getTimeInMillis() + timeleft;
		Log.d("gcm", "setting alarm " + timeleft);
		Intent intentAlarm = new Intent("startAlarm");
		PendingIntent startPIntent = PendingIntent.getBroadcast(c, 0,
				intentAlarm, PendingIntent.FLAG_UPDATE_CURRENT);

		// create the object
		AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

		// set the alarm for particular time
		alarmManager.set(AlarmManager.RTC_WAKEUP, time, startPIntent);
		// Toast.makeText(this, "Alarm Scheduled for " + timeleft,
		// Toast.LENGTH_LONG).show();
		Log.d("gcm", "setting alarm " + timeleft);

	}
	
	public static void cancelAlarm(Context c) {
		Log.d("gcm", "canceling alarm");

		Intent intentAlarm = new Intent("startAlarm");
		PendingIntent startPIntent = PendingIntent.getBroadcast(c, 0,
				intentAlarm,PendingIntent.FLAG_UPDATE_CURRENT);

		// create the object
		AlarmManager alarmManager = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);

		// set the alarm for particular time
		alarmManager.cancel(startPIntent);
		// Toast.makeText(this, "Alarm Scheduled for " + timeleft,
		// Toast.LENGTH_LONG).show();

	}

	private void regSimulationContentProvider(String value) {
		ContentValues cv = new ContentValues();
		cv.put(MessagesProvider.COL_SIMUKEY, "simulation");
		cv.put(MessagesProvider.COL_SIMUVALUE, value);
		c.getContentResolver().insert(MessagesProvider.URI_SIMULATION, cv);
	}

	private static void generateNotification(Context context, String message) {
		int icon = R.drawable.logo;
		long when = System.currentTimeMillis();
		NotificationManager notificationManager = (NotificationManager) context 
				.getSystemService(Context.NOTIFICATION_SERVICE);
		Notification notification = new Notification(icon, message, when);

		String title = context.getString(R.string.app_name);

		Intent notificationIntent = new Intent(context, DemoActivity.class);
		// set intent so it does not start a new activity
		notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
				| Intent.FLAG_ACTIVITY_SINGLE_TOP);
		PendingIntent intent = PendingIntent.getActivity(context, 0,
				notificationIntent, 0);
		notification.setLatestEventInfo(context, title, message, intent);
		notification.flags |= Notification.FLAG_AUTO_CANCEL;

		// Play default notification sound
		notification.defaults |= Notification.DEFAULT_SOUND;

		// Vibrate if vibrate is enabled
		notification.defaults |= Notification.DEFAULT_VIBRATE;
		notificationManager.notify(0, notification);
	}
}
