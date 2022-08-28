package main;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;

public class LoggerFilter extends Filter<ILoggingEvent> {
	@Override
	public FilterReply decide(ILoggingEvent event) {
		if (event.getMessage().equals("API error")) {
			String pattern = "Too many requests per second (6): Too many requests per second";
			if (event.getThrowableProxy().getMessage().equals(pattern)) {
				return FilterReply.DENY;
			}
		}
		return FilterReply.ACCEPT;
	}
}
