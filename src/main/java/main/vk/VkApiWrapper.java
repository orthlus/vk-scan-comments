package main.vk;

import com.vk.api.sdk.client.AbstractQueryBuilder;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ApiTooManyException;
import com.vk.api.sdk.exceptions.ClientException;
import com.vk.api.sdk.objects.groups.responses.GetByIdLegacyResponse;
import com.vk.api.sdk.objects.users.responses.GetResponse;
import com.vk.api.sdk.objects.wall.responses.GetCommentsResponse;
import com.vk.api.sdk.queries.groups.GroupsGetByIdQueryWithLegacy;
import com.vk.api.sdk.queries.users.UsersGetQuery;
import com.vk.api.sdk.queries.wall.WallGetCommentsQuery;
import com.vk.api.sdk.queries.wall.WallGetQuery;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class VkApiWrapper {
	@Value("${VK_API_DELAY}")
	private long apiDelayMilliseconds;

	<T> List<T> tryRequestList(AbstractQueryBuilder<?,?> req, T response) throws ClientException, ApiException {
		while (true) {
			try {
				return (List<T>) req.execute();
			} catch (ApiTooManyException e) {
				try {
					Thread.sleep(apiDelayMilliseconds);
				} catch (InterruptedException ex) {
					throw new RuntimeException(ex);
				}
			}
		}
	}

	private <T> T tryRequest(AbstractQueryBuilder<?,?> req, T response) throws ClientException, ApiException {
		while (true) {
			try {
				return (T) req.execute();
			} catch (ApiTooManyException e) {
				try {
					Thread.sleep(apiDelayMilliseconds);
				} catch (InterruptedException ex) {
					throw new RuntimeException(ex);
				}
			}
		}
	}

	public List<GetByIdLegacyResponse> call(GroupsGetByIdQueryWithLegacy req) throws ClientException, ApiException {
		return tryRequestList(req, new GetByIdLegacyResponse());
	}

	public List<GetResponse> call(UsersGetQuery req) throws ClientException, ApiException {
		return tryRequestList(req, new GetResponse());
	}

	public GetCommentsResponse call(WallGetCommentsQuery req) throws ClientException, ApiException {
		return tryRequest(req, new GetCommentsResponse());
	}

	public com.vk.api.sdk.objects.wall.responses.GetResponse call(WallGetQuery req) throws ClientException, ApiException {
		return tryRequest(req, new com.vk.api.sdk.objects.wall.responses.GetResponse());
	}
}
