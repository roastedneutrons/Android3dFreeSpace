package com.konstrui.FreeSpace;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.SoundPool;
import android.media.SoundPool.OnLoadCompleteListener;
import android.os.Bundle;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

public class FreeSpaceActivity extends Activity implements SensorEventListener {
	private SensorManager mSensorManager;
	private boolean color = false; 
	private TextView view;
	private long lastUpdate;
	private SoundPool soundPool;
	private int soundID;
	boolean loaded = false;

	private static int displayWidth = 0;
	private static int displayHeight = 0;

	private float[] mAccelerometerReading;
	private float[] mMagneticFieldReading;
	private float[] mRotationMatrix = new float[16];
	private float[] mRemapedRotationMatrix = new float[16];
	private float[] mOrientation = new float[3];

	private float[] gVec={0,0,0};
	private float[] curAccel={0,0,0};
	
	private GameView gView;
	
/** Called when the activity is first created. */

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Display display = ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
		displayWidth = display.getWidth(); 		
		displayHeight = display.getHeight(); 	

		//setContentView(R.layout.main);
		gView=new GameView(this);
		setContentView(gView);
		//view = (TextView)findViewById(R.id.textView);
		//view.setBackgroundColor(Color.BLACK);

		mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		mSensorManager.registerListener(this,
			mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
			SensorManager.SENSOR_DELAY_GAME);
		mSensorManager.registerListener(this,
			mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
			SensorManager.SENSOR_DELAY_GAME);
		lastUpdate = System.currentTimeMillis();

		this.setVolumeControlStream(AudioManager.STREAM_MUSIC);
		// Load the sound
		soundPool = new SoundPool(10, AudioManager.STREAM_MUSIC, 0);
		soundPool.setOnLoadCompleteListener(new OnLoadCompleteListener() {
			@Override
			public void onLoadComplete(SoundPool soundPool, int sampleId,
					int status) {
				loaded = true;
			}
		});
		soundID = soundPool.load(this, R.raw.bang, 1);
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		switch (event.sensor.getType()) {
			case Sensor.TYPE_ACCELEROMETER: {
				mAccelerometerReading = event.values.clone();
				break;
			}
			case Sensor.TYPE_MAGNETIC_FIELD: {
				mMagneticFieldReading = event.values.clone();
				break;
			}
		}
		if(mAccelerometerReading != null && mMagneticFieldReading != null &&
		SensorManager.getRotationMatrix(mRotationMatrix, null, mAccelerometerReading, mMagneticFieldReading))
		{
			SensorManager.remapCoordinateSystem(mRotationMatrix,SensorManager.AXIS_Y, SensorManager.AXIS_MINUS_X, mRemapedRotationMatrix);
			SensorManager.getOrientation(mRemapedRotationMatrix, mOrientation);
		}
		if(mOrientation != null)
		{
			float yaw = mOrientation[0] * 57.2957795f;
	        float pitch = mOrientation[1] * 57.2957795f;
	        float roll = mOrientation[2] * 57.2957795f;
	        gView.debugText="yaw: "+yaw+"\npitch: "+pitch+"\nroll: "+roll;
	        gView.invalidate();
		}
		if (isShaken()) {
			onShake();
		}
	}
	
	public boolean isShaken() {
		// Movement
		float x = mAccelerometerReading[0];
		float y = mAccelerometerReading[1];
		float z = mAccelerometerReading[2];
		
		
		float accelationSquareRoot = (x * x + y * y + z * z)
				/ (SensorManager.GRAVITY_EARTH * SensorManager.GRAVITY_EARTH);
		long actualTime = System.currentTimeMillis();

		if (actualTime - lastUpdate > 50) {
			lastUpdate = actualTime;
			if (accelationSquareRoot >= 1.5) {
				return true;
			} else {
				return false;
			}
		} else {
			return false;
		}
	}

	public void onShake() {
		if (color) {
			// TODO view.setBackgroundColor(Color.BLUE);
			
		} else {
			// TODO view.setBackgroundColor(Color.RED);
		}
		color = !color;
		
		AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
		float actualVolume = (float) audioManager
				.getStreamVolume(AudioManager.STREAM_MUSIC);
		float maxVolume = (float) audioManager
				.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
		float volume = actualVolume / maxVolume;
		// Is the sound loaded already?
		if (loaded) {
			soundPool.play(soundID, volume, volume, 1, 0, 1f);
		}
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void onResume() {
		super.onResume();
		// register this class as a listener for the orientation and
		// accelerometer sensors
		mSensorManager.registerListener(this,
				mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
				SensorManager.SENSOR_DELAY_NORMAL);
	}

	@Override
	protected void onPause() {
		// unregister listener
		mSensorManager.unregisterListener(this);
		super.onStop();
	}
	
	private static class GameView extends View {
		private static Rect displayRect = null; //rect we display to
		private Rect scrollRect = null; //rect we scroll over our bitmap with
		public String debugText="";

		
		public GameView(Context context) {
			super(context);
			displayRect = new Rect(0, 0, displayWidth, displayHeight);
			scrollRect = new Rect(0, 0, displayWidth/4, displayHeight/4);
		}
		
		protected void onDraw(Canvas canvas) {
			canvas.drawColor(Color.BLACK);
			Paint paint = new Paint();
			paint.setStyle(Paint.Style.FILL);
			paint.setColor(Color.BLUE);
			paint.setStyle(Paint.Style.STROKE);
			canvas.drawRect(scrollRect, paint);

			paint.setAntiAlias(true);
			paint.setTextSize(30);
			//canvas.drawText(debugText, 0, 30, paint);
			drawMultilineText(debugText, 0, 30, paint,canvas);
		}

		private void drawMultilineText(String text, float x, float y, Paint paint, Canvas canvas){
			final float textSize = paint.getTextSize();
			for(String line: text.split("\n")){
			      canvas.drawText(line, x, y, paint);
			      y+=textSize; //or whatever the font height is....
			}
			
		}
	}
}