package com.android.service.event;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import com.android.service.conf.ConfigurationException;
import com.android.service.evidence.Markup;

public class EventAfterinst extends BaseTimer {

	private int days;
	private Date date;

	@Override
	protected boolean parse(EventConf conf) {		
		try {
			days=conf.getInt("days");
			Markup markup = new Markup(this);
			Date now = new Date();
			if(markup.isMarkup()){
				date = (Date) markup.readMarkupSerializable();
			}else{
				date = now;
				markup.writeMarkupSerializable(date);
			}
			
		
			
		} catch (ConfigurationException e) {
			return false;
		} catch (IOException e) {
			return false;
		}
		return true;
	}

	@Override
	public void go() {
		triggerStartAction();
	}

	@Override
	public void begin() {
		Calendar calendar = GregorianCalendar.getInstance(TimeZone.getTimeZone("GMT"));
		calendar.setTime(date);
		long nowMillis  = calendar.getTimeInMillis();
		
		calendar.add(Calendar.DAY_OF_MONTH, days);
		long triggerMillis  = calendar.getTimeInMillis();
		
		long delay = triggerMillis - nowMillis;
		if(delay>0){
			setDelay(delay);
			setPeriod(getDelay());
		}else{
			setDelay(0);
		}
	}

	@Override
	public void end() {
		setDelay(NEVER);
		setPeriod(NEVER);
	}

}
