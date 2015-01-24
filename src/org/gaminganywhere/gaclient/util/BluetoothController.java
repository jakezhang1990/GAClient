package org.gaminganywhere.gaclient.util;

import org.gaminganywhere.gaclient.util.Pad.PartitionEventListener;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import android.widget.RelativeLayout;
import android.widget.Toast;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;

import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

public class BluetoothController extends GAController{
	// Debugging
    private static final String TAG = "BluetoothController";
    private static final boolean D = true;

    // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    // Key names received from the BluetoothChatService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    private static final int REQUEST_ENABLE_BT = 3;
    
    // Name of the connected device
    private String mConnectedDeviceName = null;
    // String buffer for outgoing messages
    private StringBuffer mOutStringBuffer;
    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;
    // Member object for the chat services
    private BluetoothService mBluetoothService = null;
    
    //Controller
    private Context context = null;
    private RelativeLayout relativeLayout = null;
    
    //Constructor
	public BluetoothController(Context context) {
		super(context);
	}

	public static String getName() {
		return "Bluetooth";
	}
	
	public static String getDescription() {
		return "Bluetooth";
	}
	
//	public Context getContext() {
//		return this.context;
//	}
	
	public void createBluetooth() {
		Log.i(TAG, "bt create");
        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            return;
        }
        if (!mBluetoothAdapter.isEnabled()){
        	Log.e(TAG, "bt");
        }
        Log.e(TAG, "bt");
        if (!mBluetoothAdapter.isEnabled()) {
        	Log.e(TAG, "bt is not enable");
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            Activity act = (Activity)context;
            act.startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        // Otherwise, setup
        } 
        else {
            if (mBluetoothService == null) 
            	mBluetoothService = new BluetoothService(mHandler);
            	
        }
        if (mBluetoothService != null) {
        	if (mBluetoothService.getState() == BluetoothService.STATE_NONE) {
        		mBluetoothService.start();
        	}
        }
	}
	
    /**
     * The Handler that gets information back from the BluetoothChatService
     * mHandler负责处理与后台BluetoothService的通信
 	 * 后台Service将msg发到这里来进行处理
     */
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MESSAGE_STATE_CHANGE:
                if(D) Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
//                switch (msg.arg1) {
//                case BluetoothService.STATE_CONNECTED:
//                    setStatus(getString(R.string.title_connected_to, mConnectedDeviceName));
////                    mConversationArrayAdapter.clear();
//                    break;
//                case BluetoothService.STATE_CONNECTING:
//                    setStatus(R.string.title_connecting);
//                    break;
//                case BluetoothService.STATE_LISTEN:
//                case BluetoothService.STATE_NONE:
//                    setStatus(R.string.title_not_connected);
//                    break;
//                }
                break;
//            case MESSAGE_WRITE:
//                byte[] writeBuf = (byte[]) msg.obj;
//                // construct a string from the buffer
//                String writeMessage = new String(writeBuf);
////                mConversationArrayAdapter.add("Me:  " + writeMessage);
//                break;
            case MESSAGE_READ:
                byte[] readBuf = (byte[]) msg.obj;
                controlSense(readBuf);
                // construct a string from the valid bytes in the buffer
                String readMessage = new String(readBuf, 0, msg.arg1);
//                mConversationArrayAdapter.add(mConnectedDeviceName+":  " + readMessage);
                break;
            case MESSAGE_DEVICE_NAME:
                // save the connected device's name
                mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                Toast.makeText(getContext(), "Connected to "
                               + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                break;
            case MESSAGE_TOAST:
                Toast.makeText(getContext(), msg.getData().getString(TOAST),
                               Toast.LENGTH_SHORT).show();
                break;
            }
        }
    };
    
    
    private void controlSense(byte[] buffer) {
        ByteArrayInputStream byteInput = new ByteArrayInputStream(buffer);
        DataInputStream dataInput = new DataInputStream(byteInput);
        try{
        	int head = dataInput.readInt();
        	switch (head) {
        	case Constants.MOUSE_KEY:
        		int mouseButton = dataInput.readInt();
        		this.sendMouseKey(true, mouseButton, this.getMouseX(), this.getMouseY() );
        		this.sendMouseKey(false, mouseButton, this.getMouseX(), this.getMouseY());
        		break;
        	case Constants.Arrow_Key:
        		int action = dataInput.readInt();
        		int part = dataInput.readInt();
        		this.emulateArrowKeys(action, part);
        		break;
        	}
        }catch (IOException e) {
        	Log.e(TAG, "io error", e);
        }
    }
    
	private boolean keyLeft = false;
	private boolean keyRight = false;
	private boolean keyUp = false;
	private boolean keyDown = false;
	private void emulateArrowKeys(int action, int part) {
		boolean myKeyLeft, myKeyRight, myKeyUp, myKeyDown;
		myKeyLeft = keyLeft;
		myKeyRight = keyRight;
		myKeyUp = keyUp;
		myKeyDown = keyDown;
		switch(action) {
		case MotionEvent.ACTION_DOWN:
		case MotionEvent.ACTION_POINTER_DOWN:
		case MotionEvent.ACTION_MOVE:
			// partition mappings for keys:
			// - up: 11, 12, 1, 2
			// - right: 2, 3, 4, 5
			// - down: 5, 6, 7, 8
			// - left: 8, 9, 10, 11
			switch(part) {
			case 0:
				myKeyUp = myKeyRight = myKeyDown = myKeyLeft = false;
				break;
			// single keys
			case 12: case 1:
				myKeyUp = true;
				myKeyRight = myKeyDown = myKeyLeft = false;
				break;
			case 3: case 4:
				myKeyRight = true;
				myKeyUp = myKeyDown = myKeyLeft = false;
				break;
			case 6: case 7:
				myKeyDown = true;
				myKeyUp = myKeyRight = myKeyLeft = false;
				break;
			case 9: case 10:
				myKeyLeft = true;
				myKeyUp = myKeyRight = myKeyDown = false;
				break;
			// hybrid keys
			case 2:
				myKeyUp = myKeyRight = true;
				myKeyDown = myKeyLeft = false;
				break;
			case 5:
				myKeyRight = myKeyDown = true;
				myKeyUp = myKeyLeft = false;
				break;
			case 8:
				myKeyDown = myKeyLeft = true;
				myKeyUp = myKeyRight = false;
				break;
			case 11:
				myKeyLeft = myKeyUp = true;
				myKeyRight = myKeyDown = false;
				break;
			}
			break;
		case MotionEvent.ACTION_UP:
		case MotionEvent.ACTION_POINTER_UP:
			if(keyLeft)
				this.sendKeyEvent(false, SDL2.Scancode.LEFT, 0, 0, 0);
			if(keyRight)
				this.sendKeyEvent(false, SDL2.Scancode.RIGHT, 0, 0, 0);
			if(keyUp)
				this.sendKeyEvent(false, SDL2.Scancode.UP, 0, 0, 0);
			if(keyDown)
				this.sendKeyEvent(false, SDL2.Scancode.DOWN, 0, 0, 0);
			myKeyUp = myKeyRight = myKeyDown = myKeyLeft = false;
			break;
		}
		if(myKeyUp != keyUp) {
			this.sendKeyEvent(myKeyUp, SDL2.Scancode.UP, SDL2.Keycode.UP, 0, 0);
			//Log.d("ga_log", String.format("Key up %s", myKeyUp ? "down" : "up"));
		}
		if(myKeyDown != keyDown) {
			this.sendKeyEvent(myKeyDown, SDL2.Scancode.DOWN, SDL2.Keycode.DOWN, 0, 0);
			//Log.d("ga_log", String.format("Key down %s", myKeyDown ? "down" : "up"));
		}
		if(myKeyLeft != keyLeft) {
			this.sendKeyEvent(myKeyLeft, SDL2.Scancode.LEFT, SDL2.Keycode.LEFT, 0, 0);
			//Log.d("ga_log", String.format("Key left %s", myKeyLeft ? "down" : "up"));
		}
		if(myKeyRight != keyRight) {
			this.sendKeyEvent(myKeyRight, SDL2.Scancode.RIGHT, SDL2.Keycode.RIGHT, 0, 0);
			//Log.d("ga_log", String.format("Key right %s", myKeyRight ? "down" : "up"));
		}
		keyUp = myKeyUp;
		keyDown = myKeyDown;
		keyLeft = myKeyLeft;
		keyRight = myKeyRight;
	}
	
//	public void onActivityResult(int requestCode, int resultCode, Intent data){
//		switch (requestCode) {
//		case REQUEST_ENABLE_BT:
//			if (resultCode == Activity.RESULT_OK){
//				//setup
//			}
//		}
//	}


}
