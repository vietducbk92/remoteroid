/*
 * Remoteroid - A remote control solution for Android platform, including handy file transfer and notify-to-PC.
 * Copyright (C) 2012 Taeho Kim(jyte82@gmail.com), Hyomin Oh(ohmnia1112@gmail.com), Hongkyun Kim(godgjdgjd@nate.com), Yongwan Hwang(singerhwang@gmail.com)
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
*/

package org.secmem.remoteroid.natives;

import org.secmem.remoteroid.BuildConfig;
import org.secmem.remoteroid.util.CommandLine;
import org.secmem.remoteroid.util.Util;

import android.content.Context;
import android.util.Log;

/**
 * Contains methods related to event injection.
 * @author Taeho Kim
 *
 */
public class InputHandler {
	private static final String TAG = "InputHandler";
	
	private boolean isDeviceOpened = false;
	private Context context;
	
	private float xScaleFactor;
	private float yScaleFactor;
	private int xOffset;
	private int yOffset;
	
	public InputHandler(Context context){
		this.context = context;
		
		xScaleFactor = Util.Screen.getXScalingFactor(context);
		yScaleFactor = Util.Screen.getYScalingFactor(context);
		
		xOffset = Util.Screen.getXOffset(context);
		yOffset = Util.Screen.getYOffset(context);

	}
	
	public boolean isDeviceOpened(){
		return isDeviceOpened;
	}
	
	/**
	 * Opens uinput(User-level input) device for event injection.
	 * @return true device has opened without error, false otherwise
	 */
	public boolean open(){
		isDeviceOpened = openInputDevice();
		return isDeviceOpened;
	}
	
	/**
	 * Closes uinput device.
	 */
	public void close(){
		closeInputDevice();
		isDeviceOpened = false;
	}
	
	static{
		System.loadLibrary("remoteroid");
	}
	
	/**
	 * Set /dev/uinput's permission to 666, to read/write events via uinput.
	 */
	public void grantUinputPermission(){
		CommandLine.execAsRoot("chmod 666 /dev/uinput");
	}
	
	/**
	 * Revert /dev/uinput's permission to 660.
	 */
	public void revertUinputPermission(){
		CommandLine.execAsRoot("chmod 660 /dev/input");
	}
	
	/**
	 * Opens uinput(User-level input) device for event injection.
	 * @return true device has opened without error, false otherwise
	 */
	private native boolean openInputDevice();
	
	/**
	 * Open input device using suinput, without setting permission 666 to /dev/uinput.<br/>
	 * If user has su binary that doesn't supports 'su -c' option, which enables running shell command with root permission,
	 * Change permission through org.secmem.remoteroid.util.ComandLine.execAsRoot() first, then use this command to open device.
	 * @return true device has opened without error, false otherwise
	 */
	public native boolean openInputDeviceWithoutPermission();
	
	/**
	 * Closes uinput device.
	 */
	private native void closeInputDevice();
	
	/**
	 * Close input device, without reverting back /dev/uinput's permission to 660.
	 */
	public native void closeInputDeviceWithoutRevertPermission();
	
	/**
	 * Injects keyDown event.
	 * @param keyCode a KeyCode of KeyEvent
	 * @see org.secmem.remoteroid.data.NativeKeyCode NativeKeyCode
	 */
	public native void keyDown(int keyCode);
	
	/**
	 * Injects keyUp event.
	 * @param keyCode a KeyCode of KeyEvent
	 * @see org.secmem.remoteroid.data.NativeKeyCode NativeKeyCode
	 */
	public native void keyUp(int keyCode);
	
	/**
	 * Injects key stroke (keyDown and keyUp) event.
	 * @param keyCode a KeyCode of KeyEvent
	 * @see org.secmem.remoteroid.data.NativeKeyCode NativeKeyCode
	 */
	public native void keyStroke(int keyCode);
	
	/**
	 * Injects touch down (user touched screen) event.<br/>
	 * This event just represents <b>'touching a screen'</b> event. Setting touch screen's coordinate is processed on touchSetPtr(int, int) method.
	 * @see #touchSetPtr(int, int)
	 */
	public native void touchDown();
	
	/**
	 * Injects touch up (user removed finger from a screen) event.
	 */
	public native void touchUp();
	
	/**
	 * Set coordinates where user has touched on the screen.<br/>
	 * When user touches the screen, this method called first to set where user has touched, then {@link #touchDown()} called to notify user has touched screen.
	 * @param x x coordinate that user has touched
	 * @param y y coordinate that user has touched
	 * @param ignoreCalibrations if this has set to true, ignore saved calibration data and will inject raw coordinates.
	 */
	public void touchSetPointer(int x, int y, boolean ignoreCalibrations){
		if(BuildConfig.DEBUG)
			Log.d(TAG, String.format("Got raw pointer (%d, %d)", x, y));
		
		if(!ignoreCalibrations){
			// Calculate calibrated coordinates
			int calX = (int)((x + xOffset)*xScaleFactor);
			int calY = (int)((y + yOffset)*yScaleFactor);
			if(BuildConfig.DEBUG)
				Log.d(TAG, String.format("Pointer calibrated (%d, %d)", calX, calY));
			touchSetPtr(calX, calY);
		}else{
			touchSetPtr(x, y);
		}
		
	}
	
	
	private native void touchSetPtr(int x, int y);
	
	/**
	 * Injects 'touch once' event, touching specific coordinate once.<br/>
	 * This method calls {@link #touchSetPtr(int, int)}, {@link #touchDown()}, and {@link #touchUp()} in sequence.
	 * @param x x coordinate that user has touched
	 * @param y y coordinate that user has touched
	 */
	public native void touchOnce(int x, int y);
}
