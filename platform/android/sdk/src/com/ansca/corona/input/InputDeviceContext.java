//////////////////////////////////////////////////////////////////////////////
//
// Copyright (C) 2018 Corona Labs Inc.
// Contact: support@coronalabs.com
//
// This file is part of the Corona game engine.
//
// Commercial License Usage
// Licensees holding valid commercial Corona licenses may use this file in
// accordance with the commercial license agreement between you and 
// Corona Labs Inc. For licensing terms and conditions please contact
// support@coronalabs.com or visit https://coronalabs.com/com-license
//
// GNU General Public License Usage
// Alternatively, this file may be used under the terms of the GNU General
// Public license version 3. The license is as published by the Free Software
// Foundation and appearing in the file LICENSE.GPL3 included in the packaging
// of this file. Please review the following information to ensure the GNU 
// General Public License requirements will
// be met: https://www.gnu.org/licenses/gpl-3.0.html
//
// For overview and more information on licensing please refer to README.md
//
//////////////////////////////////////////////////////////////////////////////

package com.ansca.corona.input;


/**
 * Stores the configuration and current status of one input device.
 * <p>
 * The device information stored in this context can be changed and is only expected to be
 * updated by the object which manages an input device. For example, when the connection
 * is gained or lost, when the configuration has changed (ie: axis input have been gained/lost),
 * or when new input has been received.
 * <p>
 * The rest of the system will only have read-only access to this device context via
 * an InputDeviceInterface object which wraps an InputDeviceContext object.
 * <p>
 * Instances of this class can only be created by Corona via the InputDeviceServices.add() method.
 */
public class InputDeviceContext {
	/** The unique integer ID assigned to this device by Corona's InputDeviceServices class. */
	private int fCoronaDeviceId;

	/** This device's current configuration. */
	private InputDeviceInfo fDeviceInfo;

	/** The input device's current connection state with the Android device. */
	private ConnectionState fConnectionState;

	/** Set to true if this device is connected to the system and can provide input. */
	private boolean fIsConnected;

	/**
	 * Stores the last recorded values from all axis inputs.
	 * <p>
	 * This is dictionary where the key is the zero based index to this device's axis.
	 */
	private java.util.HashMap<Integer, AxisDataPoint> fAxisData;

	/** Reference to the handler that will vibrate the device when requested. */
	private VibrateRequestHandler fVibrateRequestHandler;

	/**
	 * Collection of listeners to be invoked when the device's status has been changed
	 * or new data has been received.
	 */
	private java.util.ArrayList<InputDeviceContext.Listener> fListeners;

	/**
	 * Event settings that gets created when calling beginUpdate() and is used to raise
	 * a Corona "inputDeviceStatus" event when endUpdate() is called. Tracks all changes
	 * made when this object's update() methods are called.
	 */
	private InputDeviceStatusEventInfo.Settings fStatusEventSettings;

	/**
	 * Collection of axis data events that have been generated by this context's update() method
	 * when new axis values have been received. To be delivered to this context's listeners when
	 * the endUpdate() method has been called.
	 */
	private java.util.ArrayList<AxisDataEventInfo> fAxisEvents;


	/**
	 * Base interface from which all InputDeviceContext listeners derive from.
	 * @see InputDeviceContext
	 */
	public interface Listener {}

	/**
	 * Receives an event when an InputDeviceContext's configuration or connection has been changed.
	 * @see InputDeviceContext
	 */
	public interface OnStatusChangedListener extends Listener {
		/**
		 * Called when an input device's status has been changed, such as from connected to disconnected
		 * or if the device has been reconfigured.
		 * @param context Reference to the input device that has had its status changed.
		 * @param eventInfo Provides information about what has changed.
		 */
		public void onStatusChanged(InputDeviceContext context, InputDeviceStatusEventInfo eventInfo);
	}

	/**
	 * Receives an event when an InputDeviceContext has been updated with new axis data.
	 * @see InputDeviceContext
	 */
	public interface OnAxisDataReceivedListener extends Listener {
		/**
		 * Called when an input device has received new axis data.
		 * @param context Reference to the input device that has received new axis data.
		 * @param eventInfo Provides the received data and information about its axis.
		 */
		public void onAxisDataReceived(InputDeviceContext context, AxisDataEventInfo eventInfo);
	}

	/**
	 * Handler that is invoked when an input device's vibrate() method has been called.
	 * <p>
	 * This handler is assigned to an InputDeviceContext object via its setVibrateRequestHandler() method.
	 * @see InputDeviceContext
	 */
	public interface VibrateRequestHandler {
		/**
		 * Called when an input device's vibrate() method has been called.
		 * <p>
		 * This signals the handler to vibrate/rumble the requested device.
		 * @param context Reference to the input device that has been requested to be vibrated.
		 * @param settings Provides instructions on how to vibrate the input device.
		 */
		public void onHandleVibrateRequest(InputDeviceContext context, VibrationSettings settings);
	}


	/**
	 * Creates a new input device context.
	 * <p>
	 * This is an internal constructor that can only be called by Corona.
	 * Instances of this class are expected to be created by Corona's InputDeviceServices class.
	 * @param coronaDeviceId Unique ID assigned to the device by Corona.
	 * @param deviceInfo The input device's configuration. Cannot be null.
	 */
	InputDeviceContext(int coronaDeviceId, InputDeviceInfo deviceInfo) {
		// Validate.
		if (deviceInfo == null) {
			throw new NullPointerException();
		}

		// Store the given information.
		fCoronaDeviceId = coronaDeviceId;
		fDeviceInfo = deviceInfo;
		fIsConnected = false;
		fConnectionState = ConnectionState.DISCONNECTED;
		fAxisData = new java.util.HashMap<Integer, AxisDataPoint>();
		fAxisEvents = new java.util.ArrayList<AxisDataEventInfo>();
		fVibrateRequestHandler = null;
		fListeners = new java.util.ArrayList<InputDeviceContext.Listener>();
		fStatusEventSettings = null;
	}

	/**
	 * Gets the unique integer ID assigned to this device by Corona.
	 * @return Returns this device's unique integer ID assigned by Corona.
	 */
	public int getCoronaDeviceId() {
		return fCoronaDeviceId;
	}

	/**
	 * Gets this device's congifuration information.
	 * @return Returns this device's information.
	 */
	public InputDeviceInfo getDeviceInfo() {
		return fDeviceInfo;
	}

	/**
	 * Determines if the input device is currently connected to the system and can provide input.
	 * @return Returns true if the input device is currently connected. Returns false if not.
	 */
	public boolean isConnected() {
		return fConnectionState.isConnected();
	}

	/**
	 * Gets the input device's current connection state such as CONNECTED, DISCONNECTED, etc.
	 * @return Return the input device's current connection state with the Android system.
	 */
	public ConnectionState getConnectionState() {
		return fConnectionState;
	}

	/**
	 * Gets the last recorded value received from the specified axis.
	 * @param axisIndex Zero based index to this device's axis input.
	 * @return Returns the axis' last recorded data.
	 *         <p>
	 *         Returns null if no data has been received from the axis yet or if given an invalid index.
	 */
	public synchronized AxisDataPoint getAxisDataByIndex(int axisIndex) {
		return fAxisData.get(Integer.valueOf(axisIndex));
	}

	/**
	 * Determines if this input device's information and data is currently being updated.
	 * @return Returns true if the beginUpdate() method has been called, which means its update()
	 *         methods are being called.
	 *         <p>
	 *         Returns false if this device context is not currently being udpated.
	 */
	public boolean isUpdating() {
		return (fStatusEventSettings != null);
	}

	/**
	 * Informs this context that its update() methods are about to be called.
	 * <p>
	 * This prevents this context from invoking its given listeners and raising Corona events
	 * until the endUpdate() method has been called. This should be used if you plan on calling
	 * more than one update() method. Particularly if you are updating this context's device information
	 * and connection status, because those changes can be delievered as one event.
	 */
	public void beginUpdate() {
		// Do not continue if this method has already been called.
		if (fStatusEventSettings != null) {
			return;
		}

		// Create the event settings to be used by the update() methods.
		// These settings will track all changes made to this object.
		fStatusEventSettings = new InputDeviceStatusEventInfo.Settings();
	}

	/**
	 * This method must be called after calling this context's beginUpdate() method.
	 * <p>
	 * Calling this method indicates that you are done calling this context's update() methods.
	 * If any changes were detected, then this context's listener objects will be invoked
	 * and Corona "inputDeviceStatus" and "axis" events will be raised in Lua.
	 */
	public void endUpdate() {
		// Fetch a local reference to the event settings.
		InputDeviceStatusEventInfo.Settings statusEventSettings = fStatusEventSettings;
		if (statusEventSettings == null) {
			return;
		}

		// Clear the member variable reference to the event settings.
		// This signals that the update has ended.
		fStatusEventSettings = null;

		// Do not continue if there are no events to be raised.
		boolean hasReceivedAxisData = (fAxisEvents.size() > 0);
		boolean hasStatusChangeOccurred = statusEventSettings.hasConnectionStateChanged();
		hasStatusChangeOccurred |= statusEventSettings.wasReconfigured();
		if ((hasReceivedAxisData == false) && (hasStatusChangeOccurred == false)) {
			return;
		}

		// Clone event information in a thread safe manner.
		java.util.ArrayList<AxisDataEventInfo> axisEvents = null;
		java.util.ArrayList<InputDeviceContext.Listener> listeners = null;
		synchronized (this) {
			// Create a clone of the axis event collection and then clear it.
			// This is in case new axis data is received while invoking the listeners below.
			axisEvents = (java.util.ArrayList<AxisDataEventInfo>)fAxisEvents.clone();
			fAxisEvents.clear();

			// Create a clone of the listener collection.
			// This must be done in case listeners are added/removed while invoking them below.
			listeners = (java.util.ArrayList<InputDeviceContext.Listener>)fListeners.clone();
		}

		// Notify the system about this device's changes.
		if (listeners.size() > 0) {
			InputDeviceStatusEventInfo statusEventInfo = new InputDeviceStatusEventInfo(statusEventSettings);
			for (InputDeviceContext.Listener listener : listeners) {
				// Raise a status changed event, if occurred.
				if (hasStatusChangeOccurred && (listener instanceof OnStatusChangedListener)) {
					((OnStatusChangedListener)listener).onStatusChanged(this, statusEventInfo);
				}

				// Raise axis events, if any occurred.
				if (hasReceivedAxisData && (listener instanceof OnAxisDataReceivedListener)) {
					for (AxisDataEventInfo axisEvent : axisEvents) {
						((OnAxisDataReceivedListener)listener).onAxisDataReceived(this, axisEvent);
					}
				}
			}
		}
	}

	/**
	 * Updates the connection state of the input device.
	 * <p>
	 * If the state has changed, then this method will invoke the OnStatusChangedListener that
	 * was given to this context via the addListener() method. Will also raise a Corona "inputDeviceStatus"
	 * event in Lua.
	 * <p>
	 * Note that if the beginUpdate() method has been called, then this method will not raise
	 * an event until the context's endUpdate() method has been called.
	 * @param value The connection state such as CONNECTED, DISCONNECTED, etc.
	 *              <p>
	 *              Cannot be null or else an exception will be thrown.
	 */
	public void update(ConnectionState state) {
		// Validate.
		if (state == null) {
			throw new NullPointerException();
		}

		// Do not continue if the state has not changed.
		if (state == fConnectionState) {
			return;
		}

		// Update the connection state.
		fConnectionState = state;

		// Call beginUpdate() if not done already.
		boolean wasBeginUpdateCalled = isUpdating();
		if (wasBeginUpdateCalled == false) {
			beginUpdate();
		}

		// Flag that the connection state has changed.
		fStatusEventSettings.setHasConnectionStateChanged(true);

		// Raise an event now unless the caller has called beginUpdate().
		if (wasBeginUpdateCalled == false) {
			endUpdate();
		}
	}

	/**
	 * Updates the stored device configuration.
	 * <p>
	 * If the configuration has changed, then this method will invoke the OnStatusChangedListener that
	 * was given to this context via the addListener() method. Will also raise a Corona "inputDeviceStatus"
	 * event in Lua.
	 * <p>
	 * Note that if the beginUpdate() method has been called, then this method will not raise
	 * an event until the context's endUpdate() method has been called.
	 * @param settings The device information to update this context with.
	 *                 <p>
	 *                 Cannot be null or else an exception will be thrown.
	 */
	public void update(InputDeviceSettings settings) {
		update(InputDeviceInfo.from(settings));
	}

	/**
	 * Updates the stored device configuration.
	 * <p>
	 * If the configuration has changed, then this method will invoke the OnStatusChangedListener that
	 * was given to this context via the addListener() method. Will also raise a Corona "inputDeviceStatus"
	 * event in Lua.
	 * <p>
	 * Note that if the beginUpdate() method has been called, then this method will not raise
	 * an event until the context's endUpdate() method has been called.
	 * @param deviceInfo The device information to update this context with.
	 *                   <p>
	 *                   Cannot be null or else an exception will be thrown.
	 */
	void update(InputDeviceInfo deviceInfo) {
		// Validate.
		if (deviceInfo == null) {
			throw new NullPointerException();
		}

		// Do not continue if the device information has not changed.
		if (fDeviceInfo.equals(deviceInfo)) {
			return;
		}

		// Update this object's device information.
		fDeviceInfo = deviceInfo;

		// Call beginUpdate() if not done already.
		boolean wasBeginUpdateCalled = isUpdating();
		if (wasBeginUpdateCalled == false) {
			beginUpdate();
		}

		// Flag that the configuration has been changed.
		fStatusEventSettings.setWasReconfigured(true);

		// Raise an event now unless the caller has called beginUpdate().
		if (wasBeginUpdateCalled == false) {
			endUpdate();
		}
	}

	/**
	 * Updates the axis data stored by this device context.
	 * <p>
	 * Will raise a Corona "axis" event if the given data differs from the last received axis data.
	 * <p>
	 * Note that if the beginUpdate() method has been called, then this method will not raise
	 * an event until the context's endUpdate() method has been called.
	 * @param axisIndex Zero based index to this device's axis that has received data.
	 * @param dataPoint The axis data that was received. Cannot be null.
	 */
	public void update(int axisIndex, AxisDataPoint dataPoint) {
		// Validate.
		if (dataPoint == null) {
			return;
		}

		// Fetch the indexed axis information.
		AxisInfo axisInfo = fDeviceInfo.getAxes().getByIndex(axisIndex);
		if (axisInfo == null) {
			return;
		}

		// Make sure that the given value does not exceed the axis' min/max bounds.
		if (dataPoint.getValue() > axisInfo.getMaxValue()) {
			dataPoint = new AxisDataPoint(axisInfo.getMaxValue(), dataPoint.getTimestamp());
		}
		else if (dataPoint.getValue() < axisInfo.getMinValue()) {
			dataPoint = new AxisDataPoint(axisInfo.getMinValue(), dataPoint.getTimestamp());
		}

		// Do not update if the given axis value matches the last recorded axis value.
		Integer axisIndexObject = Integer.valueOf(axisIndex);
		AxisDataPoint lastDataPoint;
		synchronized (this) {
			lastDataPoint = fAxisData.get(axisIndexObject);
		}
		if (lastDataPoint != null) {
			float epsilon = axisInfo.getAccuracy();
			if (epsilon <= 0) {
				epsilon = 0.01f;
			}
			if ((dataPoint.getValue() < (lastDataPoint.getValue() + epsilon)) &&
			    (dataPoint.getValue() > (lastDataPoint.getValue() - epsilon)))
			{
				return;
			}
		}

		// Call beginUpdate() if not done already.
		boolean wasBeginUpdateCalled = isUpdating();
		if (wasBeginUpdateCalled == false) {
			beginUpdate();
		}

		// Store the given axis data and queue an axis event to be delivered when endUpdate gets called.
		synchronized (this) {
			fAxisData.put(axisIndexObject, dataPoint);
			fAxisEvents.add(new AxisDataEventInfo(fDeviceInfo, axisIndex, dataPoint));
		}

		// Raise an event now unless the caller has called beginUpdate().
		if (wasBeginUpdateCalled == false) {
			endUpdate();
		}
	}

	/**
	 * Sets the handler to be invoked when the device object's vibrate() method has been called.
	 * @param handler Reference to the handler to be invoked when the application is requesting
	 *                the device to be vibrated/rumbled.
	 *                <p>
	 *                Set to null to remove the last handler.
	 */
	public void setVibrateRequestHandler(VibrateRequestHandler handler) {
		fVibrateRequestHandler = handler;
	}

	/**
	 * Gets the handler that will be invoked when the device object's vibrate() method has been called.
	 * @return Returns a reference to the handler that performs the vibration/rumble feedback.
	 *         <p>
	 *         Returns null if a handler has not be assigned to this object via the
	 *         setVibrateRequestHandler() method.
	 */
	public VibrateRequestHandler getVibrateRequestHandler() {
		return fVibrateRequestHandler;
	}

	/**
	 * Requests the input device to vibrate/rumble.
	 * <p>
	 * Calls the VibrateRequestHandler.onHandleVibrateRequest() method to vibrate the device
	 * if a handler was assigned to this context object. If a handler was not assigned to
	 * this object, then this method will do nothing.
	 */
	public void vibrate() {
		// Do not continue if the device does not support vibration feedback.
		if (fDeviceInfo.canVibrate() == false) {
			return;
		}

		// Send the vibration request to the handler.
		VibrateRequestHandler handler = fVibrateRequestHandler;
		if (handler != null) {
			handler.onHandleVibrateRequest(this, null);
		}
	}

	/**
	 * Adds a listener used to detect when the device's connection state has changed, when the
	 * the device has been reconfigured, or when new axis data has been received.
	 * @param listener Reference to an OnStatusChangedListener or OnAxisDataReceivedListener object.
	 * @see OnStatusChangedListener
	 * @see OnAxisDataReceivedListener
	 */
	public synchronized void addListener(InputDeviceContext.Listener listener) {
		// Validate.
		if (listener == null) {
			return;
		}

		// Do not continue if the listener has already been added.
		if (fListeners.contains(listener)) {
			return;
		}

		// Add the listener to the collection.
		fListeners.add(listener);
	}

	/**
	 * Removes the event listener that was once given to this object's addListener() method.
	 * <p>
	 * This prevents the listener from receiving any more events.
	 * @param listener Reference to the listener that was once given to the addListener() method.
	 * @see OnStatusChangedListener
	 * @see OnAxisDataReceivedListener
	 */
	public synchronized void removeListener(InputDeviceContext.Listener listener) {
		// Validate.
		if (listener == null) {
			return;
		}

		// Remove the listener from the collection.
		fListeners.remove(listener);
	}
}
