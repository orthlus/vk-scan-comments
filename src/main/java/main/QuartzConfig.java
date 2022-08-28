package main;

import main.shedule.SendNewCommentsJob;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

import java.time.ZoneId;
import java.util.TimeZone;

@Configuration
public class QuartzConfig {
	@Value("${SCANNING_CRON}")
	private String cronExpression;
	private static final TimeZone TZ = TimeZone.getTimeZone(ZoneId.of("Europe/Moscow"));

	@Bean
	public JobDetail jobDetail() {
		return JobBuilder.newJob()
				.storeDurably()
				.ofType(SendNewCommentsJob.class)
				.build();
	}

	@Bean
	public Scheduler scheduler(JobDetail jobDetail, SchedulerFactoryBean factory)
			throws SchedulerException {
		CronScheduleBuilder cronScheduleBuilder = CronScheduleBuilder
				.cronSchedule(cronExpression)
				.inTimeZone(TZ);
		CronTrigger trigger = TriggerBuilder.newTrigger()
				.forJob(jobDetail)
				.withSchedule(cronScheduleBuilder)
				.build();
		Scheduler scheduler = factory.getScheduler();
		scheduler.scheduleJob(trigger);
		scheduler.start();
		return scheduler;
	}
}
