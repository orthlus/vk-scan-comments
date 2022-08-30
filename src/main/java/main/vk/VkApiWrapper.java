package main.vk;

import com.vk.api.sdk.client.AbstractQueryBuilder;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ApiTooManyException;
import com.vk.api.sdk.exceptions.ClientException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class VkApiWrapper {
	@Value("${VK_API_DELAY}")
	private long apiDelayMilliseconds;

	private <T, R> R tryRequest(AbstractQueryBuilder<T, R> req) throws ClientException, ApiException {
		while (true) {
			try {
				return req.execute();
			} catch (ApiTooManyException e) {
				try {
					Thread.sleep(apiDelayMilliseconds);
				} catch (InterruptedException ex) {
					throw new RuntimeException(ex);
				}
			}
		}
	}

	public <T, R> R call(AbstractQueryBuilder<T, R> req) throws ClientException, ApiException {
		return tryRequest(req);
	}
}
